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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsPersistenceManager;

import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.DEFAULT_WORKING_LIST;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Operation(id = PushToWorklist.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Push to Worklist",
        description = "Add the input document(s) to worklist. Returns back the document(s)")
public class PushToWorklist {

    public static final String ID = "Seam.AddToWorklist";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        if (OperationHelper.isSeamContextAvailable()) {
            OperationHelper.getDocumentListManager().addToWorkingList(
                    DEFAULT_WORKING_LIST, doc);
        }
        else {
            DocumentsListsPersistenceManager pm = new DocumentsListsPersistenceManager();
            pm.addDocumentToPersistentList(ctx.getPrincipal().getName(), DEFAULT_WORKING_LIST, doc);
        }
        return doc;
    }

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) {
        if (OperationHelper.isSeamContextAvailable()) {
            OperationHelper.getDocumentListManager().addToWorkingList(
                    DEFAULT_WORKING_LIST, docs);
        } else {
            DocumentsListsPersistenceManager pm = new DocumentsListsPersistenceManager();
            for (DocumentModel doc : docs) {
                pm.addDocumentToPersistentList(ctx.getPrincipal().getName(), DEFAULT_WORKING_LIST, doc);
            }
        }
        return docs;
    }

}
