/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.collections.jsf.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.Messages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.collections.api.CollectionManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.9.3
 */
@Name("collectionActions")
@Scope(ScopeType.PAGE)
@BypassInterceptors
public class CollectionActionsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String COLLECTION_CURRENT_SELECTION = "COLLECTION_CURRENT_SELECTION";

    public static final String DOCUMENT_ADDED_TO_COLLECTION_EVENT = "documentAddedToCollection";

    public static final String DOCUMENT_REMOVED_FROM_COLLECTION_EVENT = "documentRemovedFromCollection";

    private static final Log log = LogFactory.getLog(CollectionActionsBean.class);

    protected static void addFacesMessage(StatusMessage.Severity severity,
            String message, String arguments) {
        final FacesMessages facesMessages = (FacesMessages) Component.getInstance(
                "facesMessages", true);
        facesMessages.add(severity, Messages.instance().get(message),
                Messages.instance().get(arguments));
    }

    private List<String> docUidsToBeAdded;

    private String newDescription;

    private String newTitle;

    private DocumentModel selectedCollection;

    private String selectedCollectionUid;

    public void addCurrentDocumentToSelectedCollection() throws ClientException {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument != null) {
            final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
            final CoreSession session = (CoreSession) Component.getInstance(
                    "documentManager", true);
            if (isCreateNewCollection()) {
                collectionManager.addToNewCollection(getNewTitle(),
                        getNewDescription(), currentDocument, session);
            } else {
                collectionManager.addToCollection(getSelectedCollection(),
                        currentDocument, session);
            }

            Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED);

            navigationContext.invalidateCurrentDocument();

            addFacesMessage(StatusMessage.Severity.INFO,
                    "collection.addedToCollection",
                    isCreateNewCollection() ? getNewTitle()
                            : getSelectedCollection().getTitle());
        }
    }

    public void addCurrentSelectionToSelectedCollection()
            throws ClientException {
        final DocumentsListsManager documentsListsManager = getDocumentsListsManager();
        addToSelectedCollection(documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION));
    }

    public void addDocUidsToBeAddedToCurrentCollection() throws ClientException {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        final CoreSession session = (CoreSession) Component.getInstance(
                "documentManager", true);

        List<DocumentModel> documentListToBeAdded = new ArrayList<DocumentModel>(
                docUidsToBeAdded.size());

        for (String uid : docUidsToBeAdded) {
            documentListToBeAdded.add(session.getDocument(new IdRef(uid)));
        }

        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        collectionManager.addToCollection(currentDocument,
                documentListToBeAdded, session);

        Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED);

        addFacesMessage(StatusMessage.Severity.INFO,
                "collection.allAddedToCollection", currentDocument.getTitle());
    }

    public void addToSelectedCollection(
            final List<DocumentModel> documentListToBeAdded)
            throws ClientException {
        if (documentListToBeAdded != null && !documentListToBeAdded.isEmpty()) {
            final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
            final CoreSession session = (CoreSession) Component.getInstance(
                    "documentManager", true);
            if (isCreateNewCollection()) {
                collectionManager.addToNewCollection(getNewTitle(),
                        getNewDescription(), documentListToBeAdded, session);
            } else {
                collectionManager.addToCollection(getSelectedCollection(),
                        documentListToBeAdded, session);
            }

            Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED);

            addFacesMessage(StatusMessage.Severity.INFO,
                    "collection.allAddedToCollection",
                    isCreateNewCollection() ? getNewTitle()
                            : getSelectedCollection().getTitle());
        }
    }

    public boolean canAddSelectedDocumentBeCollected() {
        final DocumentsListsManager documentsListsManager = getDocumentsListsManager();
        List<DocumentModel> documents = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
        if (documents == null || documents.isEmpty()) {
            return false;
        }
        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        for (DocumentModel doc : documents) {
            if (!collectionManager.isCollectable(doc)) {
                return false;
            }
        }
        return true;
    }

    public boolean canAddToCollection(DocumentModel collection)
            throws ClientException {
        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        final CoreSession session = (CoreSession) Component.getInstance(
                "documentManager", true);
        final boolean result = collection != null
                && collectionManager.isCollection(collection)
                && collectionManager.canAddToCollection(collection, session);
        return result;
    }

    public boolean canAddToDocsToCurrentCollection() throws ClientException {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        final CoreSession session = (CoreSession) Component.getInstance(
                "documentManager", true);
        return collectionManager.canAddToCollection(currentDocument, session);
    }

    public boolean canAddToSelectedCollection() throws ClientException {
        final boolean result = canAddToCollection(getSelectedCollection())
                || isCreateNewCollection();
        return result;
    }

    public void cancel() {
        selectedCollectionUid = null;
        newDescription = null;
        newTitle = null;
        docUidsToBeAdded = null;
    }

    public boolean canCurrentDocumentBeCollected() {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        return collectionManager.isCollectable(currentDocument);
    }

    public boolean canManage(final DocumentModel collection)
            throws ClientException {
        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        final CoreSession session = (CoreSession) Component.getInstance(
                "documentManager", true);
        return collectionManager.canManage(collection, session);
    }

    public boolean canRemoveFromCollection() throws ClientException {
        final DocumentsListsManager documentsListsManager = getDocumentsListsManager();
        final List<DocumentModel> doccumentListToBeRemoved = documentsListsManager.getWorkingList(COLLECTION_CURRENT_SELECTION);
        if (doccumentListToBeRemoved == null
                || doccumentListToBeRemoved.isEmpty()) {
            return false;
        }
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return canAddToCollection(currentDocument);
    }

    public boolean canRemoveFromCollection(DocumentModel collection)
            throws ClientException {
        return canAddToCollection(collection);
    }

    public List<String> getDocUidsToBeAdded() {
        return docUidsToBeAdded;
    }

    protected DocumentsListsManager getDocumentsListsManager() {
        return (DocumentsListsManager) Component.getInstance(
                "documentsListsManager", true);
    }

    public List<DocumentModel> getMultipleDocumentToBeAdded() {
        final DocumentsListsManager documentsListsManager = getDocumentsListsManager();
        return documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
    }

    public String getNewDescription() {
        return newDescription;
    }

    public String getNewTitle() {
        return newTitle;
    }

    public DocumentModel getSelectedCollection() {
        if (selectedCollection == null
                && StringUtils.isNotBlank(selectedCollectionUid)
                && !isCreateNewCollection()) {
            try {
                final CoreSession session = (CoreSession) Component.getInstance(
                        "documentManager", true);
                selectedCollection = session.getDocument(new IdRef(
                        selectedCollectionUid));
            } catch (ClientException e) {
                log.error("Cannot fetch collection");
            }
        }
        return selectedCollection;
    }

    public String getSelectedCollectionDescription() throws PropertyException,
            ClientException {
        if (isCreateNewCollection()) {
            return null;
        } else {
            return getSelectedCollection().getProperty("dc:description").getValue().toString();
        }
    }

    public String getSelectedCollectionUid() {
        return selectedCollectionUid;
    }

    public boolean hasCurrentDocumentVisibleCollection() throws ClientException {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return hasVisibleCollection(currentDocument);
    }

    public boolean hasVisibleCollection(DocumentModel doc) {
        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        if (doc == null || !collectionManager.isCollectable(doc)) {
            return false;
        }
        if (collectionManager.isCollected(doc)) {
            final CoreSession session = (CoreSession) Component.getInstance(
                    "documentManager", true);
            return collectionManager.hasVisibleCollection(doc, session);
        }
        return false;
    }

    public boolean isCreateNewCollection() {
        return selectedCollectionUid != null
                && selectedCollectionUid.startsWith(CollectionConstants.MAGIC_PREFIX_ID);
    }

    public boolean isCurrentDocumentCollection() {
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        final DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        return collectionManager.isCollection(currentDocument);
    }

    public void removeCurrentDocumentFromCollection(final ActionEvent event)
            throws ClientException {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext eContext = context.getExternalContext();
        String collectionId = eContext.getRequestParameterMap().get(
                "collectionId");
        if (StringUtils.isNotBlank(collectionId)) {
            final CoreSession session = (CoreSession) Component.getInstance(
                    "documentManager", true);
            final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
            final DocumentRef collectionRef = new IdRef(collectionId);
            if (session.exists(collectionRef)) {
                final DocumentModel collection = session.getDocument(collectionRef);
                if (collectionManager.canAddToCollection(collection, session)) {
                    final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                            "navigationContext", true);
                    final DocumentModel currentDocument = navigationContext.getCurrentDocument();
                    collectionManager.removeFromCollection(collection,
                            currentDocument, session);

                    Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED);

                    addFacesMessage(StatusMessage.Severity.INFO,
                            "collection.removeCurrentDocumentFromCollection",
                            collection.getTitle());
                }
            }
        }
    }

    public void removeCurrentSelectionFromCollection() throws ClientException {
        final DocumentsListsManager documentsListsManager = getDocumentsListsManager();
        final List<DocumentModel> doccumentListToBeRemoved = documentsListsManager.getWorkingList(COLLECTION_CURRENT_SELECTION);
        final NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext", true);
        final DocumentModel collection = navigationContext.getCurrentDocument();
        removeFromCollection(collection, doccumentListToBeRemoved);
        documentsListsManager.resetWorkingList(COLLECTION_CURRENT_SELECTION);
    }

    public void removeFromCollection(DocumentModel collection,
            List<DocumentModel> documentListToBeRemoved) throws ClientException {
        final CollectionManager collectionManager = Framework.getLocalService(CollectionManager.class);
        final CoreSession session = (CoreSession) Component.getInstance(
                "documentManager", true);
        collectionManager.removeAllFromCollection(collection,
                documentListToBeRemoved, session);

        Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED);

        addFacesMessage(StatusMessage.Severity.INFO,
                "collection.removeCurrentSelectionFromCollection",
                collection.getTitle());
    }

    public void removeFromMultipleDocumentToBeAdded(ActionEvent event) {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext eContext = context.getExternalContext();
        String index = eContext.getRequestParameterMap().get("index");

        final DocumentsListsManager documentsListsManager = getDocumentsListsManager();
        final DocumentModel toBeRemovedFromWorkingList = documentsListsManager.getWorkingList(
                DocumentsListsManager.CURRENT_DOCUMENT_SELECTION).get(
                Integer.valueOf(index).intValue());
        documentsListsManager.removeFromWorkingList(
                DocumentsListsManager.CURRENT_DOCUMENT_SELECTION,
                toBeRemovedFromWorkingList);
    }

    public void setDocUidsToBeAdded(final List<String> docUidsToBeAdded) {
        this.docUidsToBeAdded = docUidsToBeAdded;
    }

    public void setNewDescription(final String newDescription) {
        this.newDescription = newDescription;
    }

    public void setNewTitle(final String newTitle) {
        this.newTitle = newTitle;
    }

    public void setSelectedCollectionUid(final String selectedCollectionUid) {
        this.selectedCollection = null;
        this.selectedCollectionUid = selectedCollectionUid;
        if (isCreateNewCollection()) {
            setNewTitle(selectedCollectionUid.substring(CollectionConstants.MAGIC_PREFIX_ID.length()));
        }
    }

}
