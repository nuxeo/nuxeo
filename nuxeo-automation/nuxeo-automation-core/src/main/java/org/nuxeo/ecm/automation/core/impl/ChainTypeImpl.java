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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.OperationChainContribution;
import org.nuxeo.ecm.automation.core.OperationChainContribution.Operation;

import static org.nuxeo.ecm.automation.core.Constants.T_PROPERTIES;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ChainTypeImpl implements OperationType {

    protected OperationChainContribution contribution;

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
    protected Param[] params;

    /**
     * Invocable methods
     */
    protected List<InvokableMethod> methods;

    /**
     * The contribution fragment name
     */
    protected String contributingComponent;

    /**
     * The operation list
     */
    protected List<OperationType> operationsList;

    public ChainTypeImpl(AutomationService service,
            OperationChainContribution contribution,
            String contributingComponent) {
        this.service = service;
        this.contribution = contribution;
        this.contributingComponent = contributingComponent;
        id = contribution.getId();
        params = contribution.getParams();
    }

    public ChainTypeImpl(String chainId) {
        this.id = chainId;
    }

    public AutomationService getService() {
        return service;
    }

    public String getId() {
        return id;
    }

    public Class<?> getType() {
        return CompiledChainImpl.class;
    }

    public Object newInstance(OperationContext ctx, Map<String, Object> args)
            throws Exception {
        Operation[] ops = contribution.getOps();
        OperationParameters[] params = new OperationParameters[ops.length];
        for (int i = 0; i < params.length; ++i) {
            Map<String, Object> operationParameters = new HashMap<String, Object>();
            for (OperationChainContribution.Param parameter : ops[i].getParams()) {
                if (T_PROPERTIES.equals(parameter.getType())) {
                    operationParameters.put(parameter.getName(),
                            parameter.getMap());
                } else {
                    operationParameters.put(parameter.getName(),
                            parameter.getValue());
                }
            }
            params[i] = new OperationParameters(ops[i].getId(),
                    operationParameters);
        }
        ctx.putAll(args);
        return CompiledChainImpl.buildChain(service, ctx.getInput().getClass(),
                params);
    }

    public OperationDocumentation getDocumentation() {
        OperationDocumentation doc = new OperationDocumentation(id);
        doc.label = contribution.getLabel();
        doc.requires = contribution.getRequires();
        doc.category = contribution.getCategory();
        doc.since = contribution.getSince();
        if (doc.requires.length() == 0) {
            doc.requires = null;
        }
        if (doc.label.length() == 0) {
            doc.label = doc.id;
        }
        doc.description = contribution.getDescription();
        doc.params = contribution.getParams();
        doc.signature = new String[] { "void:void" };
        return doc;
    }

    public String getContributingComponent() {
        return contributingComponent;
    }

    @Override
    public Map<String, Field> getParameters() {
        return null;
    }

    public void addOperations(List<OperationType> operationList) {
        this.operationsList = operationList;
    }

}
