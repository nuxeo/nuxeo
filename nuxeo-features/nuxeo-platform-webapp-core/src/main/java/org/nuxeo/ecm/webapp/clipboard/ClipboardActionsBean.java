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

import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.ScopeType.SESSION;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import javax.annotation.security.PermitAll;
import javax.ejb.Remove;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListDescriptor;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * This is the action listener behind the copy/paste template that knows how to
 * copy/paste the selected user data to the target action listener, and also
 * create/remove the corresponding objects into the backend.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Name("clipboardActions")
@Scope(SESSION)
public class ClipboardActionsBean extends InputController implements
        ClipboardActions, Serializable {

    private static final long serialVersionUID = -2407222456116573225L;

    private static final Log log = LogFactory.getLog(ClipboardActionsBean.class);

    private static final int BUFFER = 2048;

    private static final String SUMMARY_FILENAME = "INDEX.txt";
    private static final String SUMMARY_HEADER = ".";
    private static final String PASTE_OUTCOME = "after_paste";
    private static final String MOVE_OUTCOME = "after_move";
    public static final String DELETED_LIFECYCLE_STATE = "deleted";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

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

    // @Observer({EventNames.DOCUMENT_SELECTION_CHANGED})
    public void releaseClipboardableDocuments() {
    }

    public boolean isInitialized() {
        return documentManager != null;
    }

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

    public void putSelectionInWorkList() throws ClientException {
        putSelectionInWorkList(false);
    }

    public void putSelectionInDefaultWorkList() {
        canEditSelectedDocs = null;
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            List<DocumentModel> docsList = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
            Object[] params = { docsList.size() };
            facesMessages.add(FacesMessage.SEVERITY_INFO, "#0 "
                    + resourcesAccessor.getMessages().get("n_copied_docs"),
                    params);
            documentsListsManager.addToWorkingList(
                    DocumentsListsManager.DEFAULT_WORKING_LIST, docsList);

            // auto select clipboard
            autoSelectCurrentList(DocumentsListsManager.DEFAULT_WORKING_LIST);

        } else {
            log.debug("No selectable Documents in context to process copy on...");
        }
        log.debug("add to worklist processed...");
    }

    @WebRemote
    public void putInClipboard(String docId) throws ClientException {
        DocumentModel doc = documentManager.getDocument(new IdRef(docId));
        documentsListsManager.addToWorkingList(DocumentsListsManager.CLIPBOARD,
                doc);
        Object[] params = { 1 };
        FacesMessage message = FacesMessages.createFacesMessage(
                FacesMessage.SEVERITY_INFO, "#0 "
                        + resourcesAccessor.getMessages().get("n_copied_docs"),
                params);
        facesMessages.add(message);
        autoSelectCurrentList(DocumentsListsManager.CLIPBOARD);
    }

    public void putSelectionInClipboard() {
        canEditSelectedDocs = null;
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            List<DocumentModel> docsList = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
            Object[] params = { docsList.size() };

            FacesMessage message = FacesMessages.createFacesMessage(
                    FacesMessage.SEVERITY_INFO, "#0 "
                            + resourcesAccessor.getMessages().get(
                                    "n_copied_docs"), params);

            facesMessages.add(message);

            documentsListsManager.addToWorkingList(
                    DocumentsListsManager.CLIPBOARD, docsList);

            // auto select clipboard
            autoSelectCurrentList(DocumentsListsManager.CLIPBOARD);

        } else {
            log.debug("No selectable Documents in context to process copy on...");
        }
        log.debug("add to worklist processed...");
    }

    public void putSelectionInWorkList(List<DocumentModel> docsList) {
        putSelectionInWorkList(docsList, false);
    }

    public void putSelectionInWorkList(List<DocumentModel> docsList,
            Boolean forceAppend) {
        canEditSelectedDocs = null;
        if (null != docsList) {
            Object[] params = { docsList.size() };
            facesMessages.add(FacesMessage.SEVERITY_INFO, "#0 "
                    + resourcesAccessor.getMessages().get(
                            "n_added_to_worklist_docs"), params);

            // Add to the default working list
            documentsListsManager.addToWorkingList(
                    getCurrentSelectedListName(), docsList, forceAppend);
            log.debug("Elements copied to clipboard...");

            // go back to previously selected list
            // returnToPreviouslySelectedList();

        } else {
            log.debug("No copiedDocs to process copy on...");
        }

        log.debug("add to worklist processed...");
    }

    @Deprecated
    public void copySelection(List<DocumentModel> copiedDocs) {
        if (null != copiedDocs) {
            Object[] params = { copiedDocs.size() };
            facesMessages.add(FacesMessage.SEVERITY_INFO, "#0 "
                    + resourcesAccessor.getMessages().get("n_copied_docs"),
                    params);

            // clipboard.copy(copiedDocs);

            // Reset + Add to clipboard list
            documentsListsManager.resetWorkingList(DocumentsListsManager.CLIPBOARD);
            documentsListsManager.addToWorkingList(
                    DocumentsListsManager.CLIPBOARD, copiedDocs);

            // Add to the default working list
            documentsListsManager.addToWorkingList(copiedDocs);
            log.debug("Elements copied to clipboard...");

        } else {
            log.debug("No copiedDocs to process copy on...");
        }

        log.debug("Copy processed...");
    }

    public String removeWorkListItem(DocumentRef ref) throws ClientException {
        DocumentModel doc = documentManager.getDocument(ref);
        documentsListsManager.removeFromWorkingList(
                getCurrentSelectedListName(), doc);
        return null;
    }

    public String clearWorkingList() {
        documentsListsManager.resetWorkingList(getCurrentSelectedListName());
        return null;
    }

    public String pasteDocumentList(String listName) throws ClientException {
        return pasteDocumentList(documentsListsManager.getWorkingList(listName));
    }

    public String pasteDocumentListInside(String listName, String docId)
            throws ClientException {
        return pasteDocumentListInside(
                documentsListsManager.getWorkingList(listName), docId);
    }

    public String pasteDocumentList(List<DocumentModel> docPaste)
            throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (null != docPaste) {
            List<DocumentModel> newDocs = recreateDocumentsWithNewParent(
                    getParent(currentDocument), docPaste);

            Object[] params = { newDocs.size() };
            facesMessages.add(FacesMessage.SEVERITY_INFO, "#0 "
                    + resourcesAccessor.getMessages().get("n_pasted_docs"),
                    params);

            eventManager.raiseEventsOnDocumentSelected(currentDocument);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    currentDocument);

            log.debug("Elements pasted and created into the backend...");
        } else {
            log.debug("No docPaste to process paste on...");
        }

        return computeOutcome(PASTE_OUTCOME);
    }

    public String pasteDocumentListInside(List<DocumentModel> docPaste,
            String docId) throws ClientException {
        DocumentModel targetDoc = documentManager.getDocument(new IdRef(docId));
        if (null != docPaste) {
            List<DocumentModel> newDocs = recreateDocumentsWithNewParent(
                    targetDoc, docPaste);

            Object[] params = { newDocs.size() };
            facesMessages.add(FacesMessage.SEVERITY_INFO, "#0 "
                    + resourcesAccessor.getMessages().get("n_pasted_docs"),
                    params);

            eventManager.raiseEventsOnDocumentSelected(targetDoc);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    targetDoc);

            log.debug("Elements pasted and created into the backend...");
        } else {
            log.debug("No docPaste to process paste on...");
        }

        return computeOutcome(PASTE_OUTCOME);
    }

    public List<DocumentModel> moveDocumentsToNewParent(
            DocumentModel destFolder, List<DocumentModel> docs)
            throws ClientException {
        Collection<Type> allowed = typeManager.getAllowedSubTypes(destFolder.getType());
        Collection<String> allowedList = new ArrayList<String>();
        for (Type allowedType : allowed) {
            allowedList.add(allowedType.getId());
        }

        DocumentRef destFolderRef = destFolder.getRef();
        List<DocumentModel> newDocs = new ArrayList<DocumentModel>();
        for (DocumentModel docModel : docs) {
            DocumentRef sourceFolderRef = docModel.getParentRef();
            String sourceType = docModel.getType();
            boolean canRemoveDoc = documentManager.hasPermission(
                    sourceFolderRef, SecurityConstants.REMOVE_CHILDREN);
            boolean canPasteInCurrentFolder = allowedList.contains(sourceType);
            boolean sameFolder = sourceFolderRef.equals(destFolderRef);
            if (canRemoveDoc && canPasteInCurrentFolder && !sameFolder) {
                DocumentModel newDoc = documentManager.move(docModel.getRef(),
                        destFolderRef, null);
                newDocs.add(newDoc);
            }
        }
        documentManager.save();

        return newDocs;
    }

    public String moveDocumentList(String listName) throws ClientException {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(listName);

        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (null != docs) {
            List<DocumentModel> newDocs = moveDocumentsToNewParent(
                    currentDocument, docs);

            documentsListsManager.getWorkingList(listName).clear();

            Object[] params = { newDocs.size() };
            facesMessages.add(FacesMessage.SEVERITY_INFO, "#0 "
                    + resourcesAccessor.getMessages().get("n_moved_docs"),
                    params);

            eventManager.raiseEventsOnDocumentSelected(currentDocument);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    currentDocument);

            log.debug("Elements moved and created into the backend...");
        } else {
            log.debug("No documents to process move on...");
        }

        return computeOutcome(PASTE_OUTCOME);
    }

    public String moveWorkingList() throws ClientException {
        moveDocumentList(getCurrentSelectedListName());
        return computeOutcome(MOVE_OUTCOME);
    }

    public String pasteWorkingList() throws ClientException {
        pasteDocumentList(getCurrentSelectedList());
        return computeOutcome(PASTE_OUTCOME);
    }

    public String pasteClipboard() throws ClientException {
        pasteDocumentList(DocumentsListsManager.CLIPBOARD);
        returnToPreviouslySelectedList();
        return computeOutcome(PASTE_OUTCOME);
    }

    @WebRemote
    public String pasteClipboardInside(String docId) throws ClientException {
        pasteDocumentListInside(DocumentsListsManager.CLIPBOARD, docId);
        return computeOutcome(PASTE_OUTCOME);
    }

    /**
     * Creates the documents in the backend under the target parent.
     */
    protected List<DocumentModel> recreateDocumentsWithNewParent(
            DocumentModel parent, List<DocumentModel> documents)
            throws ClientException {

        List<DocumentModel> newDocuments = new ArrayList<DocumentModel>();

        if (null == parent || null == documents) {
            log.error("Null params received, returning...");
            return newDocuments;
        }

        List<DocumentModel> documentsToPast = new LinkedList<DocumentModel>();

        // filter list on content type
        Collection<Type> allowedTypes = typeManager.getAllowedSubTypes(parent.getType());
        List<String> allowedTypesNames = new LinkedList<String>();
        for (Type tip : allowedTypes) {
            allowedTypesNames.add(tip.getId());
        }
        for (DocumentModel doc : documents) {
            if (allowedTypesNames.contains(doc.getType())) {
                documentsToPast.add(doc);
            }
        }

        // copying proxy or document
        boolean isPublishSpace = isPublishSpace(parent);
        List<DocumentRef> docRefs = new ArrayList<DocumentRef>();
        List<DocumentRef> proxyRefs = new ArrayList<DocumentRef>();
        for (DocumentModel doc : documentsToPast) {
            if (doc.isProxy() && !isPublishSpace) {
                // in a non-publish space, we want to expand proxies into normal
                // docs
                proxyRefs.add(doc.getRef());
            } else {
                // copy as is
                docRefs.add(doc.getRef());
            }
        }
        if (!proxyRefs.isEmpty()) {
            newDocuments.addAll(documentManager.copyProxyAsDocument(proxyRefs,
                    parent.getRef()));
        }
        if (!docRefs.isEmpty()) {
            newDocuments.addAll(documentManager.copy(docRefs, parent.getRef()));
        }
        documentManager.save();

        return newDocuments;
    }

    /**
     * Check if the container is a publish space. If this is not the case, a
     * proxy copied to it will be recreated as a new document.
     */
    protected boolean isPublishSpace(DocumentModel container)
            throws ClientException {
        SchemaManager schemaManager;
        try {
            schemaManager = Framework.getService(SchemaManager.class);
        } catch (Exception e) {
            throw new ClientException(e);
        }
        Set<String> publishSpaces = null;
        if (schemaManager != null) {
            publishSpaces = schemaManager.getDocumentTypeNamesForFacet("PublishSpace");
        }
        if (publishSpaces == null || publishSpaces.isEmpty()) {
            publishSpaces = new HashSet<String>();
            publishSpaces.add("Section");
        }
        return publishSpaces.contains(container.getType());
    }

    @Destroy
    @Remove
    @PermitAll
    public void destroy() {
        log.debug("Removing Seam component: clipboardActions");
    }

    /**
     * Gets the parent document under the paste should be performed.
     * <p>
     * Rules:
     * <p>
     * In general the currentDocument is the parent. Exceptions to this rule:
     * when the currentDocument is a domain or null. If Domain then content root
     * is the parent. If null is passed, then the JCR root is taken as parent.
     */
    protected DocumentModel getParent(DocumentModel currentDocument)
            throws ClientException {

        if (currentDocument.isFolder()) {
            return currentDocument;
        }

        DocumentModel parent = null;
        DocumentModelList parents = navigationContext.getCurrentPath();
        for (int i = parents.size() - 1; i >= 0; i--) {
            parent = parents.get(i);
            if (parent.isFolder()) {
                return parent;
            }
        }

        return null;
    }

    @Factory(value = "isCurrentWorkListEmpty", scope = EVENT)
    public boolean factoryForIsCurrentWorkListEmpty() {
        return isWorkListEmpty();
    }

    public boolean isWorkListEmpty() {
        return documentsListsManager.isWorkingListEmpty(getCurrentSelectedListName());
    }

    public String exportWorklistAsZip() throws ClientException {
        return exportWorklistAsZip(documentsListsManager.getWorkingList(getCurrentSelectedListName()));
    }

    public String exportWorklistAsZip(List<DocumentModel> documents)
            throws ClientException {
        try {
            SummaryImpl summary = new SummaryImpl();

            SummaryEntry summaryRoot = new SummaryEntry("", SUMMARY_HEADER,
                    new Date(), "", "", null);
            summaryRoot.setDocumentRef(new IdRef("0"));
            summary.put(new IdRef("0").toString(), summaryRoot);

            FacesContext context = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

            BufferedOutputStream buff = new BufferedOutputStream(
                    response.getOutputStream());
            ZipOutputStream out = new ZipOutputStream(buff);
            out.setMethod(ZipOutputStream.DEFLATED);
            out.setLevel(9);
            byte[] data = new byte[BUFFER];
            for (DocumentModel doc : documents) {

                // first check if DM is attached to the core
                if (doc.getSessionId() == null) {
                    // refetch the doc from the core
                    doc = documentManager.getDocument(doc.getRef());
                }

                // NXP-2334 : skip deleted docs
                if (doc.getCurrentLifeCycleState().equals(DELETED_LIFECYCLE_STATE)) {
                    continue;
                }

                if (doc.isFolder() && !isEmptyFolder(doc, documentManager)) {

                    SummaryEntry summaryLeaf = new SummaryEntry(doc);
                    summaryLeaf.setParent(summaryRoot);
                    // Quick Fix to avoid adding the logo in summary
                    if (doc.getType().equals("Workspace")
                            || doc.getType().equals("WorkspaceRoot")) {
                        summaryLeaf.setFilename("");
                    }
                    summary.put(summaryLeaf.getPath(), summaryLeaf);

                    addFolderToZip("", out, doc, data, documentManager,
                            summary.get(summaryLeaf.getPath()), summary);
                } else if (doc.getType().equals("Note")) {
                    addNoteToZip("", out, doc, data, summary.getSummaryRoot(),
                            summary);
                } else if (doc.hasSchema("file")) {
                    addFileToZip("", out, doc, data, summary.getSummaryRoot(),
                            summary);
                }
            }
            if (summary.size() > 1) {
                addSummaryToZip(out, data, summary);
            }
            try {
                out.close();
            } catch (ZipException e) {
                // empty zip file, do nothing
                setFacesMessage("label.clipboard.emptyDocuments");
                return null;
            }
            response.setHeader("Content-Disposition", "attachment; filename=\""
                    + "clipboard.zip" + "\";");
            response.setContentType("application/gzip");
            response.flushBuffer();
            context.responseComplete();
            return null;
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    /**
     * Checks if Copy action is available in the context of the current
     * Document.
     * <p>
     * Conditions:
     * <p> - the list of selected documents is not empty
     *
     */
    public boolean getCanCopy() {
        if (navigationContext.getCurrentDocument() == null) {
            return false;
        }
        return !documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
    }

    /**
     * Checks if the Paste action is available in the context of the current
     * Document. Conditions:
     * <p>
     * <ul>
     * <li>list is not empty
     * <li>user has the needed permissions on the current document
     * <li>the content of the list can be added as children of the current
     * document
     * </ul>
     */
    public boolean getCanPaste(String listName) throws ClientException {

        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        if (documentsListsManager.isWorkingListEmpty(listName)
                || currentDocument == null) {
            return false;
        }

        DocumentModel pasteTarget = getParent(navigationContext.getCurrentDocument());
        if (!documentManager.hasPermission(pasteTarget.getRef(),
                SecurityConstants.ADD_CHILDREN)) {
            return false;
        } else {
            // filter on allowed content types
            // see if at least one doc can be pasted
            // String pasteTypeName = clipboard.getClipboardDocumentType();
            List<String> pasteTypesName = documentsListsManager.getWorkingListTypes(listName);
            Collection<Type> allowed = typeManager.getAllowedSubTypes(pasteTarget.getType());
            for (Type allowedType : allowed) {
                if (pasteTypesName.contains(allowedType.getId())) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean getCanPasteInside(String listName, DocumentModel document)
            throws ClientException {
        if (documentsListsManager.isWorkingListEmpty(listName)
                || document == null) {
            return false;
        }

        if (!documentManager.hasPermission(document.getRef(),
                SecurityConstants.ADD_CHILDREN)) {
            return false;
        } else {
            // filter on allowed content types
            // see if at least one doc can be pasted
            // String pasteTypeName = clipboard.getClipboardDocumentType();
            List<String> pasteTypesName = documentsListsManager.getWorkingListTypes(listName);
            Collection<Type> allowed = typeManager.getAllowedSubTypes(document.getType());
            for (Type allowedType : allowed) {
                if (pasteTypesName.contains(allowedType.getId())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Checks if the Move action is available in the context of the current
     * Document. Conditions:
     * <p>
     * <ul>
     * <li>list is not empty
     * <li>user has the needed permissions on the current document
     * <li>an element in the list can be removed from its folder and added as
     * child of the current document
     * </ul>
     */
    public boolean getCanMove(String listName) throws ClientException {

        DocumentModel currentDocument = navigationContext.getCurrentDocument();

        if (documentsListsManager.isWorkingListEmpty(listName)
                || currentDocument == null) {
            return false;
        }

        DocumentRef destFolderRef = currentDocument.getRef();
        DocumentModel destFolder = currentDocument;
        if (!documentManager.hasPermission(destFolderRef,
                SecurityConstants.ADD_CHILDREN)) {
            return false;
        } else {
            // filter on allowed content types
            // see if at least one doc can be removed and pasted
            Collection<Type> allowed = typeManager.getAllowedSubTypes(destFolder.getType());
            Collection<String> allowedList = new ArrayList<String>();
            for (Type allowedType : allowed) {
                allowedList.add(allowedType.getId());
            }
            for (DocumentModel docModel : documentsListsManager.getWorkingList(listName)) {
                DocumentRef sourceFolderRef = docModel.getParentRef();
                String sourceType = docModel.getType();
                boolean canRemoveDoc = documentManager.hasPermission(
                        sourceFolderRef, SecurityConstants.REMOVE_CHILDREN);
                boolean canPasteInCurrentFolder = allowedList.contains(sourceType);
                boolean sameFolder = sourceFolderRef.equals(destFolderRef);
                if (canRemoveDoc && canPasteInCurrentFolder && !sameFolder) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean getCanPasteWorkList() throws ClientException {
        return getCanPaste(getCurrentSelectedListName());
    }

    public boolean getCanMoveWorkingList() throws ClientException {
        return getCanMove(getCurrentSelectedListName());
    }

    public boolean getCanPasteFromClipboard() throws ClientException {
        return getCanPaste(DocumentsListsManager.CLIPBOARD);
    }

    public boolean getCanPasteFromClipboardInside(DocumentModel document)
            throws ClientException {
        return getCanPasteInside(DocumentsListsManager.CLIPBOARD, document);
    }

    // Misc internal function for Ziping Clipboard
    private void addFolderToZip(String path, ZipOutputStream out,
            DocumentModel doc, byte[] data, CoreSession documentManager,
            SummaryEntry parent, SummaryImpl summary) throws ClientException,
            IOException {

        String title = (String) doc.getProperty("dublincore", "title");
        List<DocumentModel> docList = documentManager.getChildren(doc.getRef());
        for (DocumentModel docChild : docList) {

            // NXP-2334 : skip deleted docs
            if (docChild.getCurrentLifeCycleState().equals(DELETED_LIFECYCLE_STATE)) {
                continue;
            }

            if (docChild.isFolder()
                    && !isEmptyFolder(docChild, documentManager)) {

                SummaryEntry summaryLeaf = new SummaryEntry(docChild);
                if (doc.getType().equals("Workspace")
                        || doc.getType().equals("WorkspaceRoot")) {
                    summaryLeaf.setFilename("");
                }
                summaryLeaf.setParent(parent);
                summary.put(summaryLeaf.getPath(), summaryLeaf);

                addFolderToZip(path + title + "/", out, docChild, data,
                        documentManager, summary.get(summaryLeaf.getPath()),
                        summary);

            } else if (docChild.getType().equals("Note")) {
                addNoteToZip(path + title + "/", out, docChild, data,
                        summary.get(parent.getPath()), summary);

            } else if (docChild.hasSchema("file")) {
                addFileToZip(path + title + "/", out, docChild, data,
                        summary.get(parent.getPath()), summary);
            }
        }
    }

    private boolean isEmptyFolder(DocumentModel doc, CoreSession documentManager)
            throws ClientException {

        List<DocumentModel> docList = documentManager.getChildren(doc.getRef());
        for (DocumentModel docChild : docList) {
            if (docChild.isFolder()) {
                return isEmptyFolder(docChild, documentManager);
            } else if (docChild.getType().equals("Note")) {
                String content = (String) docChild.getProperty("note", "note");
                if (content != null && !"".equals(content)) {
                    return false;
                }
            } else if (docChild.hasSchema("file")) {
                Blob content = (Blob) docChild.getProperty("file", "content");
                if (content != null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Writes a summary file and puts it in the archive.
     */
    private void addSummaryToZip(ZipOutputStream out, byte[] data,
            SummaryImpl summary) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append(summary.toString());

        Blob content = new StringBlob(sb.toString());

        BufferedInputStream buffi = new BufferedInputStream(
                content.getStream(), BUFFER);

        ZipEntry entry = new ZipEntry(SUMMARY_FILENAME);
        out.putNextEntry(entry);
        int count = buffi.read(data, 0, BUFFER);

        while (count != -1) {
            out.write(data, 0, count);
            count = buffi.read(data, 0, BUFFER);
        }
        out.closeEntry();
        buffi.close();
    }

    private void addNoteToZip(String path, ZipOutputStream out,
            DocumentModel doc, byte[] data, SummaryEntry parent,
            SummaryImpl summary) throws IOException {

        String note;
        try {
            note = (String) doc.getProperty("note", "note");
        } catch (ClientException e1) {
            note = null;
        }
        String fileName;
        try {
            fileName = doc.getProperty("dublincore", "title") + ".html";
        } catch (ClientException e1) {
            fileName = null;
        }
        Blob content = new StringBlob(note);

        SummaryEntry summaryLeaf = new SummaryEntry(doc);
        summaryLeaf.setParent(parent);
        summary.put(summaryLeaf.getPath(), summaryLeaf);

        BufferedInputStream buffi = new BufferedInputStream(
                content.getStream(), BUFFER);

        // Workaround to deal with duplicate file names.
        int tryCount = 0;
        while (true) {
            try {
                ZipEntry entry;
                if (tryCount == 0) {
                    entry = new ZipEntry(path + fileName);
                } else {
                    entry = new ZipEntry(path + fileName + '(' + tryCount
                            + ')');
                }
                out.putNextEntry(entry);
                break;
            } catch (ZipException e) {
                tryCount++;
            }
        }
        int count = buffi.read(data, 0, BUFFER);

        while (count != -1) {
            out.write(data, 0, count);
            count = buffi.read(data, 0, BUFFER);
        }
        out.closeEntry();
        buffi.close();
    }

    private void addFileToZip(String path, ZipOutputStream out,
            DocumentModel doc, byte[] data, SummaryEntry parent,
            SummaryImpl summary) throws IOException, ClientException {

        String fileName = (String) doc.getProperty("file", "filename");
        Blob content = (Blob) doc.getProperty("file", "content");

        // TODO : Quickfix to avoid a workspace to send his Logo as Blob. Need
        // an EP to deal with that
        // WS also have the schema file
        // We have to ensure that there are not empty to be
        // added to the summary.
        if (doc.isFolder() && isEmptyFolder(doc, documentManager)) {
            content = null;
        }
        if (content != null) {

            SummaryEntry summaryLeaf = new SummaryEntry(doc);
            summaryLeaf.setParent(parent);
            summary.put(summaryLeaf.getPath(), summaryLeaf);

            BufferedInputStream buffi = new BufferedInputStream(
                    content.getStream(), BUFFER);

            // Workaround to deal with duplicate file names.
            int tryCount = 0;
            while (true) {
                try {
                    ZipEntry entry;
                    if (tryCount == 0) {
                        entry = new ZipEntry(path + fileName);
                    } else {
                        entry = new ZipEntry(path + fileName + '(' + tryCount
                                + ')');
                    }
                    out.putNextEntry(entry);
                    break;
                } catch (ZipException e) {
                    tryCount++;
                }
            }

            int count = buffi.read(data, 0, BUFFER);
            while (count != -1) {
                out.write(data, 0, count);
                count = buffi.read(data, 0, BUFFER);
            }
            out.closeEntry();
            buffi.close();
        }
    }

    public void setCurrentSelectedList(String listId) {
        if (listId != null && !listId.equals(currentSelectedList)) {
            currentSelectedList = listId;
            canEditSelectedDocs = null;
        }
    }

    @RequestParameter()
    String listIdToSelect;

    public void selectList() {
        if (listIdToSelect != null) {
            setCurrentSelectedList(listIdToSelect);
        }
    }

    public List<DocumentModel> getCurrentSelectedList() {
        return documentsListsManager.getWorkingList(getCurrentSelectedListName());
    }

    public String getCurrentSelectedListName() {
        if (currentSelectedList == null) {
            if (!getAvailableLists().isEmpty()) {
                setCurrentSelectedList(availableLists.get(0));
            }
        }
        return currentSelectedList;
    }

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

    public List<String> getAvailableLists() {
        if (availableLists == null) {
            availableLists = documentsListsManager.getWorkingListNamesForCategory("CLIPBOARD");
        }
        return availableLists;
    }

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

    public List<Action> getActionsForCurrentList() {
        String lstName = getCurrentSelectedListName();
        if (isWorkListEmpty()) {
            // we use cache here since this is a very common case ...
            if (actionCache == null) {
                actionCache = new HashMap<String, List<Action>>();
            }
            if (!actionCache.containsKey(lstName)) {
                actionCache.put(lstName, webActions.getActionsList(lstName
                        + "_LIST"));
            }
            return actionCache.get(lstName);
        } else {
            return webActions.getActionsList(lstName + "_LIST");
        }
    }

    public List<Action> getActionsForSelection() {
        return webActions.getUnfiltredActionsList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION
                + "_LIST");
    }

    private void autoSelectCurrentList(String listName) {
        previouslySelectedList = getCurrentSelectedListName();
        setCurrentSelectedList(listName);
    }

    private void returnToPreviouslySelectedList() {
        setCurrentSelectedList(previouslySelectedList);
    }

    public boolean getCanEditSelectedDocs() throws ClientException {
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

    @Deprecated
    // no longer used by the user_clipboard.xhtml template
    public boolean getCanEditListDocs(String listName) throws ClientException {
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

    private boolean checkWritePerm(List<DocumentModel> selectedDocs)
            throws ClientException {
        for (DocumentModel documentModel : selectedDocs) {
            boolean canWrite = documentManager.hasPermission(
                    documentModel.getRef(), SecurityConstants.WRITE_PROPERTIES);
            if (!canWrite) {
                return false;
            }
        }
        return true;
    }

    public boolean isCacheEnabled() {
        return isWorkListEmpty();
    }

    public String getCacheKey() {
        return getCurrentSelectedListName() + "::"
                + localeSelector.getLocaleString();
    }

    public boolean isCacheEnabledForSelection() {
        return documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
    }

}
