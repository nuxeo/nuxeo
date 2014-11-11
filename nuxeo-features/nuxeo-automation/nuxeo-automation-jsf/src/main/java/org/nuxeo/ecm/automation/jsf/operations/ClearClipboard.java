/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 */

package org.nuxeo.ecm.automation.jsf.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.jsf.OperationHelper;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;

/**
 * @author Anahide Tchertchian
 */
@Operation(id = ClearClipboard.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Clear Clipboard", description = "Clear the clipboard content.")
public class ClearClipboard {

    public static final String ID = "Seam.ClearClipboard";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public void run() {
        OperationHelper.getDocumentListManager().resetWorkingList(
                DocumentsListsManager.CLIPBOARD);
    }

}
