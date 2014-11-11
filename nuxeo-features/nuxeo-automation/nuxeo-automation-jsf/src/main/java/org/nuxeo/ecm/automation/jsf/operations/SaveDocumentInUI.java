/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * Saves a document (equivalent to clicking on the 'save' button on a document
 * edition form).
 *
 * @since 5.4.2
 */
@Operation(id = SaveDocumentInUI.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Save Document in UI", description = "Saves a document in UI, "
        + "as if user was hitting the 'Save' button on a the document edition form. "
        + "It assumes that the contextual 'currentDocument' document from the Seam context has been updated "
        + "to hold the new properties. It will navigate to the edited document context, "
        + "set its view as outcome, and return it.")
public class SaveDocumentInUI {

    public static final String ID = "Seam.SaveDocumentInUI";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public DocumentModel run() throws Exception {
        ctx.put(SeamOperation.OUTCOME,
                OperationHelper.getDocumentActions().updateCurrentDocument());
        return OperationHelper.getNavigationContext().getCurrentDocument();
    }

}
