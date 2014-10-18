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

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_SELECTION;

import java.io.Serializable;
import java.util.ArrayList;
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
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.tag.TagService;
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
     * Returns the common layouts of the current selected documents for the
     * {@code edit} mode.
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

    public String bulkEditSelection() throws ClientException {
        if (fictiveDocumentModel != null) {
            List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SELECTION);
            Framework.getLocalService(BulkEditService.class).updateDocuments(
                    documentManager, fictiveDocumentModel, selectedDocuments);

            for (DocumentModel doc : selectedDocuments) {
                Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED, doc);
            }

            // handle tags
            addTagsOnDocuments(selectedDocuments, fictiveDocumentModel);

            // handle collections
            addCollectionsOnDocuments(selectedDocuments, fictiveDocumentModel);

            facesMessages.add(StatusMessage.Severity.INFO,
                    messages.get("label.bulk.edit.documents.updated"),
                    selectedDocuments.size());

            Events.instance().raiseEvent(SELECTION_EDITED, selectedDocuments,
                    fictiveDocumentModel);
            fictiveDocumentModel = null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void addTagsOnDocuments(List<DocumentModel> documents,
            DocumentModel importDocumentModel) throws ClientException {
        List<String> tags = (List<String>) importDocumentModel.getContextData(
                ScopeType.REQUEST, "bulk_edit_tags");
        if (tags != null && !tags.isEmpty()) {
            TagService tagService = Framework.getLocalService(TagService.class);
            String username = documentManager.getPrincipal().getName();
            for (DocumentModel doc : documents) {
                for (String tag : tags) {
                    tagService.tag(documentManager, doc.getId(), tag, username);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void addCollectionsOnDocuments(List<DocumentModel> documents,
            DocumentModel importDocumentModel) throws ClientException {
        List<String> collectionIds = (List<String>) importDocumentModel.getContextData(
                ScopeType.REQUEST, "bulk_edit_collections");
        if (collectionIds != null && !collectionIds.isEmpty()) {
            List<DocumentModel> collections = new ArrayList<>();
            for (String collectionId : collectionIds) {
                IdRef idRef = new IdRef(collectionId);
                if (documentManager.exists(idRef)) {
                    collections.add(documentManager.getDocument(idRef));
                }
            }

            CollectionManager collectionManager = Framework.getService(CollectionManager.class);
            for (DocumentModel collection : collections) {
                if (collectionManager.canAddToCollection(collection,
                        documentManager)) {
                    collectionManager.addToCollection(collection, documents,
                            documentManager);
                }
            }
        }
    }

    /**
     *
     * @deprecated since 5.7. Use
     *             {@link org.nuxeo.ecm.webapp.bulkedit.BulkEditActions#bulkEditSelection()}
     *             .
     */
    @Deprecated
    public void bulkEditSelectionNoRedirect() throws ClientException {
        bulkEditSelection();
    }

    public boolean getCanEdit() throws ClientException {
        if (documentsListsManager.isWorkingListEmpty(CURRENT_DOCUMENT_SELECTION)) {
            return false;
        }

        List<DocumentModel> docs = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SELECTION);
        for (DocumentModel doc : docs) {
            if (!documentManager.hasPermission(doc.getRef(),
                    SecurityConstants.WRITE)) {
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
