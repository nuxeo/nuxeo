/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.automation.jsf.operations;

import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Operation(id = PushToSeamContext.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Push to Seam Context", description = "Push the current input document into Seam context. Returns back the document.", aliases = {
        "WebUI.PushDocumentToSeamContext" })
public class PushToSeamContext {

    public static final String ID = "Seam.PushDocument";

    @Context
    protected OperationContext ctx;

    @Param(name = "name")
    protected String name;

    @Param(name = "scope", widget = Constants.W_OPTION, values = { "session", "conversation", "page", "event" })
    protected String scope;

    @OperationMethod
    public DocumentModel push(DocumentModel value) {
        if ("session".equalsIgnoreCase(scope)) {
            Contexts.getSessionContext().set(name, value);
        } else if ("conversation".equalsIgnoreCase(scope)) {
            Contexts.getConversationContext().set(name, value);
        } else if ("page".equalsIgnoreCase(scope)) {
            Contexts.getPageContext().set(name, value);
        } else if ("event".equalsIgnoreCase(scope)) {
            Contexts.getEventContext().set(name, value);
        }

        return value;
    }

}
