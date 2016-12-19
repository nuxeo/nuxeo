/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.webapp.bulkedit;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_SELECTION;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

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

    public static final String SELECTION_EDITED = "selectionEdited";

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true)
    protected transient TypeManager typeManager;

    @In(create = true)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected DocumentModel fictiveDocumentModel;

    /**
     * Returns the common layouts of the current selected documents for the {@code edit} mode.
     */
    public List<String> getCommonsLayouts() {
        if (documentsListsManager.isWorkingListEmpty(CURRENT_DOCUMENT_SELECTION)) {
            return Collections.emptyList();
        }

        List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SELECTION);
        return BulkEditHelper.getCommonLayouts(typeManager, selectedDocuments);
    }

    /**
     * Returns the common schemas for the current selected documents.
     *
     * @deprecated not yet used since 5.7
     */
    protected List<String> getCommonSchemas() {
        if (documentsListsManager.isWorkingListEmpty(CURRENT_DOCUMENT_SELECTION)) {
            return Collections.emptyList();
        }

        List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SELECTION);
        return BulkEditHelper.getCommonSchemas(selectedDocuments);
    }

    public DocumentModel getBulkEditDocumentModel() {
        if (fictiveDocumentModel == null) {
            fictiveDocumentModel = new SimpleDocumentModel();
        }
        return fictiveDocumentModel;
    }

    public String bulkEditSelection() {
        if (fictiveDocumentModel != null) {
            List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SELECTION);
            Framework.getLocalService(BulkEditService.class).updateDocuments(documentManager, fictiveDocumentModel,
                    selectedDocuments);

            for (DocumentModel doc : selectedDocuments) {
                Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED, doc);
            }

            facesMessages.add(StatusMessage.Severity.INFO, messages.get("label.bulk.edit.documents.updated"),
                    selectedDocuments.size());

            Events.instance().raiseEvent(SELECTION_EDITED, selectedDocuments, fictiveDocumentModel);
            fictiveDocumentModel = null;
        }
        return null;
    }

    public boolean getCanEdit() {
        if (documentsListsManager.isWorkingListEmpty(CURRENT_DOCUMENT_SELECTION)) {
            return false;
        }

        List<DocumentModel> docs = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SELECTION);
        for (DocumentModel doc : docs) {
            if (!documentManager.hasPermission(doc.getRef(), SecurityConstants.WRITE)) {
                return false;
            }
        }
        return true;
    }

    @Observer(CURRENT_DOCUMENT_SELECTION + "Updated")
    public void cancel() {
        fictiveDocumentModel = null;
    }

}
