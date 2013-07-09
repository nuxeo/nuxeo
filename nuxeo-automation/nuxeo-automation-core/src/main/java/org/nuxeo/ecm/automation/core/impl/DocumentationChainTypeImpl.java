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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.OperationChainContribution;

/**
 * @since 5.7.2 Documentation Operation Type for a chain
 */
public class DocumentationChainTypeImpl extends ChainTypeImpl {

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
     * The operations listing
     */
    protected OperationParameters[] operations;

    public DocumentationChainTypeImpl(AutomationService service,
            OperationChainContribution contribution,
            String contributingComponent) {
        super();
        this.service = service;
        this.contribution = contribution;
        this.contributingComponent = contributingComponent;
        this.id = contribution.getId();
        params = contribution.getParams();
        registerOperations();
    }

    private void registerOperations() {
        OperationChainContribution.Operation[] ops = contribution.getOps();
        operations = new OperationParameters[ops.length];
        for (int i = 0; i < operations.length; ++i) {
            Map<String, Object> operationParameters = new HashMap<String, Object>();
            for (OperationChainContribution.Param parameter : ops[i].getParams()) {
                if ("properties".equals(parameter.getType())) {
                    operationParameters.put(parameter.getName(),
                            parameter.getMap());
                } else {
                    operationParameters.put(parameter.getName(),
                            parameter.getValue());
                }
            }
            operations[i] = new OperationParameters(ops[i].getId(),
                    operationParameters);
        }
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

}
