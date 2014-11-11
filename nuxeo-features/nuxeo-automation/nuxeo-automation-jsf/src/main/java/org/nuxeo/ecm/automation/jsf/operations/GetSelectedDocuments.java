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
 */
@Operation(id = GetSelectedDocuments.ID, category = Constants.CAT_FETCH, requires = Constants.SEAM_CONTEXT, label = "UI Selected documents", description = "Fetch the documents selected in the current folder listing")
public class GetSelectedDocuments {

    public static final String ID = "Seam.GetSelectedDocuments";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public DocumentModelList run() {
        List<DocumentModel> res = OperationHelper.getDocumentListManager().getWorkingList(
                DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
        return new DocumentModelListImpl(res);
    }

}
