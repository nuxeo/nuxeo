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
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsPersistenceManager;

/**
 * @author Anahide Tchertchian
 */
@Operation(id = ClearWorklist.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Clear Worklist", description = "Clear the worklist content.")
public class ClearWorklist {

    public static final String ID = "Seam.ClearWorklist";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public void run() {
        if (OperationHelper.isSeamContextAvailable()) {
            OperationHelper.getDocumentListManager().resetWorkingList(
                    DocumentsListsManager.DEFAULT_WORKING_LIST);
        }
        else {
            DocumentsListsPersistenceManager pm = new DocumentsListsPersistenceManager();
            pm.clearPersistentList(ctx.getPrincipal().getName(), DocumentsListsManager.DEFAULT_WORKING_LIST);
        }
    }

}
