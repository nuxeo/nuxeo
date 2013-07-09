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

/**
 * @since 5.7.2 Operation Type Implementation for a chain
 */
public class ChainTypeImpl implements OperationType {

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
    protected Map<String, Object> chainParameters;

    /**
     * The operations listing
     */
    protected OperationParameters[] operations;

    public ChainTypeImpl(AutomationService service, OperationChain chain) {
        this.service = service;
        this.operations = chain.getOperations().toArray(
                new OperationParameters[chain.getOperations().size()]);
        this.id = chain.getId();
        this.chainParameters = chain.getChainParameters();
    }

    public ChainTypeImpl() {

    }

    public Map<String, Object> getChainParameters() {
        return chainParameters;
    }

    public AutomationService getService() {
        return service;
    }

    @Override
    public OperationDocumentation getDocumentation() {
        return null;
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
    public Object newInstance(OperationContext ctx, Map<String, Object> args)
            throws Exception {
        Object input = ctx.getInput();
        Class<?> inputType = input == null ? Void.TYPE : input.getClass();
        return CompiledChainImpl.buildChain(service, inputType,
                operations);
    }

    @Override
    public String getContributingComponent() {
        return null;
    }

}
