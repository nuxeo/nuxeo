/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     vpasquier
 *     slacoin
 */
package org.nuxeo.ecm.automation.core.impl;

import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.OperationChainContribution;

/**
 * @since 5.7.2 Operation Type Implementation for a chain
 */
public class ChainTypeImpl implements OperationType {

    protected final OperationChain chain;

    /**
     *
     * Chain/Operation Parameters
     */
    protected Map<String, Object> chainParameters;

    /**
     * The service that registered the operation
     */
    protected AutomationService service;

    /**
     * The operation ID - used for lookups.
     */
    protected String id;

    /**
     * Chain/Operation Parameters
     */
    protected OperationDocumentation.Param[] params;

    /**
     * Invocable methods
     */
    protected InvokableMethod[] methods = new InvokableMethod[] { runMethod() };

    /**
     * The contribution fragment name
     */
    protected String contributingComponent;

    /**
     * The operations listing
     */
    protected OperationParameters[] operations;

    protected OperationChainContribution contribution;

    public ChainTypeImpl(AutomationService service, OperationChain chain) {
        this.service = service;
        this.operations = chain.getOperations().toArray(
                new OperationParameters[chain.getOperations().size()]);
        this.id = chain.getId();
        this.chainParameters = chain.getChainParameters();
        this.chain = chain;
    }

    public ChainTypeImpl(AutomationService service, OperationChain chain,
            OperationChainContribution contribution) {
        this.service = service;
        this.operations = chain.getOperations().toArray(
                new OperationParameters[chain.getOperations().size()]);
        this.id = chain.getId();
        this.chainParameters = chain.getChainParameters();
        this.contribution = contribution;
        this.chain = chain;
    }

    public OperationChain getChain() {
        return chain;
    }

    public Map<String, Object> getChainParameters() {
        return chainParameters;
    }

    @Override
    public Object newInstance(OperationContext ctx, Map<String, Object> args)
            throws Exception {
        Object input = ctx.getInput();
        Class<?> inputType = input == null ? Void.TYPE : input.getClass();
        CompiledChainImpl op = CompiledChainImpl.buildChain(service, inputType,
                operations);
        op.context = ctx;
        return op;
    }

    @Override
    public AutomationService getService() {
        return service;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Class<?> getType() {
        return CompiledChainImpl.class;
    }

    @Override
    public OperationDocumentation getDocumentation() {
        OperationDocumentation doc = new OperationDocumentation(id);
        doc.label = id;
        doc.requires = contribution.getRequires();
        doc.category = contribution.getCategory();
        doc.operations = contribution.getOps();
        doc.since = contribution.getSince();
        if (doc.requires.length() == 0) {
            doc.requires = null;
        }
        if (doc.label.length() == 0) {
            doc.label = doc.id;
        }
        id: doc.description = contribution.getDescription();
        doc.params = contribution.getParams();
        doc.signature = new String[] { "void", "void" };
        return doc;
    }

    @Override
    public String getContributingComponent() {
        return contributingComponent;
    }

    @Override
    public InvokableMethod[] getMethodsMatchingInput(Class<?> in) {
        return methods;
    }

    protected InvokableMethod runMethod() {
        try {
            return new InvokableMethod(this, CompiledChainImpl.class.getMethod("run"));
        } catch (NoSuchMethodException | SecurityException e) {
            throw new UnsupportedOperationException("Cannot use reflection for run method", e);
        }
    }

    @Override
    public String toString() {
        return "ChainTypeImpl [id=" + id + "]";
    }

    public OperationChainContribution getContribution() {
        return contribution;
    }
}
