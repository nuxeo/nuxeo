/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *     vpasquier
 *     slacoin
 */
package org.nuxeo.ecm.automation.core.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.AdapterNotFoundException;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.CompiledChain;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.TypeAdapter;
import org.nuxeo.ecm.automation.core.Constants;

/**
 * The operation registry is thread safe and optimized for modifications at
 * startup and lookups at runtime.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationServiceImpl implements AutomationService {

    protected final OperationTypeRegistry operations;

    protected Map<String, CompiledChainImpl> compiledChains = new HashMap<String, CompiledChainImpl>();

    /**
     * Adapter registry
     */
    protected AdapterKeyedRegistry adapters;

    public OperationServiceImpl() {
        operations = new OperationTypeRegistry();
        adapters = new AdapterKeyedRegistry();
    }

    @Override
    public Object run(OperationContext ctx, String chainId)
            throws Exception {
        ChainTypeImpl chain = (ChainTypeImpl) getOperation(chainId);
        return run(ctx, chain, chain.getChainParameters());
    }

    @Override
    public Object run(OperationContext ctx, OperationChain chain)
            throws Exception {
        ChainTypeImpl chainType = new ChainTypeImpl(this, chain);
        return run(ctx, chainType, Collections.<String, Object> emptyMap());
    }

    public Object run(OperationContext ctx, OperationChain chain, Map<String,Object> runtimeParameters)
            throws Exception {
        ChainTypeImpl chainType = new ChainTypeImpl(this, chain);
        return run(ctx, chainType, runtimeParameters);
    }

    /**
     * TODO avoid creating a temporary chain and then compile it. try to find a
     * way to execute the single operation without compiling it. (for
     * optimization)
     */
    @Override
    public Object run(OperationContext ctx, String operationId,
            Map<String, Object> runtimeParameters) throws Exception {
        OperationType type = getOperation(operationId);
        return run(ctx, type, runtimeParameters);
    }

    /**
     * @since 5.7.2
     * @param ctx the operation context
     * @param operationType a chain or an operation
     * @param params The chain parameters
     */
    public Object run(OperationContext ctx, OperationType operationType,
            Map<String, Object> params) throws Exception {
        CompiledChainImpl chain;
        // Init chain parameters sub context with empty params
        HashMap<String, Object> emptyParams = new HashMap<String, Object>();
        ctx.put(Constants.VAR_RUNTIME_CHAIN, params);
        // Put Chain parameters into the context - even for cached chains
        if (params != null && !params.isEmpty()) {
            ctx.put(Constants.VAR_RUNTIME_CHAIN, params);
        }
        try {
            Object input = ctx.getInput();
            Class<?> inputType = input == null ? Void.TYPE : input.getClass();
            if (ChainTypeImpl.class.isAssignableFrom(operationType.getClass())) {
                chain = compiledChains.get(operationType.getId());
                if (chain == null) {
                    chain = (CompiledChainImpl) operationType.newInstance(ctx,
                            params);
                    // Registered Chains are the only ones that can be cached
                    // Runtime ones can update their operations, model...
                    if (hasOperation(operationType.getId())) {
                        compiledChains.put(operationType.getId(), chain);
                    }
                }
            } else {
                chain = CompiledChainImpl.buildChain(inputType,
                        toParams(operationType.getId()));
            }
            Object ret = chain.invoke(ctx);
            if (ctx.getCoreSession() != null && ctx.isCommit()) {
                // auto save session if any
                ctx.getCoreSession().save();
            }
            return ret;
        } finally {
            ctx.dispose();
        }
    }

    public static OperationParameters[] toParams(String... ids) {
        OperationParameters[] operationParameters = new OperationParameters[ids.length];
        for (int i = 0; i < ids.length; ++i) {
            operationParameters[i] = new OperationParameters(ids[i]);
        }
        return operationParameters;
    }

    /**
     * Deprecated since 5.7.2 - Reason: no chain registry existence since chain
     * became an operation - use #putOperation method instead
     */
    @Override
    @Deprecated
    public synchronized void putOperationChain(OperationChain chain)
            throws OperationException {
        putOperationChain(chain, false);
    }

    /**
     * Deprecated since 5.7.2 - Reason: no chain registry existence since chain
     * became an operation - use #putOperation method instead
     */
    @Override
    @Deprecated
    public synchronized void putOperationChain(OperationChain chain,
            boolean replace) throws OperationException {
        OperationType docChainType = new ChainTypeImpl(this,chain);
        this.putOperation(docChainType, replace);
    }

    /**
     * Deprecated since 5.7.2 - Reason: no chain registry existence since chain
     * became an operation - use #removeOperation method instead
     */
    @Override
    @Deprecated
    public synchronized void removeOperationChain(String id) {
        OperationChain chain = new OperationChain(id);
        OperationType docChainType = new ChainTypeImpl(this,chain);
        operations.removeContribution(docChainType);
    }

    /**
     * Deprecated since 5.7.2 - Reason: no chain registry existence since chain
     * became an operation - use #getOperation method instead
     */
    @Override
    @Deprecated
    public OperationChain getOperationChain(String id)
            throws OperationNotFoundException {
        ChainTypeImpl chain = (ChainTypeImpl) getOperation(id);
        return chain.getChain();
    }

    /**
     * Deprecated since 5.7.2 - Reason: no chain registry existence since chain
     * became an operation - use #getOperations method instead
     */
    @Override
    @Deprecated
    public List<OperationChain> getOperationChains() {
        List<ChainTypeImpl> chainsType = new ArrayList<ChainTypeImpl>();
        List<OperationChain> chains = new ArrayList<OperationChain>();
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

    public synchronized void flushCompiledChains() {
        compiledChains.clear();
    }

    @Override
    public void putOperation(Class<?> type) throws OperationException {
        OperationTypeImpl op = new OperationTypeImpl(this, type);
        putOperation(op, false);
    }

    @Override
    public void putOperation(Class<?> type, boolean replace)
            throws OperationException {
        putOperation(type, replace, null);
    }

    @Override
    public void putOperation(Class<?> type, boolean replace,
            String contributingComponent) throws OperationException {
        OperationTypeImpl op = new OperationTypeImpl(this, type,
                contributingComponent);
        putOperation(op, replace);
    }

    public synchronized void putOperation(OperationType op,
            boolean replace) throws OperationException {
        operations.addContribution(op, replace);
    }

    @Override
    public synchronized void removeOperation(Class<?> key) {
        OperationType op = operations.getOperationType(key);
        if (op != null) {
            operations.removeContribution(op);
        }
    }

    @Override
    public OperationType[] getOperations() {
        Collection<OperationType> values = operations.lookup().values();
        return values.toArray(new OperationType[values.size()]);
    }

    @Override
     public OperationType getOperation(String id)
            throws OperationNotFoundException {
        OperationType op = operations.lookup().get(id);
        if (op == null) {
            throw new OperationNotFoundException(
                    "No operation was bound on ID: " + id);
        }
        return op;
    }

    /**
     * @since 5.7.2
     * @param id operation ID
     * @return true if operation registry contains the given operation
     */
    @Override
    public boolean hasOperation(String id) {
        OperationType op = operations.lookup().get(id);
        if (op == null) {
           return false;
        }
        return true;
    }

    @Override
    public CompiledChain compileChain(Class<?> inputType, OperationChain chain)
            throws Exception, InvalidChainException {
        List<OperationParameters> ops = chain.getOperations();
        return compileChain(inputType,
                ops.toArray(new OperationParameters[ops.size()]));
    }

    @Override
    public CompiledChain compileChain(Class<?> inputType,
            OperationParameters... operations) throws Exception,
            InvalidChainException {
        return CompiledChainImpl.buildChain(this, inputType == null ? Void.TYPE
                : inputType, operations);
    }

    @Override
    public void putTypeAdapter(Class<?> accept, Class<?> produce,
            TypeAdapter adapter) {
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
    public <T> T getAdaptedValue(OperationContext ctx, Object toAdapt,
            Class<?> targetType) throws Exception {
        if (toAdapt == null) {
            return null;
        }
        // handle primitive types
        Class<?> toAdaptClass = toAdapt.getClass();
        if (targetType.isPrimitive()) {
            targetType = getTypeForPrimitive(targetType);
            if (targetType.isAssignableFrom(toAdaptClass)) {
                return (T) toAdapt;
            }
        }
        TypeAdapter adapter = getTypeAdapter(toAdaptClass, targetType);
        if (adapter == null) {
            if (toAdapt instanceof JsonNode) {
                // fall-back to generic jackson adapter
                ObjectMapper mapper = new ObjectMapper();
                return (T) mapper.convertValue(toAdapt, targetType);
            }
            throw new AdapterNotFoundException(
                    "No type adapter found for input: " + toAdapt.getClass()
                            + " and output " + targetType, ctx);
        }
        return (T) adapter.getAdaptedValue(ctx, toAdapt);
    }

    @Override
    public List<OperationDocumentation> getDocumentation() {
        List<OperationDocumentation> result = new ArrayList<OperationDocumentation>();
        Collection<OperationType> ops = operations.lookup().values();
        for (OperationType ot : ops.toArray(new OperationType[ops.size()])) {
            result.add(ot.getDocumentation());
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

}
