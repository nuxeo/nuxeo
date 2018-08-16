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
 * Sets the outcome for JSF navigation.
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 * @since 5.4.2
 */
@Operation(id = SetOutcome.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Set JSF Outcome", description = "Set the 'Outcome' context variable that represent a JSF outcome string. This outcome can be used by the next operations that need an outcome. It preserves the current input (e.g returns back the same input).", aliases = {
        "WebUI.SetJSFOutcome" })
public class SetOutcome {

    public static final String ID = "Seam.SetOutcome";

    @Context
    protected OperationContext ctx;

    @Param(name = "outcome")
    protected String outcome;

    @OperationMethod
    public void run() {
        ctx.put(SeamOperation.OUTCOME, outcome);
    }

}
