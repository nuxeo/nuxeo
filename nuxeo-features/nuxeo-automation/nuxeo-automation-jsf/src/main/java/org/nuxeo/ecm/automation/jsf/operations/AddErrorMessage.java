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

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Operation(id = AddErrorMessage.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Add Error Message", description = "Add a message to be displayed in case the chain execution fails. This is a void operation - the input will be returned back as output", aliases = {
        "WebUI.AddErrorMessage" })
public class AddErrorMessage {

    public static final String ID = "Seam.AddErrorMessage";

    @Context
    protected OperationContext ctx;

    @Param(name = "message")
    protected String message;

    @OperationMethod
    public void run() {
        ctx.put(ID, message);
    }

}
