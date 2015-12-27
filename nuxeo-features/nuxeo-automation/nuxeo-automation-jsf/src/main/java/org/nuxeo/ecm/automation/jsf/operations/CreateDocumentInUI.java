/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.jsf.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.jsf.OperationHelper;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Creates a document (equivalent to clicking on the 'create' button on a document creation form).
 *
 * @since 5.4.2
 */
@Operation(id = CreateDocumentInUI.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Create Document in UI", description = "Creates a document in UI, "
        + "as if user was hitting the 'Create' button on a the document creation form. "
        + "It assumes that the contextual 'changeableDocument' document from the Seam context has been updated "
        + "to hold properties defined for creation. It will navigate to the newly created document context, "
        + "set its view as outcome, and return the newly created document.")
public class CreateDocumentInUI {

    public static final String ID = "Seam.CreateDocumentInUI";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public DocumentModel run() {
        ctx.put(SeamOperation.OUTCOME, OperationHelper.getDocumentActions().saveDocument());
        return OperationHelper.getNavigationContext().getCurrentDocument();
    }

}
