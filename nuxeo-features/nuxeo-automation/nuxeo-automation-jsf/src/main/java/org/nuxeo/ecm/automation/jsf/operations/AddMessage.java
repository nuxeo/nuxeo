/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.automation.jsf.operations;

import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * Operation that displays a feedback message.
 * <p>
 * The {@code message} will be internationalized if available.
 * <p>
 * The severity of the message can be:
 * <ul>
 * <li>INFO</li>
 * <li>WARN</li>
 * <li>ERROR</li>
 * </ul>
 * The default one is INFO.
 * <p>
 * Message parameters, if any, are extracted form the {@link OperationContext} using the
 * {@code AddMessage#MESSAGE_PARAMS_KEY} key.
 * <p>
 * Requires an active Seam context.
 *
 * @since 5.7
 */
@Operation(id = AddMessage.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Add Message", description = "Add a feedback message to be displayed. The message will be internationalized. You can specify the severity of the message using INFO, WARN and ERROR, default is INFO. Messages parameters are extracted from the context using the 'AddMessage.messageParams'.", aliases = {
        "WebUI.AddMessage" })
public class AddMessage {

    public static final String ID = "Seam.AddMessage";

    public static final String MESSAGE_PARAMS_KEY = "AddMessage.messageParams";

    @Context
    protected OperationContext ctx;

    @Param(name = "message")
    protected String message;

    @Param(name = "severity")
    protected String severityStr = StatusMessage.Severity.INFO.name();

    @OperationMethod
    public void run() {
        StatusMessage.Severity severity = StatusMessage.Severity.valueOf(severityStr);
        FacesMessages facesMessages = (FacesMessages) Contexts.getConversationContext().get(FacesMessages.class);
        Object[] params = (Object[]) ctx.get(MESSAGE_PARAMS_KEY);
        if (params == null) {
            params = new Object[0];
        }
        facesMessages.addFromResourceBundle(severity, message, params);
    }

}
