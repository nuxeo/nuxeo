/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.clipboard;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.cache.SeamCacheHelper;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListDescriptor;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.ScopeType.SESSION;

/**
 * This is the action listener behind the copy/paste template that knows how to copy/paste the selected user data to the
 * target action listener, and also create/remove the corresponding objects into the backend.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Name("clipboardActions")
@Scope(SESSION)
public class ClipboardActionsBean implements ClipboardActions, Serializable {

    private static final long serialVersionUID = -2407222456116573225L;

    private static final Log log = LogFactory.getLog(ClipboardActionsBean.class);

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true)
    protected TypeManager typeManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected transient WebActions webActions; // it is serializable

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    @RequestParameter()
    protected String workListDocId;

    private String currentSelectedList;

    private String previouslySelectedList;

    private transient List<String> availableLists;

    private transient List<DocumentsListDescriptor> descriptorsForAvailableLists;

    private Boolean canEditSelectedDocs;

    private transient Map<String, List<Action>> actionCache;

    @Override
    public void releaseClipboardableDocuments() {
    }

    @Override
    public boolean isInitialized() {
        return documentManager != null;
    }

    @Override
    public void putSelectionInWorkList(Boolean forceAppend) {
        canEditSelectedDocs = null;
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            putSelectionInWorkList(
                    documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION),
                    forceAppend);
            autoSelectCurrentList(DocumentsListsManager.DEFAULT_WORKING_LIST);
        } else {
            log.debug("No selectable Documents in context to process copy on...");
        }
        log.debug("add to worklist processed...");
    }

    @Override
    public void putSelectionInWorkList() {
        putSelectionInWorkList(false);
    }

    @Override
    public void putSelectionInDefaultWorkList() {
        canEditSelectedDocs = null;
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            List<DocumentModel> docsList = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
            Object[] params = { docsList.size() };
            facesMessages.add(StatusMessage.Severity.INFO, "#0 " + messages.get("n_copied_docs"), params);
            documentsListsManager.addToWorkingList(DocumentsListsManager.DEFAULT_WORKING_LIST, docsList);

            // auto select clipboard
            autoSelectCurrentList(DocumentsListsManager.DEFAULT_WORKING_LIST);

        } else {
            log.debug("No selectable Documents in context to process copy on...");
        }
        log.debug("add to worklist processed...");
    }

    @Override
    @WebRemote
    public void putInClipboard(String docId) {
        DocumentModel doc = documentManager.getDocument(new IdRef(docId));
        documentsListsManager.addToWorkingList(DocumentsListsManager.CLIPBOARD, doc);
        Object[] params = { 1 };
        facesMessages.add(StatusMessage.Severity.INFO, "#0 " + messages.get("n_copied_docs"), params);

        autoSelectCurrentList(DocumentsListsManager.CLIPBOARD);
    }

    @Override
    public void putSelectionInClipboard() {
        canEditSelectedDocs = null;
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            List<DocumentModel> docsList = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
            Object[] params = { docsList.size() };
            facesMessages.add(StatusMessage.Severity.INFO, "#0 " + messages.get("n_copied_docs"), params);

            documentsListsManager.addToWorkingList(DocumentsListsManager.CLIPBOARD, docsList);

            // auto select clipboard
            autoSelectCurrentList(DocumentsListsManager.CLIPBOARD);

        } else {
            log.debug("No selectable Documents in context to process copy on...");
        }
        log.debug("add to worklist processed...");
    }

    @Override
    public void putSelectionInWorkList(List<DocumentModel> docsList) {
        putSelectionInWorkList(docsList, false);
    }

    @Override
    public void putSelectionInWorkList(List<DocumentModel> docsList, Boolean forceAppend) {
        canEditSelectedDocs = null;
        if (null != docsList) {
            Object[] params = { docsList.size() };
            facesMessages.add(StatusMessage.Severity.INFO, "#0 " + messages.get("n_added_to_worklist_docs"), params);

            // Add to the default working list
            documentsListsManager.addToWorkingList(getCurrentSelectedListName(), docsList, forceAppend);
            log.debug("Elements copied to clipboard...");

        } else {
            log.debug("No copiedDocs to process copy on...");
        }

        log.debug("add to worklist processed...");
    }

    @Override
    @Deprecated
    public void copySelection(List<DocumentModel> copiedDocs) {
        if (null != copiedDocs) {
            Object[] params = { copiedDocs.size() };
            facesMessages.add(StatusMessage.Severity.INFO, "#0 " + messages.get("n_copied_docs"), params);

            // clipboard.copy(copiedDocs);

            // Reset + Add to clipboard list
            documentsListsManager.resetWorkingList(DocumentsListsManager.CLIPBOARD);
            documentsListsManager.addToWorkingList(DocumentsListsManager.CLIPBOARD, copiedDocs);

            // Add to the default working list
            documentsListsManager.addToWorkingList(copiedDocs);
            log.debug("Elements copied to clipboard...");

        } else {
            log.debug("No copiedDocs to process copy on...");
        }

        log.debug("Copy processed...");
    }

    public boolean exists(DocumentRef ref) {
        return ref != null && documentManager.exists(ref);
    }

    @Override
    public String removeWorkListItem(DocumentRef ref) {
        DocumentModel doc = null;
        if (exists(ref)) {
            doc = documentManager.getDocument(ref);
        } else { // document was permanently deleted so let's use the one in the work list
            List<DocumentModel> workingListDocs = documentsListsManager.getWorkingList(getCurrentSelectedListName());
            for (DocumentModel wDoc : workingListDocs) {
                if (wDoc.getRef().equals(ref)) {
                    doc = wDoc;
                }
            }
        }
        documentsListsManager.removeFromWorkingList(getCurrentSelectedListName(), doc);
        return null;
    }

    @Override
    public String clearWorkingList() {
        documentsListsManager.resetWorkingList(getCurrentSelectedListName());
        return null;
    }

    @Override
    public String pasteDocumentList(String listName) {
        return pasteDocumentList(documentsListsManager.getWorkingList(listName));
    }

    @Override
    public String pasteDocumentListInside(String listName, String docId) {
        return pasteDocumentListInside(documentsListsManager.getWorkingList(listName), docId);
    }

    @Override
    public String pasteDocumentList(List<DocumentModel> docPaste) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (null != docPaste) {
            List<DocumentModel> newDocs = recreateDocumentsWithNewParent(getParent(currentDocument), docPaste);

            Object[] params = { newDocs.size() };
            facesMessages.add(StatusMessage.Severity.INFO, "#0 " + messages.get("n_pasted_docs"), params);

            EventManager.raiseEventsOnDocumentSelected(currentDocument);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED, currentDocument);

            log.debug("Elements pasted and created into the backend...");
        } else {
            log.debug("No docPaste to process paste on...");
        }

        return null;
    }

    @Override
    public String pasteDocumentListInside(List<DocumentModel> docPaste, String docId) {
        DocumentModel targetDoc = documentManager.getDocument(new IdRef(docId));
        if (null != docPaste) {
            List<DocumentModel> newDocs = recreateDocumentsWithNewParent(targetDoc, docPaste);

            Object[] params = { newDocs.size() };
            facesMessages.add(StatusMessage.Severity.INFO, "#0 " + messages.get("n_pasted_docs"), params);

            EventManager.raiseEventsOnDocumentSelected(targetDoc);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED, targetDoc);

            log.debug("Elements pasted and created into the backend...");
        } else {
            log.debug("No docPaste to process paste on...");
        }

        return null;
    }

    public List<DocumentModel> moveDocumentsToNewParent(DocumentModel destFolder, List<DocumentModel> docs)
            {
        DocumentRef destFolderRef = destFolder.getRef();
        boolean destinationIsDeleted = LifeCycleConstants.DELETED_STATE.equals(destFolder.getCurrentLifeCycleState());
        List<DocumentModel> newDocs = new ArrayList<DocumentModel>();
        StringBuilder sb = new StringBuilder();
        for (DocumentModel docModel : docs) {
            DocumentRef sourceFolderRef = docModel.getParentRef();

            String sourceType = docModel.getType();
            boolean canRemoveDoc = documentManager.hasPermission(sourceFolderRef, SecurityConstants.REMOVE_CHILDREN);
            boolean canPasteInCurrentFolder = typeManager.isAllowedSubType(sourceType, destFolder.getType(),
                    navigationContext.getCurrentDocument());
            boolean sameFolder = sourceFolderRef.equals(destFolderRef);
            if (canRemoveDoc && canPasteInCurrentFolder && !sameFolder) {
                if (destinationIsDeleted) {
                    if (checkDeletedState(docModel)) {
                        DocumentModel newDoc = documentManager.move(docModel.getRef(), destFolderRef, null);
                        setDeleteState(newDoc);
                        newDocs.add(newDoc);
                    } else {
                        addWarnMessage(sb, docModel);
                    }
                } else {
                    DocumentModel newDoc = documentManager.move(docModel.getRef(), destFolderRef, null);
                    newDocs.add(newDoc);
                }
            }
        }
        documentManager.save();

        if (sb.length() > 0) {
            facesMessages.add(StatusMessage.Severity.WARN, sb.toString());
        }
        return newDocs;
    }

    public String moveDocumentList(String listName, String docId) {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(listName);
        DocumentModel targetDoc = documentManager.getDocument(new IdRef(docId));
        // Get all parent folders
        Set<DocumentRef> parentRefs = new HashSet<DocumentRef>();
        for (DocumentModel doc : docs) {
            parentRefs.add(doc.getParentRef());
        }

        List<DocumentModel> newDocs = moveDocumentsToNewParent(targetDoc, docs);

        documentsListsManager.resetWorkingList(listName);

        Object[] params = { newDocs.size() };
        facesMessages.add(StatusMessage.Severity.INFO, "#0 " + messages.get("n_moved_docs"), params);

        EventManager.raiseEventsOnDocumentSelected(targetDoc);
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED, targetDoc);

        // Send event to all initial parents
        for (DocumentRef docRef : parentRefs) {
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED, documentManager.getDocument(docRef));
        }

        log.debug("Elements moved and created into the backend...");

        return null;
    }

    public String moveDocumentList(String listName) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return moveDocumentList(listName, currentDocument.getId());
    }

    @Override
    public String moveWorkingList() {
        try {
            moveDocumentList(getCurrentSelectedListName());
        } catch (NuxeoException e) {
            log.error("moveWorkingList failed" + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.WARN, messages.get("invalid_operation"));
        }
        return null;
    }

    @Override
    public String pasteWorkingList() {
        try {
            pasteDocumentList(getCurrentSelectedList());
        } catch (NuxeoException e) {
            log.error("pasteWorkingList failed" + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.WARN, messages.get("invalid_operation"));
        }
        return null;
    }

    @Override
    public String pasteClipboard() {
        try {
            pasteDocumentList(DocumentsListsManager.CLIPBOARD);
            returnToPreviouslySelectedList();
        } catch (NuxeoException e) {
            log.error("pasteClipboard failed" + e.getMessage(), e);
            facesMessages.add(StatusMessage.Severity.WARN, messages.get("invalid_operation"));

        }
        return null;
    }

    @Override
    @WebRemote
    public String pasteClipboardInside(String docId) {
        pasteDocumentListInside(DocumentsListsManager.CLIPBOARD, docId);
        return null;
    }

    @Override
    @WebRemote
    public String moveClipboardInside(String docId) {
        moveDocumentList(DocumentsListsManager.CLIPBOARD, docId);
        return null;
    }

    /**
     * Creates the documents in the backend under the target parent.
     */
    protected List<DocumentModel> recreateDocumentsWithNewParent(DocumentModel parent, List<DocumentModel> documents)
            {

        List<DocumentModel> newDocuments = new ArrayList<DocumentModel>();

        if (null == parent || null == documents) {
            log.error("Null params received, returning...");
            return newDocuments;
        }

        List<DocumentModel> documentsToPast = new LinkedList<DocumentModel>();

        // filter list on content type
        for (DocumentModel doc : documents) {
            if (typeManager.isAllowedSubType(doc.getType(), parent.getType(), navigationContext.getCurrentDocument())) {
                documentsToPast.add(doc);
            }
        }

        // copying proxy or document
        boolean isPublishSpace = isPublishSpace(parent);
        boolean destinationIsDeleted = LifeCycleConstants.DELETED_STATE.equals(parent.getCurrentLifeCycleState());
        List<DocumentRef> docRefs = new ArrayList<DocumentRef>();
        List<DocumentRef> proxyRefs = new ArrayList<DocumentRef>();
        StringBuilder sb = new StringBuilder();
        for (DocumentModel doc : documentsToPast) {
            if (destinationIsDeleted && !checkDeletedState(doc)) {
                addWarnMessage(sb, doc);
            } else if (doc.isProxy() && !isPublishSpace) {
                // in a non-publish space, we want to expand proxies into
                // normal docs
                proxyRefs.add(doc.getRef());
            } else {
                // copy as is
                docRefs.add(doc.getRef());
            }
        }
        if (!proxyRefs.isEmpty()) {
            newDocuments.addAll(documentManager.copyProxyAsDocument(proxyRefs, parent.getRef(), true));
        }
        if (!docRefs.isEmpty()) {
            newDocuments.addAll(documentManager.copy(docRefs, parent.getRef(), true));
        }
        if (destinationIsDeleted) {
            for (DocumentModel d : newDocuments) {
                setDeleteState(d);
            }
        }
        documentManager.save();
        if (sb.length() > 0) {
            facesMessages.add(StatusMessage.Severity.WARN, sb.toString());
        }
        return newDocuments;
    }

    protected boolean checkDeletedState(DocumentModel doc) {
        if (LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
            return true;
        }
        if (doc.getAllowedStateTransitions().contains(LifeCycleConstants.DELETE_TRANSITION)) {
            return true;
        }
        return false;
    }

    protected void setDeleteState(DocumentModel doc) {
        if (doc.getAllowedStateTransitions().contains(LifeCycleConstants.DELETE_TRANSITION)) {
            doc.followTransition(LifeCycleConstants.DELETE_TRANSITION);
        }
    }

    protected void addWarnMessage(StringBuilder sb, DocumentModel doc) {
        if (sb.length() == 0) {
            sb.append(messages.get("document_no_deleted_state"));
            sb.append("'").append(doc.getTitle()).append("'");
        } else {
            sb.append(", '").append(doc.getTitle()).append("'");
        }
    }

    /**
     * Check if the container is a publish space. If this is not the case, a proxy copied to it will be recreated as a
     * new document.
     */
    protected boolean isPublishSpace(DocumentModel container) {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Set<String> publishSpaces = schemaManager.getDocumentTypeNamesForFacet(FacetNames.PUBLISH_SPACE);
        if (publishSpaces == null || publishSpaces.isEmpty()) {
            publishSpaces = new HashSet<String>();
        }
        return publishSpaces.contains(container.getType());
    }

    /**
     * Gets the parent document under the paste should be performed.
     * <p>
     * Rules:
     * <p>
     * In general the currentDocument is the parent. Exceptions to this rule: when the currentDocument is a domain or
     * null. If Domain then content root is the parent. If null is passed, then the JCR root is taken as parent.
     */
    protected DocumentModel getParent(DocumentModel currentDocument) {

        if (currentDocument.isFolder()) {
            return currentDocument;
        }

        DocumentModelList parents = navigationContext.getCurrentPath();
        for (int i = parents.size() - 1; i >= 0; i--) {
            DocumentModel parent = parents.get(i);
            if (parent.isFolder()) {
                return parent;
            }
        }

        return null;
    }

    @Override
    @Factory(value = "isCurrentWorkListEmpty", scope = EVENT)
    public boolean factoryForIsCurrentWorkListEmpty() {
        return isWorkListEmpty();
    }

    @Override
    public boolean isWorkListEmpty() {
        return documentsListsManager.isWorkingListEmpty(getCurrentSelectedListName());
    }

    @Override
    public String exportWorklistAsZip() {
        return exportWorklistAsZip(documentsListsManager.getWorkingList(getCurrentSelectedListName()));
    }

    @Override
    public String exportAllBlobsFromWorkingListAsZip() {
        return exportWorklistAsZip();
    }

    @Override
    public String exportMainBlobFromWorkingListAsZip() {
        return exportWorklistAsZip();
    }

    @Override
    public String exportWorklistAsZip(List<DocumentModel> documents) {
        return exportWorklistAsZip(documents, true);
    }

    public String exportWorklistAsZip(DocumentModel document) {
        return exportWorklistAsZip(Arrays.asList(new DocumentModel[] { document }), true);
    }

    /**
     * Checks if copy action is available in the context of the current Document.
     * <p>
     * Condition: the list of selected documents is not empty.
     */
    @Override
    public boolean getCanCopy() {
        if (navigationContext.getCurrentDocument() == null) {
            return false;
        }
        return !documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
    }

    /**
     * Checks if the Paste action is available in the context of the current Document. Conditions:
     * <p>
     * <ul>
     * <li>list is not empty
     * <li>user has the needed permissions on the current document
     * <li>the content of the list can be added as children of the current document
     * </ul>
     */
    @Override
    public boolean getCanPaste(String listName) {

        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        if (documentsListsManager.isWorkingListEmpty(listName) || currentDocument == null) {
            return false;
        }

        DocumentModel pasteTarget = getParent(navigationContext.getCurrentDocument());
        if (pasteTarget == null) {
            // parent may be unreachable (right inheritance blocked)
            return false;
        }
        if (!documentManager.hasPermission(pasteTarget.getRef(), SecurityConstants.ADD_CHILDREN)) {
            return false;
        } else {
            // filter on allowed content types
            // see if at least one doc can be pasted
            // String pasteTypeName = clipboard.getClipboardDocumentType();
            List<String> pasteTypesName = documentsListsManager.getWorkingListTypes(listName);
            for (String pasteTypeName : pasteTypesName) {
                if (typeManager.isAllowedSubType(pasteTypeName, pasteTarget.getType(),
                        navigationContext.getCurrentDocument())) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public boolean getCanPasteInside(String listName, DocumentModel document) {
        if (documentsListsManager.isWorkingListEmpty(listName) || document == null) {
            return false;
        }

        if (!documentManager.hasPermission(document.getRef(), SecurityConstants.ADD_CHILDREN)) {
            return false;
        } else {
            // filter on allowed content types
            // see if at least one doc can be pasted
            // String pasteTypeName = clipboard.getClipboardDocumentType();
            List<String> pasteTypesName = documentsListsManager.getWorkingListTypes(listName);
            for (String pasteTypeName : pasteTypesName) {
                if (typeManager.isAllowedSubType(pasteTypeName, document.getType(),
                        navigationContext.getCurrentDocument())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Checks if the Move action is available in the context of the document document. Conditions:
     * <p>
     * <ul>
     * <li>list is not empty
     * <li>user has the needed permissions on the document
     * <li>an element in the list can be removed from its folder and added as child of the current document
     * </ul>
     */
    @Override
    public boolean getCanMoveInside(String listName, DocumentModel document) {
        if (documentsListsManager.isWorkingListEmpty(listName) || document == null) {
            return false;
        }
        DocumentRef destFolderRef = document.getRef();
        DocumentModel destFolder = document;
        if (!documentManager.hasPermission(destFolderRef, SecurityConstants.ADD_CHILDREN)) {
            return false;
        } else {
            // filter on allowed content types
            // see if at least one doc can be removed and pasted
            for (DocumentModel docModel : documentsListsManager.getWorkingList(listName)) {
                // skip deleted documents
                if (!exists(docModel.getRef())) {
                    continue;
                }
                DocumentRef sourceFolderRef = docModel.getParentRef();
                String sourceType = docModel.getType();
                boolean canRemoveDoc = documentManager.hasPermission(sourceFolderRef,
                        SecurityConstants.REMOVE_CHILDREN);
                boolean canPasteInCurrentFolder = typeManager.isAllowedSubType(sourceType, destFolder.getType(),
                        navigationContext.getCurrentDocument());
                boolean sameFolder = sourceFolderRef.equals(destFolderRef);
                if (canRemoveDoc && canPasteInCurrentFolder && !sameFolder) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Checks if the Move action is available in the context of the current Document. Conditions:
     * <p>
     * <ul>
     * <li>list is not empty
     * <li>user has the needed permissions on the current document
     * <li>an element in the list can be removed from its folder and added as child of the current document
     * </ul>
     */
    public boolean getCanMove(String listName) {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return getCanMoveInside(listName, currentDocument);
    }

    @Override
    public boolean getCanPasteWorkList() {
        return getCanPaste(getCurrentSelectedListName());
    }

    @Override
    public boolean getCanMoveWorkingList() {
        return getCanMove(getCurrentSelectedListName());
    }

    @Override
    public boolean getCanPasteFromClipboard() {
        return getCanPaste(DocumentsListsManager.CLIPBOARD);
    }

    @Override
    public boolean getCanPasteFromClipboardInside(DocumentModel document) {
        return getCanPasteInside(DocumentsListsManager.CLIPBOARD, document);
    }

    @Override
    public boolean getCanMoveFromClipboardInside(DocumentModel document) {
        return getCanMoveInside(DocumentsListsManager.CLIPBOARD, document);
    }

    @Override
    public void setCurrentSelectedList(String listId) {
        if (listId != null && !listId.equals(currentSelectedList)) {
            currentSelectedList = listId;
            canEditSelectedDocs = null;
        }
    }

    @RequestParameter()
    String listIdToSelect;

    @Override
    public void selectList() {
        if (listIdToSelect != null) {
            setCurrentSelectedList(listIdToSelect);
        }
    }

    @Override
    public List<DocumentModel> getCurrentSelectedList() {
        return documentsListsManager.getWorkingList(getCurrentSelectedListName());
    }

    @Override
    public String getCurrentSelectedListName() {
        if (currentSelectedList == null) {
            if (!getAvailableLists().isEmpty()) {
                setCurrentSelectedList(availableLists.get(0));
            }
        }
        return currentSelectedList;
    }

    @Override
    public String getCurrentSelectedListTitle() {
        String title = null;
        String listName = getCurrentSelectedListName();
        if (listName != null) {
            DocumentsListDescriptor desc = documentsListsManager.getWorkingListDescriptor(listName);
            if (desc != null) {
                title = desc.getTitle();
            }
        }
        return title;
    }

    @Override
    public List<String> getAvailableLists() {
        if (availableLists == null) {
            availableLists = documentsListsManager.getWorkingListNamesForCategory("CLIPBOARD");
        }
        return availableLists;
    }

    @Override
    public List<DocumentsListDescriptor> getDescriptorsForAvailableLists() {
        if (descriptorsForAvailableLists == null) {
            List<String> availableLists = getAvailableLists();
            descriptorsForAvailableLists = new ArrayList<DocumentsListDescriptor>();
            for (String lName : availableLists) {
                descriptorsForAvailableLists.add(documentsListsManager.getWorkingListDescriptor(lName));
            }
        }
        return descriptorsForAvailableLists;
    }

    @Override
    public List<Action> getActionsForCurrentList() {
        String lstName = getCurrentSelectedListName();
        if (isWorkListEmpty()) {
            // we use cache here since this is a very common case ...
            if (actionCache == null) {
                actionCache = new HashMap<String, List<Action>>();
            }
            if (!actionCache.containsKey(lstName)) {
                actionCache.put(lstName, webActions.getActionsList(lstName + "_LIST"));
            }
            return actionCache.get(lstName);
        } else {
            return webActions.getActionsList(lstName + "_LIST");
        }
    }

    @Override
    public List<Action> getActionsForSelection() {
        return webActions.getActionsList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION + "_LIST", false);
    }

    private void autoSelectCurrentList(String listName) {
        previouslySelectedList = getCurrentSelectedListName();
        setCurrentSelectedList(listName);
    }

    private void returnToPreviouslySelectedList() {
        setCurrentSelectedList(previouslySelectedList);
    }

    @Override
    public boolean getCanEditSelectedDocs() {
        if (canEditSelectedDocs == null) {
            if (getCurrentSelectedList().isEmpty()) {
                canEditSelectedDocs = false;
            } else {
                final List<DocumentModel> selectedDocs = getCurrentSelectedList();

                // check selected docs
                canEditSelectedDocs = checkWritePerm(selectedDocs);
            }
        }
        return canEditSelectedDocs;
    }

    @Override
    @Deprecated
    // no longer used by the user_clipboard.xhtml template
    public boolean getCanEditListDocs(String listName) {
        final List<DocumentModel> docs = documentsListsManager.getWorkingList(listName);

        final boolean canEdit;
        if (docs.isEmpty()) {
            canEdit = false;
        } else {
            // check selected docs
            canEdit = checkWritePerm(docs);
        }
        return canEdit;
    }

    private boolean checkWritePerm(List<DocumentModel> selectedDocs) {
        for (DocumentModel documentModel : selectedDocs) {
            boolean canWrite = documentManager.hasPermission(documentModel.getRef(),
                    SecurityConstants.WRITE_PROPERTIES);
            if (!canWrite) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isCacheEnabled() {
        if (!SeamCacheHelper.canUseSeamCache()) {
            return false;
        }
        return isWorkListEmpty();
    }

    @Override
    public String getCacheKey() {
        return getCurrentSelectedListName() + "::" + localeSelector.getLocaleString();
    }

    @Override
    public boolean isCacheEnabledForSelection() {
        if (!SeamCacheHelper.canUseSeamCache()) {
            return false;
        }
        return documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
    }

    @Override
    public String exportWorklistAsZip(List<DocumentModel> documents, boolean exportAllBlobs) {
        Blob blob = null;
        try {
            DownloadService downloadService = Framework.getService(DownloadService.class);
            DocumentListZipExporter zipExporter = new DocumentListZipExporter();
            blob = zipExporter.exportWorklistAsZip(documents, documentManager, exportAllBlobs);
            if (blob == null) {
                // empty zip file, do nothing
                facesMessages.add(StatusMessage.Severity.INFO, messages.get("label.clipboard.emptyDocuments"));
                return null;
            }
            blob.setMimeType("application/zip");
            blob.setFilename("clipboard.zip");

            String key = downloadService.storeBlobs(Collections.singletonList(blob));
            String url = BaseURL.getBaseURL() + downloadService.getDownloadUrl(key);
            ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
            context.redirect(url);
            return "";
        } catch (IOException io) {
            if (blob != null) {
                blob.getFile().delete();
            }
            throw new NuxeoException("Error while redirecting for clipboard content", io);
        }
    }
}
