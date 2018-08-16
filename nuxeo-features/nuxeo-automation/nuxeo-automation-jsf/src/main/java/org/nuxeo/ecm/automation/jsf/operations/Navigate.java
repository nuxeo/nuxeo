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
import org.nuxeo.ecm.automation.jsf.OperationHelper;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Operation(id = Navigate.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Navigate to Document", description = "Navigate to the input document. The outcome of the UI action will be stored in the operation chain context as the 'Outcome' variable. Returns back the document.", aliases = {
        "WebUI.NavigateTo" })
public class Navigate {

    public static final String ID = "Seam.NavigateTo";

    @Context
    protected OperationContext ctx;

    @Param(name = "view", required = false)
    protected String view;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        String outcome = null;

        if (view == null) {
            outcome = OperationHelper.getNavigationContext().navigateToDocument(doc);
        } else {
            outcome = OperationHelper.getNavigationContext().navigateToDocument(doc, view);
        }

        ctx.put(SeamOperation.OUTCOME, outcome);

        return doc;
    }
}
