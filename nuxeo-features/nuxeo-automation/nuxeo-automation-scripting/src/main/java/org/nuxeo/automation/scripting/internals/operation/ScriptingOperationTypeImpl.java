/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals.operation;

import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.nuxeo.automation.scripting.internals.ScriptOperationContext;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;
import org.nuxeo.ecm.automation.core.impl.OperationTypeImpl;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentRefList;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * @since 7.2
 */
public class ScriptingOperationTypeImpl extends OperationTypeImpl {

    protected final AutomationService service;

    protected final ScriptingOperationDescriptor desc;

    protected final InvokableMethod[] methods;

    public ScriptingOperationTypeImpl(AutomationService service, ScriptingOperationDescriptor desc) {
        this.service = service;
        this.desc = desc;
        this.inputType = desc.getInputType();
        this.methods = new InvokableMethod[] { runMethod() };
    }

    @Override
    public String getContributingComponent() {
        return null;
    }

    @Override
    public OperationDocumentation getDocumentation() {
        OperationDocumentation doc = new OperationDocumentation(getId());
        doc.label = getId();
        doc.category = desc.getCategory();
        doc.description = desc.getDescription();
        doc.params = desc.getParams();
        doc.signature = new String[] { desc.getInputType(), desc.getOutputType() };
        doc.aliases = desc.getAliases();
        return doc;
    }

    @Override
    public String getId() {
        return desc.getId();
    }

    @Override
    public String[] getAliases() {
        return desc.getAliases();
    }

    @Override
    public List<InvokableMethod> getMethods() {
        return Arrays.asList(methods);
    }

    @Override
    public InvokableMethod[] getMethodsMatchingInput(Class<?> in) {
        return methods;
    }

    @Override
    public AutomationService getService() {
        return service;
    }

    @Override
    public Class<?> getType() {
        return ScriptingOperationImpl.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object newInstance(OperationContext ctx, Map<String, Object> args) throws OperationException {
        try {
            resolveArguments(ctx, args);
            ScriptOperationContext sctx = new ScriptOperationContext(ctx);
            return new ScriptingOperationImpl(desc.getScript(), sctx, args);
        } catch (ScriptException e) {
            throw new NuxeoException(e);
        }
    }

    /**
     * As
     * {@link org.nuxeo.ecm.automation.core.impl.OperationTypeImpl#inject(org.nuxeo.ecm.automation.OperationContext, java.util.Map, java.lang.Object)}
     * is not called in this OperationTypeImpl, we have to inject into arguments all context variables to play the
     * fallback on chains variables and evaluate script expression like MVEL.
     *
     * @since 8.3
     * @param ctx Automation Context
     * @param args Operation Parameters
     */
    protected void resolveArguments(OperationContext ctx, Map<String, Object> args) {
        for (String key : args.keySet()) {
            if (args.get(key) instanceof Expression) {
                Object obj = ((Expression) args.get(key)).eval(ctx);
                args.put(key, obj);
            }
        }
        if (ctx.getVars().containsKey(Constants.VAR_RUNTIME_CHAIN)) {
            args.putAll((Map<String, Object>) ctx.getVars().get(Constants.VAR_RUNTIME_CHAIN));
        }
    }

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

    protected InvokableMethod runMethod() {
        try {
            return new InvokableMethod(this, ScriptingOperationImpl.class.getMethod("run", Object.class));
        } catch (NoSuchMethodException | SecurityException e) {
            throw new UnsupportedOperationException("Cannot use reflection for run method", e);
        }
    }

}
