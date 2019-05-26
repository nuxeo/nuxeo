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

import java.util.List;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.jsf.OperationHelper;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsPersistenceManager;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Operation(id = FetchFromWorklist.ID, category = Constants.CAT_FETCH, requires = Constants.SEAM_CONTEXT, label = "UI Worklist", description = "Get worklist content from the UI context.")
public class FetchFromWorklist {

    public static final String ID = "Seam.FetchFromWorklist";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public DocumentModelList run() {
        List<DocumentModel> res = null;
        if (OperationHelper.isSeamContextAvailable()) {
            res = OperationHelper.getDocumentListManager().getWorkingList(DocumentsListsManager.DEFAULT_WORKING_LIST);
        } else {
            DocumentsListsPersistenceManager pm = new DocumentsListsPersistenceManager();
            res = pm.loadPersistentDocumentsLists(ctx.getCoreSession(), ctx.getPrincipal().getName(),
                    DocumentsListsManager.DEFAULT_WORKING_LIST);
        }
        return new DocumentModelListImpl(res);
    }
}
