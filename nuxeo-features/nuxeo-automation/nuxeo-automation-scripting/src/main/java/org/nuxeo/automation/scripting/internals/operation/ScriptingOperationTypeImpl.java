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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.automation.scripting.api.AutomationScriptingService;
import org.nuxeo.automation.scripting.api.ScriptingException;
import org.nuxeo.automation.scripting.internals.AutomationScriptingServiceImpl;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;
import org.nuxeo.ecm.automation.core.scripting.Expression;

/**
 * @since 7.2
 */
public class ScriptingOperationTypeImpl implements OperationType {

    protected final AutomationScriptingService scripting;

    protected final AutomationService automation;

    protected final ScriptingOperationDescriptor desc;

    protected final InvokableMethod method = runMethod();

    public ScriptingOperationTypeImpl(AutomationScriptingServiceImpl scripting, AutomationService automation,
            ScriptingOperationDescriptor desc) throws ScriptingException {
        this.scripting = scripting;
        this.automation = automation;
        this.desc = desc;
    }

    @Override
    public String getContributingComponent() {
        return "org.nuxeo.automation.scripting.internals.AutomationScriptingComponent";
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
    public Object newInstance(OperationContext ctx, Map<String, ?> args) throws OperationException {
       return new ScriptingOperationImpl(desc.source, ctx, resolve(ctx, args));
    }

    /**
     * As
     * {@link org.nuxeo.ecm.automation.core.impl.OperationTypeImpl#inject(org.nuxeo.ecm.automation.OperationContext, java.util.Map, java.lang.Object)}
     * is not called in this OperationTypeImpl, we have to inject into arguments all context variables to play the
     * fallback on chains variables and evaluate script expression like MVEL.
     *
     * @since 8.3
     * @param ctx
     *            Automation Context
     * @param args
     *            Operation Parameters
     */
    protected Map<String, Object> resolve(OperationContext ctx, Map<String, ?> args) {
        Map<String, Object> resolved = args.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue() instanceof Expression ? ((Expression) e.getValue()).eval(ctx) : e.getValue()));

        if (ctx.getVars().containsKey(Constants.VAR_RUNTIME_CHAIN)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) ctx.getVars().get(Constants.VAR_RUNTIME_CHAIN);
            resolved.putAll(m);
        }
        return resolved;
    }

//    @Override
//    protected String getParamDocumentationType(Class<?> type, boolean isIterable) {
//        String t;
//        if (DocumentModel.class.isAssignableFrom(type) || DocumentRef.class.isAssignableFrom(type)) {
//            t = isIterable ? Constants.T_DOCUMENTS : Constants.T_DOCUMENT;
//        } else if (DocumentModelList.class.isAssignableFrom(type) || DocumentRefList.class.isAssignableFrom(type)) {
//            t = Constants.T_DOCUMENTS;
//        } else if (BlobList.class.isAssignableFrom(type)) {
//            t = Constants.T_BLOBS;
//        } else if (Blob.class.isAssignableFrom(type)) {
//            t = isIterable ? Constants.T_BLOBS : Constants.T_BLOB;
//        } else if (URL.class.isAssignableFrom(type)) {
//            t = Constants.T_RESOURCE;
//        } else if (Calendar.class.isAssignableFrom(type)) {
//            t = Constants.T_DATE;
//        } else {
//            t = type.getSimpleName().toLowerCase();
//        }
//        return t;
//    }

    @Override
    public Class<?> getType() {
        return ScriptingOperationImpl.class;
    }

    @Override
    public AutomationService getService() {
        return automation;
    }

    @Override
    public InvokableMethod[] getMethodsMatchingInput(Class<?> in) {
        return new InvokableMethod[] { method };
    }

    @Override
    public List<InvokableMethod> getMethods() {
        return Collections.singletonList(method);
    }

    protected InvokableMethod runMethod() {
        try {
            return new InvokableMethod(this, ScriptingOperationImpl.class.getMethod("run"));
        } catch (ReflectiveOperationException cause) {
            throw new Error("Cannot reference run method of " + ScriptingOperationImpl.class);
        }
    }

}
