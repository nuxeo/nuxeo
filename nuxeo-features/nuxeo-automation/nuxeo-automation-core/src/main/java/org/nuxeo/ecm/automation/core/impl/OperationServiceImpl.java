/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationAdmin;
import org.nuxeo.ecm.automation.AutomationFilter;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.ChainException;
import org.nuxeo.ecm.automation.CompiledChain;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.TypeAdapter;
import org.nuxeo.ecm.automation.core.exception.CatchChainException;
import org.nuxeo.ecm.automation.core.exception.ChainExceptionRegistry;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;

/**
 * The operation registry is thread safe and optimized for modifications at startup and lookups at runtime.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationServiceImpl implements AutomationService, AutomationAdmin {

    private static final Log log = LogFactory.getLog(OperationServiceImpl.class);

    public static final String EXPORT_ALIASES_CONFIGURATION_PARAM = "nuxeo.automation.export.aliases";

    protected final OperationTypeRegistry operations;

    protected final ChainExceptionRegistry chainExceptionRegistry;

    protected final AutomationFilterRegistry automationFilterRegistry;

    protected final OperationChainCompiler compiler = new OperationChainCompiler(this);

    /**
     * Adapter registry.
     */
    protected AdapterKeyedRegistry adapters;

    public OperationServiceImpl() {
        operations = new OperationTypeRegistry();
        adapters = new AdapterKeyedRegistry();
        chainExceptionRegistry = new ChainExceptionRegistry();
        automationFilterRegistry = new AutomationFilterRegistry();
    }

    @Override
    public Object run(OperationContext ctx, String operationId) throws OperationException {
        return run(ctx, getOperationChain(operationId));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object run(OperationContext ctx, String operationId, Map<String, ?> args) throws OperationException {
        OperationType op = operations.lookup().get(operationId);
        if (op == null) {
            throw new IllegalArgumentException("No such operation " + operationId);
        }
        if (args == null) {
            log.warn("null operation parameters given for " + operationId, new Throwable("stack trace"));
            args = Collections.emptyMap();
        }
        return ctx.callWithChainParameters(() -> run(ctx, getOperationChain(operationId)), (Map<String, Object>) args);
    }

    @Override
    public Object run(OperationContext ctx, OperationChain chain) throws OperationException {
        Object input = ctx.getInput();
        Class<?> inputType = input == null ? Void.TYPE : input.getClass();
        CompiledChain compiled = compileChain(inputType, chain);
        boolean completedAbruptly = true;
        try {
            Object result = compiled.invoke(ctx);
            completedAbruptly = false;
            return result;
        } catch (OperationException cause) {
            completedAbruptly = false;
            if (hasChainException(chain.getId())) {
                return run(ctx, getChainExceptionToRun(ctx, chain.getId(), cause));
            } else if (cause.isRollback()) {
                ctx.setRollback();
            }
            throw cause;
        } finally {
            if (completedAbruptly) {
                ctx.setRollback();
            }
        }
    }

    @Override
    public Object runInNewTx(OperationContext ctx, String chainId, Map<String, ?> chainParameters, Integer timeout,
            boolean rollbackGlobalOnError) throws OperationException {
        Object result = null;
        // if the current transaction was already marked for rollback,
        // do nothing
        if (TransactionHelper.isTransactionMarkedRollback()) {
            return null;
        }
        // commit the current transaction
        TransactionHelper.commitOrRollbackTransaction();

        int to = timeout == null ? 0 : timeout;

        TransactionHelper.startTransaction(to);
        boolean ok = false;

        try {
            result = run(ctx, chainId, chainParameters);
            ok = true;
        } catch (OperationException e) {
            if (rollbackGlobalOnError) {
                throw e;
            } else {
                // just log, no rethrow
                log.error("Error while executing operation " + chainId, e);
            }
        } finally {
            if (!ok) {
                // will be logged by Automation framework
                TransactionHelper.setTransactionRollbackOnly();
            }
            TransactionHelper.commitOrRollbackTransaction();
            // caller expects a transaction to be started
            TransactionHelper.startTransaction();
        }
        return result;
    }

    /**
     * @since 5.7.3 Fetch the right chain id to run when catching exception for given chain failure.
     */
    protected String getChainExceptionToRun(OperationContext ctx, String operationTypeId, OperationException oe)
            throws OperationException {
        // Inject exception name into the context
        // since 6.0-HF05 should use exceptionName and exceptionObject on the context instead of Exception
        ctx.put("Exception", oe.getClass().getSimpleName());
        ctx.put("exceptionName", oe.getClass().getSimpleName());
        ctx.put("exceptionObject", oe);

        ChainException chainException = getChainException(operationTypeId);
        CatchChainException catchChainException = new CatchChainException();
        for (CatchChainException catchChainExceptionItem : chainException.getCatchChainExceptions()) {
            // Check first a possible filter value
            if (catchChainExceptionItem.hasFilter()) {
                AutomationFilter filter = getAutomationFilter(catchChainExceptionItem.getFilterId());
                try {
                    String filterValue = (String) filter.getValue().eval(ctx);
                    // Check if priority for this chain exception is higher
                    if (Boolean.parseBoolean(filterValue)) {
                        catchChainException = getCatchChainExceptionByPriority(catchChainException,
                                catchChainExceptionItem);
                    }
                } catch (RuntimeException e) { // TODO more specific exceptions?
                    throw new OperationException(
                            "Cannot evaluate Automation Filter " + filter.getId() + " mvel expression.", e);
                }
            } else {
                // Check if priority for this chain exception is higher
                catchChainException = getCatchChainExceptionByPriority(catchChainException, catchChainExceptionItem);
            }
        }
        String chainId = catchChainException.getChainId();
        if (chainId.isEmpty()) {
            throw new OperationException(
                    "No chain exception has been selected to be run. You should verify Automation filters applied.");
        }
        if (catchChainException.getRollBack()) {
            ctx.setRollback();
        }
        return catchChainException.getChainId();
    }

    /**
     * @since 5.7.3
     */
    protected CatchChainException getCatchChainExceptionByPriority(CatchChainException catchChainException,
            CatchChainException catchChainExceptionItem) {
        return catchChainException.getPriority() <= catchChainExceptionItem.getPriority() ? catchChainExceptionItem
                : catchChainException;
    }

    public static OperationParameters[] toParams(String... ids) {
        OperationParameters[] operationParameters = new OperationParameters[ids.length];
        for (int i = 0; i < ids.length; ++i) {
            operationParameters[i] = new OperationParameters(ids[i]);
        }
        return operationParameters;
    }

    @Override
    public void putOperationChain(OperationChain chain) throws OperationException {
        putOperationChain(chain, false);
    }

    final Map<String, OperationType> typeofChains = new HashMap<>();

    @Override
    public void putOperationChain(OperationChain chain, boolean replace) throws OperationException {
        final OperationType typeof = OperationType.typeof(chain, replace);
        this.putOperation(typeof, replace);
        typeofChains.put(chain.getId(), typeof);
    }

    @Override
    public void removeOperationChain(String id) {
        OperationType typeof = operations.lookup().get(id);
        if (typeof == null) {
            throw new IllegalArgumentException("no such chain " + id);
        }
        this.removeOperation(typeof);
    }

    @Override
    public OperationChain getOperationChain(String id) throws OperationNotFoundException {
        OperationType type = getOperation(id);
        if (type instanceof ChainTypeImpl) {
            return ((ChainTypeImpl) type).chain;
        }
        OperationChain chain = new OperationChain(id);
        chain.add(id);
        return chain;
    }

    @Override
    public List<OperationChain> getOperationChains() {
        List<ChainTypeImpl> chainsType = new ArrayList<>();
        List<OperationChain> chains = new ArrayList<>();
        for (OperationType operationType : operations.lookup().values()) {
            if (operationType instanceof ChainTypeImpl) {
                chainsType.add((ChainTypeImpl) operationType);
            }
        }
        for (ChainTypeImpl chainType : chainsType) {
            chains.add(chainType.getChain());
        }
        return chains;
    }

    @Override
    public synchronized void flushCompiledChains() {
        compiler.cache.invalidateAll();
    }

    @Override
    public void putOperation(Class<?> type) throws OperationException {
        OperationTypeImpl op = new OperationTypeImpl(this, type);
        putOperation(op, false);
    }

    @Override
    public void putOperation(Class<?> type, boolean replace) throws OperationException {
        putOperation(type, replace, null);
    }

    @Override
    public void putOperation(Class<?> type, boolean replace, String contributingComponent) throws OperationException {
        OperationTypeImpl op = new OperationTypeImpl(this, type, contributingComponent);
        putOperation(op, replace);
    }

    @Override
    public void putOperation(Class<?> type, boolean replace, String contributingComponent,
            List<WidgetDefinition> widgetDefinitionList) throws OperationException {
        OperationTypeImpl op = new OperationTypeImpl(this, type, contributingComponent, widgetDefinitionList);
        putOperation(op, replace);
    }

    @Override
    public void putOperation(OperationType op, boolean replace) throws OperationException {
        operations.addContribution(op, replace);
    }

    @Override
    public void removeOperation(Class<?> key) {
        OperationType type = operations.getOperationType(key);
        if (type == null) {
            log.warn("Cannot remove operation, no such operation " + key);
            return;
        }
        removeOperation(type);
    }

    @Override
    public void removeOperation(OperationType type) {
        operations.removeContribution(type);
    }

    @Override
    public OperationType[] getOperations() {
        HashSet<OperationType> values = new HashSet<>(operations.lookup().values());
        return values.toArray(new OperationType[values.size()]);
    }

    @Override
    public OperationType getOperation(String id) throws OperationNotFoundException {
        OperationType op = operations.lookup().get(id);
        if (op == null) {
            throw new OperationNotFoundException("No operation was bound on ID: " + id);
        }
        return op;
    }

    /**
     * @since 5.7.2
     * @param id operation ID.
     * @return true if operation registry contains the given operation.
     */
    @Override
    public boolean hasOperation(String id) {
        OperationType op = operations.lookup().get(id);
        return op != null;
    }

    @Override
    public CompiledChain compileChain(Class<?> inputType, OperationParameters... ops) throws OperationException {
        return compileChain(inputType, new OperationChain("", Arrays.asList(ops)));
    }

    @Override
    public CompiledChain compileChain(Class<?> inputType, OperationChain chain) throws OperationException {
        return compiler.compile(ChainTypeImpl.typeof(chain, false), inputType);
    }

    @Override
    public void putTypeAdapter(Class<?> accept, Class<?> produce, TypeAdapter adapter) {
        adapters.put(new TypeAdapterKey(accept, produce), adapter);
    }

    @Override
    public void removeTypeAdapter(Class<?> accept, Class<?> produce) {
        adapters.remove(new TypeAdapterKey(accept, produce));
    }

    @Override
    public TypeAdapter getTypeAdapter(Class<?> accept, Class<?> produce) {
        return adapters.get(new TypeAdapterKey(accept, produce));
    }

    @Override
    public boolean isTypeAdaptable(Class<?> typeToAdapt, Class<?> targetType) {
        return getTypeAdapter(typeToAdapt, targetType) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdaptedValue(OperationContext ctx, Object toAdapt, Class<?> targetType) throws OperationException {
        if (targetType.isAssignableFrom(Void.class)) {
            return null;
        }
        if (OperationContext.class.isAssignableFrom(targetType)) {
            return (T) ctx;
        }
        // handle primitive types
        Class<?> toAdaptClass = toAdapt == null ? Void.class : toAdapt.getClass();
        if (targetType.isPrimitive()) {
            targetType = getTypeForPrimitive(targetType);
            if (targetType.isAssignableFrom(toAdaptClass)) {
                return (T) toAdapt;
            }
        }
        if (targetType.isArray() && toAdapt instanceof List) {
            @SuppressWarnings("rawtypes")
            final Iterable iterable = (Iterable) toAdapt;
            return (T) Iterables.toArray(iterable, targetType.getComponentType());
        }
        TypeAdapter adapter = getTypeAdapter(toAdaptClass, targetType);
        if (adapter == null) {
            if (toAdapt == null) {
                return null;
            }
            if (toAdapt instanceof JsonNode) {
                // fall-back to generic jackson adapter
                ObjectMapper mapper = new ObjectMapper();
                return (T) mapper.convertValue(toAdapt, targetType);
            }
            if (targetType.isAssignableFrom(OperationContext.class)) {
                return (T) ctx;
            }
            throw new OperationException(
                    "No type adapter found for input: " + toAdaptClass + " and output " + targetType);
        }
        return (T) adapter.getAdaptedValue(ctx, toAdapt);
    }

    @Override
    public List<OperationDocumentation> getDocumentation() throws OperationException {
        List<OperationDocumentation> result = new ArrayList<>();
        HashSet<OperationType> ops = new HashSet<>(operations.lookup().values());
        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        boolean exportAliases = configurationService.isBooleanTrue(EXPORT_ALIASES_CONFIGURATION_PARAM);
        for (OperationType ot : ops.toArray(new OperationType[ops.size()])) {
            try {
                OperationDocumentation documentation = ot.getDocumentation();
                result.add(documentation);

                // we may want to add an operation documentation for each alias to be backward compatible with old
                // automation clients
                String[] aliases = ot.getAliases();
                if (exportAliases && aliases != null && aliases.length > 0) {
                    for (String alias : aliases) {
                        result.add(OperationDocumentation.copyForAlias(documentation, alias));
                    }
                }
            } catch (OperationNotFoundException e) {
                // do nothing
            }
        }
        Collections.sort(result);
        return result;
    }

    public static Class<?> getTypeForPrimitive(Class<?> primitiveType) {
        if (primitiveType == Boolean.TYPE) {
            return Boolean.class;
        } else if (primitiveType == Integer.TYPE) {
            return Integer.class;
        } else if (primitiveType == Long.TYPE) {
            return Long.class;
        } else if (primitiveType == Float.TYPE) {
            return Float.class;
        } else if (primitiveType == Double.TYPE) {
            return Double.class;
        } else if (primitiveType == Character.TYPE) {
            return Character.class;
        } else if (primitiveType == Byte.TYPE) {
            return Byte.class;
        } else if (primitiveType == Short.TYPE) {
            return Short.class;
        }
        return primitiveType;
    }

    /**
     * @since 5.7.3
     */
    @Override
    public void putChainException(ChainException exceptionChain) {
        chainExceptionRegistry.addContribution(exceptionChain);
    }

    /**
     * @since 5.7.3
     */
    @Override
    public void removeExceptionChain(ChainException exceptionChain) {
        chainExceptionRegistry.removeContribution(exceptionChain);
    }

    /**
     * @since 5.7.3
     */
    @Override
    public ChainException[] getChainExceptions() {
        Collection<ChainException> chainExceptions = chainExceptionRegistry.lookup().values();
        return chainExceptions.toArray(new ChainException[chainExceptions.size()]);
    }

    /**
     * @since 5.7.3
     */
    @Override
    public ChainException getChainException(String onChainId) {
        return chainExceptionRegistry.getChainException(onChainId);
    }

    /**
     * @since 5.7.3
     */
    @Override
    public boolean hasChainException(String onChainId) {
        return chainExceptionRegistry.getChainException(onChainId) != null;
    }

    /**
     * @since 5.7.3
     */
    @Override
    public void putAutomationFilter(AutomationFilter automationFilter) {
        automationFilterRegistry.addContribution(automationFilter);
    }

    /**
     * @since 5.7.3
     */
    @Override
    public void removeAutomationFilter(AutomationFilter automationFilter) {
        automationFilterRegistry.removeContribution(automationFilter);
    }

    /**
     * @since 5.7.3
     */
    @Override
    public AutomationFilter getAutomationFilter(String id) {
        return automationFilterRegistry.getAutomationFilter(id);
    }

    /**
     * @since 5.7.3
     */
    @Override
    public AutomationFilter[] getAutomationFilters() {
        Collection<AutomationFilter> automationFilters = automationFilterRegistry.lookup().values();
        return automationFilters.toArray(new AutomationFilter[automationFilters.size()]);
    }

}
