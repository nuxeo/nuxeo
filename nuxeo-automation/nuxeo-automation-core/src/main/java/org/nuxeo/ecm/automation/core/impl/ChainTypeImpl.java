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

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationDocumentation.Param;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.OperationChainContribution;
import org.nuxeo.ecm.automation.core.OperationChainContribution.Operation;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentRefList;

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

    static class Match implements Comparable<Match> {
        protected InvokableMethod method;

        int priority;

        Match(InvokableMethod method, int priority) {
            this.method = method;
            this.priority = priority;
        }

        @Override
        public int compareTo(Match o) {
            return o.priority - priority;
        }

        @Override
        public String toString() {
            return "Match(" + method + ", " + priority + ")";
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

    public Object newInstance(OperationContext ctx, Map<String, Object> args)
            throws Exception {
        Operation[] ops = contribution.getOps();
        OperationParameters[] params = new OperationParameters[ops.length];
        for (int i = 0; i < params.length; ++i) {
            params[i] = new OperationParameters(ops[i].getId());
        }
        return CompiledChainImpl.buildChain(service, ctx.getInput().getClass(),
                params);
    }

    public List<InvokableMethod> getMethods() {
        return methods;
    }

    public InvokableMethod[] getMethodsMatchingInput(Class<?> in) {
        List<Match> result = new ArrayList<Match>();
        for (InvokableMethod m : methods) {
            int priority = m.inputMatch(in);
            if (priority > 0) {
                result.add(new Match(m, priority));
            }
        }
        int size = result.size();
        if (size == 0) {
            return null;
        }
        if (size == 1) {
            return new InvokableMethod[] { result.get(0).method };
        }
        Collections.sort(result);
        InvokableMethod[] ar = new InvokableMethod[result.size()];
        for (int i = 0; i < ar.length; i++) {
            ar[i] = result.get(i).method;
        }
        return ar;
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

    protected String getParamDocumentationType(Class<?> type) {
        return getParamDocumentationType(type, false);
    }

    protected String getParamDocumentationType(Class<?> type, boolean isIterable) {
        String t;
        if (DocumentModel.class.isAssignableFrom(type)
                || DocumentRef.class.isAssignableFrom(type)) {
            t = isIterable ? Constants.T_DOCUMENTS : Constants.T_DOCUMENT;
        } else if (DocumentModelList.class.isAssignableFrom(type)
                || DocumentRefList.class.isAssignableFrom(type)) {
            t = Constants.T_DOCUMENTS;
        } else if (BlobList.class.isAssignableFrom(type)) {
            t = Constants.T_BLOBS;
        } else if (Blob.class.isAssignableFrom(type)) {
            t = isIterable ? Constants.T_BLOBS : Constants.T_BLOB;
        } else if (URL.class.isAssignableFrom(type)) {
            t = Constants.T_RESOURCE;
        } else if (Calendar.class.isAssignableFrom(type)) {
            t = Constants.T_DATE;
        } else {
            t = type.getSimpleName().toLowerCase();
        }
        return t;
    }

    public String getContributingComponent() {
        return contributingComponent;
    }

    @Override
    public void addOperations(List<OperationType> operationList) {
        this.operationsList = operationList;
    }

    @Override
    public List<OperationType> getOperations() {
        return operationsList;
    }
}
