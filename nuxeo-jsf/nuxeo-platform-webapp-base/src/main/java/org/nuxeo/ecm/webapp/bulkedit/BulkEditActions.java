/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.webapp.bulkedit;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;

import static org.jboss.seam.ScopeType.CONVERSATION;

/**
 * Handles Bulk Edit actions.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@Name("bulkEditActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class BulkEditActions implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true)
    protected transient TypeManager typeManager;

    @In(create = true)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    protected DocumentModel fictiveDocumentModel;

    /**
     * Returns the common layouts of the current selected documents for the
     * {@code edit} mode.
     */
    public List<String> getCommonsLayouts() {
        if (documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            return Collections.emptyList();
        }

        List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
        return BulkEditHelper.getCommonLayouts(typeManager, selectedDocuments);
    }

    /**
     * Returns the common schemas for the current selected documents.
     */
    protected List<String> getCommonSchemas() {
        if (documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            return Collections.emptyList();
        }

        List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
        return BulkEditHelper.getCommonSchemas(selectedDocuments);
    }

    @Factory(value = "bulkEditDocumentModel", scope = ScopeType.EVENT)
    public DocumentModel getBulkEditDocumentModel() {
        if (fictiveDocumentModel == null) {
            fictiveDocumentModel = new SimpleDocumentModel(getCommonSchemas());
        }
        return fictiveDocumentModel;
    }

    public String bulkEditSelection() throws ClientException {
        bulkEditSelectionNoRedirect();
        return navigationContext.navigateToDocument(navigationContext.getCurrentDocument());
    }

    public void bulkEditSelectionNoRedirect() throws ClientException {
        if (fictiveDocumentModel != null) {
            List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
            BulkEditHelper.copyMetadata(documentManager, fictiveDocumentModel,
                    selectedDocuments);
            fictiveDocumentModel = null;
        }
    }

    public void cancel() {
        fictiveDocumentModel = null;
    }

}
