/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Antoine Taillefer
 */

package org.nuxeo.ecm.automation.jsf.operations;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.jsf.OperationHelper;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsPersistenceManager;

/**
 * Gets selected documents from the selection list passed as a parameter.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
@Operation(id = GetDocumentsFromSelectionList.ID, category = Constants.CAT_FETCH, requires = Constants.SEAM_CONTEXT, label = "UI Selected documents form list", description = "Fetch the documents selected in the selection list passed as a parameter. If the list name is empty, the current folder selection list is used.")
public class GetDocumentsFromSelectionList {

    public static final String ID = "Seam.GetDocumentsFromSelectionList";

    @Context
    protected OperationContext ctx;

    @Param(name = "listName", required = false)
    protected String listName;

    @OperationMethod
    public DocumentModelList run() throws OperationException {

        String workingListName = listName;
        if (StringUtils.isEmpty(workingListName)) {
            workingListName = DocumentsListsManager.CURRENT_DOCUMENT_SELECTION;
        }

        List<DocumentModel> res = null;
        if (OperationHelper.isSeamContextAvailable()) {
            res = OperationHelper.getDocumentListManager().getWorkingList(
                    workingListName);
        } else {
            if (OperationHelper.getDocumentListManager().getWorkingListDescriptor(
                    workingListName).getPersistent()) {
                DocumentsListsPersistenceManager pm = new DocumentsListsPersistenceManager();
                res = pm.loadPersistentDocumentsLists(ctx.getCoreSession(),
                        ctx.getPrincipal().getName(), workingListName);
            } else {
                throw new OperationException(
                        String.format(
                                "Cannot get selected documents from selection list '%s' because the Seam context is not available and this list is not persisted.",
                                workingListName));
            }
        }
        return new DocumentModelListImpl(res);
    }

}
