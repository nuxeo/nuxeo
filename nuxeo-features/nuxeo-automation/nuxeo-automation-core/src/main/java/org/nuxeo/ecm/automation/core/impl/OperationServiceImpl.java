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
 */
package org.nuxeo.ecm.automation.core.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

/**
 * The operation registry is thread safe and optimized for modifications at
 * startup and lookups at runtime.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationServiceImpl implements AutomationService {

    protected final OperationTypeRegistry operations;

    protected final ChainEntryRegistry chains;

    /**
     * Adapter registry
     */
    protected AdapterKeyedRegistry adapters;

    public OperationServiceImpl() {
        operations = new OperationTypeRegistry();
        chains = new ChainEntryRegistry();
        adapters = new AdapterKeyedRegistry();
    }

    public Object run(OperationContext ctx, String chainId)
            throws OperationException, InvalidChainException, Exception {
        try {
            Object input = ctx.getInput();
            Class<?> inputType = input == null ? Void.TYPE : input.getClass();
            ChainEntry chain = getChainEntry(chainId);
            if (chain.cchain == null) {
                chain.cchain = compileChain(inputType, chain.chain);
            }
            Object ret = chain.cchain.invoke(ctx);
            if (ctx.getCoreSession() != null && ctx.isCommit()) {
                // auto save session if any
                ctx.getCoreSession().save();
            }
            return ret;
        } finally {
            ctx.dispose();
        }
    }

    public Object run(OperationContext ctx, OperationChain chain)
            throws OperationException, InvalidChainException, Exception {
        try {
            Object input = ctx.getInput();
            Class<?> inputType = input == null ? Void.TYPE : input.getClass();
            Object ret = compileChain(inputType, chain).invoke(ctx);
            if (ctx.getCoreSession() != null && ctx.isCommit()) {
                // auto save session if any
                ctx.getCoreSession().save();
            }
            return ret;
        } finally {
            ctx.dispose();
        }
    }

    /**
     * TODO avoid creating a temporary chain and then compile it. try to find a
     * way to execute the single operation without compiling it. (for
     * optimization)
     */
    @Override
    public Object run(OperationContext ctx, String id,
            Map<String, Object> params) throws OperationException,
            InvalidChainException, Exception {
        OperationChain chain = new OperationChain("operation");
        OperationParameters oparams = new OperationParameters(id, params);
        chain.add(oparams);
        return run(ctx, chain);
    }

    public synchronized void putOperationChain(OperationChain chain)
            throws OperationException {
        putOperationChain(chain, false);
    }

    public synchronized void putOperationChain(OperationChain chain,
            boolean replace) throws OperationException {
        chains.addContribution(new ChainEntry(chain), replace);
    }

    public synchronized void removeOperationChain(String id) {
        ChainEntry contrib = chains.getChainEntry(id);
        chains.removeContribution(contrib);
    }

    public OperationChain getOperationChain(String id)
            throws OperationNotFoundException {
        ChainEntry chain = chains.lookup().get(id);
        if (chain == null) {
            throw new OperationNotFoundException(
                    "No such chain was registered: " + id);
        }
        return chain.chain;
    }

    public List<OperationChain> getOperationChains() {
        List<OperationChain> result = new ArrayList<OperationChain>();
        Map<String, ChainEntry> ochains = chains.lookup();
        for (ChainEntry entry : ochains.values()) {
            result.add(entry.chain);
        }
        return result;
    }

    public ChainEntry getChainEntry(String id) throws OperationException {
        ChainEntry chain = chains.lookup().get(id);
        if (chain == null) {
            throw new OperationException("No such chain was registered: " + id);
        }
        return chain;
    }

    public synchronized void flushCompiledChains() {
        chains.flushCompiledChains();
    }

    public void putOperation(Class<?> type) throws OperationException {
        OperationTypeImpl op = new OperationTypeImpl(this, type);
        putOperation(op, false);
    }

    public void putOperation(Class<?> type, boolean replace)
            throws OperationException {
        putOperation(type, replace, null);
    }

    public void putOperation(Class<?> type, boolean replace,
            String contributingComponent) throws OperationException {
        OperationTypeImpl op = new OperationTypeImpl(this, type,
                contributingComponent);
        putOperation(op, replace);
    }

    protected synchronized void putOperation(OperationTypeImpl op,
            boolean replace) throws OperationException {
        operations.addContribution(op, replace);
    }

    public synchronized void removeOperation(Class<?> key) {
        OperationType op = operations.getOperationType(key);
        if (op != null) {
            operations.removeContribution(op);
        }
    }

    public OperationType[] getOperations() {
        Collection<OperationType> values = operations.lookup().values();
        return values.toArray(new OperationType[values.size()]);
    }

    public OperationType getOperation(String id)
            throws OperationNotFoundException {
        OperationType op = operations.lookup().get(id);
        if (op == null) {
            throw new OperationNotFoundException(
                    "No operation was bound on ID: " + id);
        }
        return op;
    }

    public CompiledChain compileChain(Class<?> inputType, OperationChain chain)
            throws Exception, InvalidChainException {
        List<OperationParameters> ops = chain.getOperations();
        return compileChain(inputType,
                ops.toArray(new OperationParameters[ops.size()]));
    }

    public CompiledChain compileChain(Class<?> inputType,
            OperationParameters... chain) throws Exception,
            InvalidChainException {
        return CompiledChainImpl.buildChain(this, inputType == null ? Void.TYPE
                : inputType, chain);
    }

    public void putTypeAdapter(Class<?> accept, Class<?> produce,
            TypeAdapter adapter) {
        adapters.put(new TypeAdapterKey(accept, produce), adapter);
    }

    public void removeTypeAdapter(Class<?> accept, Class<?> produce) {
        adapters.remove(new TypeAdapterKey(accept, produce));
    }

    public TypeAdapter getTypeAdapter(Class<?> accept, Class<?> produce) {
        return adapters.get(new TypeAdapterKey(accept, produce));
    }

    public boolean isTypeAdaptable(Class<?> typeToAdapt, Class<?> targetType) {
        return getTypeAdapter(typeToAdapt, targetType) != null;
    }

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

    public List<OperationDocumentation> getDocumentation() {
        List<OperationDocumentation> result = new ArrayList<OperationDocumentation>();
        Collection<OperationType> ops = operations.lookup().values();
        for (OperationTypeImpl ot : ops.toArray(new OperationTypeImpl[ops.size()])) {
            result.add(ot.getDocumentation());
        }
        Collections.sort(result);
        return result;
    }

    static class ChainEntry {
        OperationChain chain;

        CompiledChain cchain;

        ChainEntry(OperationChain chain) {
            this.chain = chain;
        }
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
