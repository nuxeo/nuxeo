/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
@Operation(id = GetSelectedDocuments.ID, category = Constants.CAT_FETCH, requires=Constants.SEAM_CONTEXT,
        label = "UI Selected documents", description = "Fetch the documents selected in the current folder listing")
public class GetSelectedDocuments {

    public static final String ID = "Seam.GetSelectedDocuments";

    protected @Context OperationContext ctx;

    @OperationMethod
    public DocumentModelList run() throws Exception {
        List<DocumentModel> res = OperationHelper.getDocumentListManager().getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
        return new DocumentModelListImpl(res);
    }

}
