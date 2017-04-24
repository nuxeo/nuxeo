/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.automation.scripting.internals;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

public class AutomationScriptingRegistry extends SimpleContributionRegistry<ScriptingOperationDescriptor> {

    protected AutomationScriptingServiceImpl scripting;

    protected AutomationService automation;

    protected final Map<String,OperationType> registration = new HashMap<>();

    @Override
    public String getContributionId(ScriptingOperationDescriptor contrib) {
        return contrib.getId();
    }

    @Override
    public void contributionRemoved(String id, ScriptingOperationDescriptor origContrib) {
        automation.removeOperation(registration.remove(id));
    }

    @Override
    public void contributionUpdated(String id, ScriptingOperationDescriptor contrib,
            ScriptingOperationDescriptor newOrigContrib) {
        ScriptingOperationTypeImpl type = new ScriptingOperationTypeImpl(scripting,
                automation, contrib);
        try {
            automation.putOperation(type, true);
        } catch (OperationException cause) {
            throw new NuxeoException("Cannot update scripting operation " + id, cause);
        }
        registration.put(id, type);
    }


}
