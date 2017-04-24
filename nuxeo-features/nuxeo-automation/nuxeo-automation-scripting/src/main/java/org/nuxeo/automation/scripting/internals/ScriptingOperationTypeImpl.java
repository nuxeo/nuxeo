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
package org.nuxeo.automation.scripting.internals;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;

/**
 * @since 7.2
 */
public class ScriptingOperationTypeImpl implements OperationType {

    protected final AutomationScriptingServiceImpl scripting;

    protected final AutomationService automation;

    protected final ScriptingOperationDescriptor desc;

    protected final InvokableMethod method = runMethod();

    public ScriptingOperationTypeImpl(AutomationScriptingServiceImpl scripting, AutomationService automation,
            ScriptingOperationDescriptor desc) {
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
    public Object newInstance(OperationContext ctx, Map<String, Object> args) throws OperationException {
        Map<String, Object> params = new HashMap<>(args);
        scripting.paramsInjector.inject(params, ctx, desc);
        return new ScriptingOperationImpl(desc.source, ctx, params);
    }

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
