/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vpasquier
 *     slacoin
 */
package org.nuxeo.ecm.automation.core.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.InvalidChainException;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationNotFoundException;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.OperationChainContribution;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentRefList;

/**
 * @since 5.7.2 Operation Type Implementation for a chain
 */
public class ChainTypeImpl implements OperationType {

    protected final OperationChain chain;

    /**
     * Chain/Operation Parameters.
     */
    protected Map<String, Object> chainParameters;

    /**
     * The service that registered the operation.
     */
    protected AutomationService service;

    /**
     * The operation ID - used for lookups.
     */
    protected String id;

    /**
     * The operation ID Aliases array.
     *
     * @since 7.1
     */
    protected final String[] aliases;

    /**
     * Chain/Operation Parameters.
     */
    protected OperationDocumentation.Param[] params;

    /**
     * Invocable methods.
     */
    protected InvokableMethod[] methods = new InvokableMethod[] { runMethod() };

    /**
     * The contribution fragment name.
     */
    protected String contributingComponent;

    /**
     * The operations listing.
     */
    protected OperationParameters[] operations;

    /**
     * The operation chain XMAP contribution
     */
    protected OperationChainContribution contribution;

    /**
     * An output of operation type
     */
    protected Class<?> outputChain;

    /**
     * A method of operation type
     */
    protected InvokableMethod method;

    /**
     * The input type of a chain/operation. If set, the following input types {"document", "documents", "blob", "blobs"}
     * for all 'run method(s)' will handled. Other values will be adapted as java.lang.Object. If not set, Automation
     * will set the input type(s) as the 'run methods(s)' parameter types (by introspection).
     *
     * @since 7.4
     */
    protected String inputType;

    public ChainTypeImpl(AutomationService service, OperationChain chain) {
        this.service = service;
        operations = chain.getOperations().toArray(new OperationParameters[chain.getOperations().size()]);
        id = chain.getId();
        aliases = chain.getAliases();
        chainParameters = chain.getChainParameters();
        this.chain = chain;
    }

    public ChainTypeImpl(AutomationService service, OperationChain chain, OperationChainContribution contribution) {
        this.service = service;
        operations = chain.getOperations().toArray(new OperationParameters[chain.getOperations().size()]);
        id = chain.getId();
        aliases = chain.getAliases();
        chainParameters = chain.getChainParameters();
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
    public Object newInstance(OperationContext ctx, Map<String, Object> args) throws OperationNotFoundException,
            InvalidChainException {
        Object input = ctx.getInput();
        Class<?> inputType = input == null ? Void.TYPE : input.getClass();
        CompiledChainImpl op = CompiledChainImpl.buildChain(service, inputType, operations);
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
    public String[] getAliases() {
        return aliases;
    }

    @Override
    public Class<?> getType() {
        return CompiledChainImpl.class;
    }

    @Override
    public String getInputType() {
        return inputType;
    }

    @Override
    public OperationDocumentation getDocumentation() throws OperationException {
        OperationDocumentation doc = new OperationDocumentation(id);
        doc.label = id;
        doc.requires = contribution.getRequires();
        doc.category = contribution.getCategory();
        doc.setAliases(contribution.getAliases());
        OperationChainContribution.Operation[] operations = contribution.getOps();
        doc.operations = operations;
        doc.since = contribution.getSince();
        if (doc.requires.length() == 0) {
            doc.requires = null;
        }
        if (doc.label.length() == 0) {
            doc.label = doc.id;
        }
        doc.description = contribution.getDescription();
        doc.params = contribution.getParams();
        // load signature
        if (operations.length != 0) {
            // Fill signature with first inputs of the first operation and
            // related outputs of last operation
            // following the proper automation path
            ArrayList<String> result = getSignature(operations);
            doc.signature = result.toArray(new String[result.size()]);
        } else {
            doc.signature = new String[] { "void", "void" };
        }
        return doc;
    }

    /**
     * @since 5.7.2
     * @param operations operations listing that chain contains.
     * @return the chain signature.
     */
    protected ArrayList<String> getSignature(OperationChainContribution.Operation[] operations)
            throws OperationException {
        ArrayList<String> result = new ArrayList<String>();
        Collection<String> collectedSigs = new HashSet<String>();
        OperationType operationType = service.getOperation(operations[0].getId());
        for (InvokableMethod method : operationType.getMethods()) {
            String inputChain = getParamDocumentationType(method.getInputType(), method.isIterable());
            outputChain = method.getInputType();
            String outputChain = getParamDocumentationType(getChainOutput(operations));
            String sigKey = inputChain + ":" + outputChain;
            if (!collectedSigs.contains(sigKey)) {
                result.add(inputChain);
                result.add(outputChain);
                collectedSigs.add(sigKey);
            }
        }
        return result;
    }

    /**
     * @since 5.7.2
     */
    protected Class<?> getChainOutput(OperationChainContribution.Operation[] operations) throws OperationException {
        for (OperationChainContribution.Operation operation : operations) {
            OperationType operationType = service.getOperation(operation.getId());
            if (operationType instanceof OperationTypeImpl) {
                outputChain = getOperationOutput(outputChain, operationType);
            } else {
                outputChain = getChainOutput(operationType.getDocumentation().getOperations());
            }
        }
        return outputChain;
    }

    /**
     * @since 5.7.2
     */
    public Class<?> getOperationOutput(Class<?> input, OperationType operationType) {
        InvokableMethod[] methods = operationType.getMethodsMatchingInput(input);
        if (methods == null || methods.length == 0) {
            return input;
        }
        // Choose the top priority method
        InvokableMethod topMethod = getTopMethod(methods);
        Class<?> nextInput = topMethod.getOutputType();
        // If output is void, skip this method
        if (nextInput == Void.TYPE) {
            return input;
        }
        return nextInput;
    }

    /**
     * @since 5.7.2 Define the top priority method to take into account for chain operations signature.
     */
    protected InvokableMethod getTopMethod(InvokableMethod[] methods) {
        InvokableMethod topMethod = methods[0];
        for (InvokableMethod method : methods) {
            if (method.getPriority() > topMethod.getPriority()) {
                topMethod = method;
            }
        }
        return topMethod;
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

    /**
     * @since 5.7.2
     */
    protected String getParamDocumentationType(Class<?> type) {
        return getParamDocumentationType(type, false);
    }

    /**
     * @since 5.7.2
     */
    protected String getParamDocumentationType(Class<?> type, boolean isIterable) {
        String t;
        if (DocumentModel.class.isAssignableFrom(type) || DocumentRef.class.isAssignableFrom(type)) {
            t = isIterable ? Constants.T_DOCUMENTS : Constants.T_DOCUMENT;
        } else if (DocumentModelList.class.isAssignableFrom(type) || DocumentRefList.class.isAssignableFrom(type)) {
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

    @Override
    public String toString() {
        return "ChainTypeImpl [id=" + id + "]";
    }

    public OperationChainContribution getContribution() {
        return contribution;
    }

    /**
     * @since 5.7.2
     */
    @Override
    public List<InvokableMethod> getMethods() {
        return Arrays.asList(methods);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((chain == null) ? 0 : chain.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ChainTypeImpl)) {
            return false;
        }
        ChainTypeImpl other = (ChainTypeImpl) obj;
        if (chain == null) {
            if (other.chain != null) {
                return false;
            }
        } else if (!chain.equals(other.chain)) {
            return false;
        }
        return true;
    }
}
