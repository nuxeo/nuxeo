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
 * Creates a document (equivalent to clicking on the 'create' button on a
 * document creation form).
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
    public DocumentModel run() throws Exception {
        ctx.put(SeamOperation.OUTCOME,
                OperationHelper.getDocumentActions().saveDocument());
        return OperationHelper.getNavigationContext().getCurrentDocument();
    }

}
