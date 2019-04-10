/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.api.helper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.routing.api.ActionableObject;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.runtime.api.Framework;

/**
 * An actionable validator allows to run an {@link ActionableObject}.
 *
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
@Deprecated
public class ActionableValidator {

    protected ActionableObject actionnable;

    protected CoreSession session;

    protected Map<String, Serializable> additionalProperties = new HashMap<String, Serializable>();

    public ActionableValidator(ActionableObject actionnable, CoreSession session) {
        this.actionnable = actionnable;
        this.session = session;
    }

    public ActionableValidator(ActionableObject actionnable, CoreSession session,
            Map<String, Serializable> additionalProperties) {
        this.actionnable = actionnable;
        this.session = session;
        this.additionalProperties = additionalProperties;
    }

    public void validate() {
        String chainId = actionnable.getValidateOperationChainId();
        runChain(chainId);
    }

    public void refuse() {
        String chainId = actionnable.getRefuseOperationChainId();
        runChain(chainId);
    }

    protected void runChain(String chainId) {
        AutomationService automationService = getAutomationService();
        try (OperationContext context = new OperationContext(session)) {
            context.put(DocumentRoutingConstants.OPERATION_STEP_DOCUMENT_KEY,
                    actionnable.getDocumentRouteStep(session));
            context.setInput(actionnable.getAttachedDocuments(session));
            context.putAll(additionalProperties);
            automationService.run(context, chainId);
        } catch (OperationException e) {
            throw new NuxeoException(e);
        }
    }

    protected AutomationService getAutomationService() {
        return Framework.getService(AutomationService.class);
    }

}
