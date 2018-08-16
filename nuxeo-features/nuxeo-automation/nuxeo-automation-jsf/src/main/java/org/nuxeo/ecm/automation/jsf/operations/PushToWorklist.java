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

import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.DEFAULT_WORKING_LIST;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.jsf.OperationHelper;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsPersistenceManager;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Operation(id = PushToWorklist.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Push to Worklist", description = "Add the input document(s) to worklist. Returns back the document(s)", aliases = {
        "WebUI.AddToWorklist" })
public class PushToWorklist {

    public static final String ID = "Seam.AddToWorklist";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        if (OperationHelper.isSeamContextAvailable()) {
            OperationHelper.getDocumentListManager().addToWorkingList(DEFAULT_WORKING_LIST, doc);
        } else {
            DocumentsListsPersistenceManager pm = new DocumentsListsPersistenceManager();
            pm.addDocumentToPersistentList(ctx.getPrincipal().getName(), DEFAULT_WORKING_LIST, doc);
        }
        return doc;
    }

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) {
        if (OperationHelper.isSeamContextAvailable()) {
            OperationHelper.getDocumentListManager().addToWorkingList(DEFAULT_WORKING_LIST, docs);
        } else {
            DocumentsListsPersistenceManager pm = new DocumentsListsPersistenceManager();
            for (DocumentModel doc : docs) {
                pm.addDocumentToPersistentList(ctx.getPrincipal().getName(), DEFAULT_WORKING_LIST, doc);
            }
        }
        return docs;
    }

}
