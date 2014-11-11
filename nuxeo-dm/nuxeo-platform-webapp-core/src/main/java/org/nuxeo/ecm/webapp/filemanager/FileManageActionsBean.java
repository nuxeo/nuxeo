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

package org.nuxeo.ecm.webapp.filemanager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.nuxeo.common.utils.Base64;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.UserAction;
import org.nuxeo.ecm.platform.ui.web.util.files.FileUtils;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.clipboard.ClipboardActions;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

/**
 * @author <a href="mailto:andreas.kalogeropoulos@nuxeo.com">Andreas
 *         Kalogeropoulos</a>
 */
@Name("FileManageActions")
@Scope(ScopeType.EVENT)
@Install(precedence = Install.FRAMEWORK)
public class FileManageActionsBean extends InputController implements
        FileManageActions {

    private static final Log log = LogFactory.getLog(FileManageActionsBean.class);

    public static final String TRANSF_ERROR = "TRANSF_ERROR";

    public static final String SECURITY_ERROR = "SECURITY_ERROR";

    public static final String MOVE_ERROR = "MOVE_ERROR";

    public static final String COPY_ERROR = "COPY_ERROR";

    public static final String PASTE_ERROR = "PASTE_ERROR";

    public static final String MOVE_IMPOSSIBLE = "MOVE_IMPOSSIBLE";

    public static final String MOVE_PUBLISH = "MOVE_PUBLISH";

    public static final String MOVE_OK = "MOVE_OK";

    @In(create = true, required = false)
    protected CoreSession documentManager;

    @In(create = true)
    protected TypeManager typeManager;

    @In(create = true)
    protected ClipboardActions clipboardActions;

    @In(create = true, required = false)
    protected UploadItemHolder fileUploadHolder;

    @In(create = true, required = false)
    protected UploadItemHolderCycleManager fileUploadHolderCycle;

    protected FileManager fileManager;

    protected FileManager getFileManagerService() throws ClientException {
        if (fileManager == null) {
            try {
                fileManager = Framework.getService(FileManager.class);
            } catch (Exception e) {
                log.error("Unable to get FileManager service ", e);
                throw new ClientException("Unable to get FileManager service ",
                        e);
            }
        }
        return fileManager;
    }

    public void destroy() {
        log.debug("Removing Seam action listener...");
    }

    public String display() {
        return "view_documents";
    }

    public String addFile() throws ClientException {
        return addFile(getFileUpload(), getFileName());
    }

    @SuppressWarnings("static-access")
    public String addFile(InputStream fileUpload, String fileName)
            throws ClientException {
        try {
            if (fileUpload == null || fileName == null) {
                facesMessages.add(
                        FacesMessage.SEVERITY_ERROR,
                        resourcesAccessor.getMessages().get(
                                "fileImporter.error.nullUploadedFile"));
                return navigationContext.getActionResult(
                        navigationContext.getCurrentDocument(),
                        UserAction.AFTER_CREATE);
            }
            fileName = FileUtils.getCleanFileName(fileName);
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            String path = currentDocument.getPathAsString();
            Blob blob = FileUtils.createSerializableBlob(fileUpload, fileName,
                    null);

            DocumentModel createdDoc = getFileManagerService().createDocumentFromBlob(
                    documentManager, blob, path, true, fileName);
            eventManager.raiseEventsOnDocumentSelected(createdDoc);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    currentDocument);

            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get("document_saved"),
                    resourcesAccessor.getMessages().get(createdDoc.getType()));
            return navigationContext.getActionResult(createdDoc,
                    UserAction.AFTER_CREATE);

        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
    }

    @Deprecated
    // TODO: update the Seam remoting-based desktop plugins to stop calling
    // this
    // method
    @WebRemote
    public boolean canWrite() throws ClientException {
        // let the FolderImporter and FileImporter plugin handle the security
        // checks to avoid hardcoded behavior
        return true;
    }

    protected String getErrorMessage(String errorType, String errorInfo) {
        return getErrorMessage(errorType, errorInfo,
                "message.operation.fails.generic");
    }

    protected String getErrorMessage(String errorType, String errorInfo,
            String errorLabel) {
        return String.format("%s |(%s)| %s", errorType, errorInfo,
                resourcesAccessor.getMessages().get(errorLabel));
    }

    /**
     * @deprecated use addBinaryFileFromPlugin with a Blob argument API to avoid
     *             loading the content in memory
     */
    @Deprecated
    @WebRemote
    public String addFileFromPlugin(String content, String mimetype,
            String fullName, String morePath, Boolean UseBase64)
            throws ClientException {
        try {
            byte[] bcontent;
            if (UseBase64.booleanValue()) {
                bcontent = Base64.decode(content);
            } else {
                bcontent = content.getBytes();
            }
            return addBinaryFileFromPlugin(bcontent, mimetype, fullName,
                    morePath);
        } catch (Throwable t) {
            log.error(t, t);
            return getErrorMessage(TRANSF_ERROR, fullName);
        }
    }

    @WebRemote
    public String addBinaryFileFromPlugin(Blob blob, String fullName,
            String morePath) throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        String curPath = currentDocument.getPathAsString();

        String path = curPath + morePath;
        return createDocumentFromBlob(blob, fullName, path);
    }

    @WebRemote
    public String addBinaryFileFromPlugin(Blob blob, String fullName,
            DocumentModel targetContainer) throws ClientException {
        return createDocumentFromBlob(blob, fullName,
                targetContainer.getPathAsString());
    }

    @SuppressWarnings("static-access")
    protected String createDocumentFromBlob(Blob blob, String fullName,
            String path) throws ClientException {
        DocumentModel createdDoc;
        try {
            createdDoc = getFileManagerService().createDocumentFromBlob(
                    documentManager, blob, path, true, fullName);
        } catch (Throwable t) {
            Throwable unwrappedError = ExceptionHelper.unwrapException(t);
            if (ExceptionHelper.isSecurityError(unwrappedError)) {
                // security check failed
                log.debug("No permissions creating " + fullName);
                return getErrorMessage(SECURITY_ERROR, fullName,
                        "Error.Insuffisant.Rights");
            } else {
                // log error stack trace for server side debugging while giving
                // a generic and localized error message to the client
                log.error("Error importing " + fullName, t);
                return getErrorMessage(TRANSF_ERROR, fullName);
            }
        }
        if (createdDoc == null) {
            log.error("could not create the document " + fullName);
            return getErrorMessage(TRANSF_ERROR, fullName);
        }
        // update the context, raise events to update the seam context
        if (navigationContext.getCurrentDocument().getRef().equals(
                createdDoc.getRef())) {
            // contextManager.updateContext(createdDoc);
            navigationContext.updateDocumentContext(createdDoc);
        }
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                currentDocument);
        eventManager.raiseEventsOnDocumentSelected(createdDoc);
        return createdDoc.getName();
    }

    /**
     * @deprecated Use addBinaryFileFromPlugin(Blob, String, String) to avoid
     *             loading the data in memory as a Bytes array
     */
    @Deprecated
    public String addBinaryFileFromPlugin(byte[] content, String mimetype,
            String fullName, String morePath) throws ClientException {
        Blob blob = StreamingBlob.createFromByteArray(content, null);
        return addBinaryFileFromPlugin(blob, fullName, morePath);
    }

    @WebRemote
    public String addFolderFromPlugin(String fullName, String morePath)
            throws ClientException {
        try {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();

            String curPath = currentDocument.getPathAsString();
            if (!currentDocument.isFolder()) {
                curPath = curPath.substring(0, curPath.lastIndexOf('/'));
            }
            String path = curPath + morePath;

            DocumentModel createdDoc;
            try {
                createdDoc = getFileManagerService().createFolder(
                        documentManager, fullName, path);
            } catch (Throwable t) {
                Throwable unwrappedError = ExceptionHelper.unwrapException(t);
                if (ExceptionHelper.isSecurityError(unwrappedError)) {
                    // security check failed
                    log.debug("No permissions creating folder " + fullName);
                    return getErrorMessage(SECURITY_ERROR, fullName,
                            "Error.Insuffisant.Rights");
                } else {
                    log.error("Couldn't create the folder " + fullName);
                    return getErrorMessage(TRANSF_ERROR, fullName);
                }
            }

            if (createdDoc == null) {
                log.error("Couldn't create the folder " + fullName);
                return getErrorMessage(TRANSF_ERROR, fullName);
            }

            EventManager.raiseEventsOnDocumentSelected(createdDoc);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    currentDocument);
            return createdDoc.getName();
        } catch (Throwable t) {
            log.error(t, t);
            return getErrorMessage(TRANSF_ERROR, fullName);
        }
    }

    @WebRemote
    protected String checkMoveAllowed(DocumentRef docRef,
            DocumentRef containerRef) throws ClientException {

        DocumentModel doc = documentManager.getDocument(docRef);
        DocumentModel container = documentManager.getDocument(containerRef);

        // check that we are not trying to move a folder inside itself

        if ((container.getPathAsString() + "/").startsWith(doc.getPathAsString()
                + "/")) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get("move_impossible"));
            return MOVE_IMPOSSIBLE;
        }
        if (!doc.isProxy() && container.getType().equals("Section")
                && !doc.getType().equals("Section")) {
            // we try to do a publication check browse in sections
            // TODO: use a PUBLICATION_TARGET facet instead of hardcoding the
            // Section type name
            if (!documentManager.hasPermission(containerRef,
                    SecurityConstants.ADD_CHILDREN)) {
                // only publish via D&D if this can be done directly (no wf)
                // => need to have write access
                facesMessages.add(
                        FacesMessage.SEVERITY_WARN,
                        resourcesAccessor.getMessages().get(
                                "move_insuffisant_rights"));
                // TODO: this should be PUBLISH_IMPOSSIBLE
                return MOVE_IMPOSSIBLE;
            }

            if (doc.hasFacet(FacetNames.PUBLISHABLE)) {
                return MOVE_PUBLISH;
            } else {
                facesMessages.add(
                        FacesMessage.SEVERITY_WARN,
                        resourcesAccessor.getMessages().get(
                                "publish_impossible"));
                // TODO: this should be PUBLISH_IMPOSSIBLE
                return MOVE_IMPOSSIBLE;
            }
        }
        // this is a real move operation (not a publication)

        // check the right to remove the document from the source container
        if (!documentManager.hasPermission(doc.getParentRef(),
                SecurityConstants.REMOVE_CHILDREN)
                || !documentManager.hasPermission(doc.getRef(),
                        SecurityConstants.REMOVE)) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get("move_impossible"));
            return MOVE_IMPOSSIBLE;
        }

        // check that we have the right to create the copy in the target
        if (!documentManager.hasPermission(containerRef,
                SecurityConstants.ADD_CHILDREN)) {
            facesMessages.add(
                    FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "move_insuffisant_rights"));
            return MOVE_IMPOSSIBLE;
        }

        if (doc.isProxy()) {
            if (!container.getType().equals("Section")) {
                // do not allow to move a published document back in a
                // workspace
                // TODO: use a PUBLICATION_TARGET facet instead of hardcoding
                // the Section type name
                facesMessages.add(FacesMessage.SEVERITY_WARN,
                        resourcesAccessor.getMessages().get("move_impossible"));
                return MOVE_IMPOSSIBLE;
            }
        } else {
            // check allowed content types constraints for non-proxy documents
            if (!typeManager.isAllowedSubType(doc.getType(),
                    container.getType(), container)) {
                facesMessages.add(FacesMessage.SEVERITY_WARN,
                        resourcesAccessor.getMessages().get("move_impossible"));
                return MOVE_IMPOSSIBLE;
            }
        }

        return MOVE_OK;
    }

    @SuppressWarnings("static-access")
    @WebRemote
    public String moveWithId(String docId, String containerId)
            throws ClientException {
        try {
            String debug = "move " + docId + " into " + containerId;
            log.debug(debug);
            if (docId.startsWith("docRef:")) {
                docId = docId.split("docRef:")[1];
            }
            if (docId.startsWith("docClipboardRef:")) {
                docId = docId.split("docClipboardRef:")[1];
            }
            DocumentRef srcRef = new IdRef(docId);
            String dst = containerId;
            if (dst.startsWith("docRef:")) {
                dst = dst.split("docRef:")[1];
            }
            if (dst.startsWith("nodeRef:")) {
                dst = dst.split("nodeRef:")[1];
            }
            DocumentRef dstRef = new IdRef(dst);

            String moveStatus = checkMoveAllowed(srcRef, dstRef);

            if (moveStatus.equals(MOVE_IMPOSSIBLE)) {
                return debug;
            }

            String action = "document_moved";

            if (moveStatus.equals(MOVE_PUBLISH)) {
                DocumentModel srcDoc = documentManager.getDocument(srcRef);
                DocumentModel dstDoc = documentManager.getDocument(dstRef);
                documentManager.publishDocument(srcDoc, dstDoc);
                action = "document_published";
            } else {
                documentManager.move(srcRef, dstRef, null);
            }

            // delCopyWithId(docId);
            documentManager.save();
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            eventManager.raiseEventsOnDocumentChildrenChange(currentDocument);

            // notify current container
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    currentDocument);
            // notify the other container
            DocumentModel otherContainer = documentManager.getDocument(dstRef);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    otherContainer);

            facesMessages.add(
                    FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(action),
                    resourcesAccessor.getMessages().get(
                            documentManager.getDocument(srcRef).getType()));

            return debug;
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
            return getErrorMessage(MOVE_ERROR, docId);
        }
    }

    @WebRemote
    public String copyWithId(String docId) throws ClientException {
        try {
            String debug = "copying " + docId;
            log.debug(debug);
            if (docId.startsWith("docRef:")) {
                docId = docId.split("docRef:")[1];
            }
            if (docId.startsWith("docClipboardRef:")) {
                docId = docId.split("docClipboardRef:")[1];
            }
            DocumentRef srcRef = new IdRef(docId);
            DocumentModel srcDoc = documentManager.getDocument(srcRef);
            List<DocumentModel> docsToAdd = new ArrayList<DocumentModel>();
            docsToAdd.add(srcDoc);
            clipboardActions.putSelectionInWorkList(docsToAdd, true);
            return debug;
        } catch (Throwable t) {
            log.error(t, t);
            return getErrorMessage(COPY_ERROR, docId);
        }
    }

    @WebRemote
    public String pasteWithId(String docId) throws ClientException {
        try {
            String debug = "pasting " + docId;
            log.debug(debug);
            if (docId.startsWith("pasteRef_")) {
                docId = docId.split("pasteRef_")[1];
            }
            if (docId.startsWith("docClipboardRef:")) {
                docId = docId.split("docClipboardRef:")[1];
            }
            DocumentRef srcRef = new IdRef(docId);
            DocumentModel srcDoc = documentManager.getDocument(srcRef);
            List<DocumentModel> pasteDocs = new ArrayList<DocumentModel>();
            pasteDocs.add(srcDoc);
            clipboardActions.pasteDocumentList(pasteDocs);
            return debug;
        } catch (Throwable t) {
            log.error(t, t);
            return getErrorMessage(PASTE_ERROR, docId);
        }
    }

    public void initialize() {
        log.info("Initializing...");
    }

    public void processUpload(UploadEvent uploadEvent) {
        try {
            if (fileUploadHolder != null) {
                fileUploadHolder.setTempFile(uploadEvent.getUploadItem().getFile());
                fileUploadHolder.setFileName(uploadEvent.getUploadItem().getFileName());
            } else {
                log.error("Unable to reach fileUploadHolder");
            }
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    public void validateMultiplesUpload() throws ClientException,
            FileNotFoundException {
        DocumentModel current = navigationContext.getCurrentDocument();
        validateMultipleUploadForDocument(current);
    }

    @SuppressWarnings("unchecked")
    public void validateMultipleUploadForDocument(DocumentModel current)
            throws ClientException, FileNotFoundException {
        if (!current.hasSchema("files")) {
            return;
        }
        try {
            Collection files = (Collection) current.getProperty("files",
                    "files");
            for (UploadItem uploadItem : getUploadedFiles()) {
                String filename = FileUtils.getCleanFileName(uploadItem.getFileName());
                Blob blob = FileUtils.createSerializableBlob(
                        new FileInputStream(uploadItem.getFile()), filename,
                        null);

                HashMap<String, Object> fileMap = new HashMap<String, Object>(2);
                fileMap.put("file", blob);
                fileMap.put("filename", filename);
                if (!files.contains(fileMap)) {
                    files.add(fileMap);
                }
            }
            current.setProperty("files", "files", files);
            documentManager.saveDocument(current);
            documentManager.save();
        } finally {
            for (UploadItem uploadItem : getUploadedFiles()) {
                uploadItem.getFile().delete();
            }
        }
        Contexts.getConversationContext().remove("fileUploadHolder");
    }

    @SuppressWarnings("unchecked")
    public void performAction(ActionEvent event) {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext eContext = context.getExternalContext();
        String index = eContext.getRequestParameterMap().get("index");

        try {
            DocumentModel current = navigationContext.getCurrentDocument();
            if (!current.hasSchema("files")) {
                return;
            }
            Collection files = (Collection) current.getProperty("files",
                    "files");
            Object file = CollectionUtils.get(files, new Integer(index));
            files.remove(file);
            current.setProperty("files", "files", files);
            documentManager.saveDocument(current);
            documentManager.save();
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    public String validate() throws ClientException {
        if (fileUploadHolder != null && fileUploadHolder.getTempFile() != null) {
            InputStream stream = null;
            try {
                stream = new FileInputStream(fileUploadHolder.getTempFile());
                return addFile(stream, getFileName());
            } catch (Exception e) {
                // NXP-3570 : temporary solution before real fix
                log.error(e, e);
                facesMessages.add(
                        FacesMessage.SEVERITY_ERROR,
                        resourcesAccessor.getMessages().get(
                                "fileImporter.error.unsupportedFile"));
                return null;
            } finally {
                org.nuxeo.common.utils.FileUtils.close(stream);
                // the content of the temporary blob has been
                if (fileUploadHolder.getTempFile() != null) {
                    fileUploadHolder.getTempFile().delete();
                }
            }
        } else {
            facesMessages.add(
                    FacesMessage.SEVERITY_ERROR,
                    resourcesAccessor.getMessages().get(
                            "fileImporter.error.nullUploadedFile"));
            return null;
        }
    }

    public InputStream getFileUpload() {
        if (fileUploadHolder != null) {
            return fileUploadHolder.getFileUpload();
        } else {
            return null;
        }
    }

    public void setFileUpload(InputStream fileUpload) {
        if (fileUploadHolder != null) {
            fileUploadHolder.setFileUpload(fileUpload);
        }
    }

    public String getFileName() {
        if (fileUploadHolder != null) {
            return fileUploadHolder.getFileName();
        }
        return null;
    }

    public void setFileName(String fileName) {
        if (fileUploadHolder != null) {
            fileUploadHolder.setFileName(fileName);
        }
    }

    public DocumentModel getChangeableDocument() {
        return navigationContext.getChangeableDocument();
    }

    public void setChangeableDocument(DocumentModel changeableDocument) {
        navigationContext.setChangeableDocument(changeableDocument);
    }

    public Collection<UploadItem> getUploadedFiles() {
        if (fileUploadHolder != null) {
            return fileUploadHolder.getUploadedFiles();
        } else {
            return null;
        }
    }

    public void setUploadedFiles(Collection<UploadItem> uploadedFiles) {
        if (fileUploadHolder != null) {
            fileUploadHolder.setUploadedFiles(uploadedFiles);
        }
    }

    @WebRemote
    public String removeSingleUploadedFile() throws ClientException {
        if (fileUploadHolder != null) {
            if (fileUploadHolder.getTempFile() != null) {
                fileUploadHolder.getTempFile().delete();
            }
            fileUploadHolder.setFileName(null);
            fileUploadHolder.setTempFile(null);
        }
        return "";
    }

    @WebRemote
    public String removeAllUploadedFile() throws ClientException {
        if (fileUploadHolder != null) {
            for (UploadItem item : fileUploadHolder.getUploadedFiles()) {
                item.getFile().delete();
            }
            fileUploadHolder.getUploadedFiles().clear();
        }
        return "";
    }

    @WebRemote
    public String removeUploadedFile(String fileName) throws ClientException {
        UploadItem fileToDelete = null;

        for (UploadItem file : getUploadedFiles()) {
            String uploadedFileName = file.getFileName();
            if (fileName.equals(uploadedFileName)) {
                fileToDelete = file;
                break;
            }
        }
        if (null != fileToDelete) {
            fileToDelete.getFile().delete();
            getUploadedFiles().remove(fileToDelete);
        }
        return "";
    }

}
