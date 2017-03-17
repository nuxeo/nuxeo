/*
 * (C) Copyright 2006-2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Andreas Kalogeropoulos
 *     Anahide Tchertchian
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.webapp.filemanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.common.utils.Base64;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.RecoverableClientException;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.UserAction;
import org.nuxeo.ecm.platform.ui.web.util.files.FileUtils;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.ecm.webapp.clipboard.ClipboardActions;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.richfaces.event.FileUploadEvent;

@Name("FileManageActions")
@Scope(ScopeType.EVENT)
@Install(precedence = Install.FRAMEWORK)
public class FileManageActionsBean implements FileManageActions {

    private static final Log log = LogFactory.getLog(FileManageActionsBean.class);

    public static final String TRANSF_ERROR = "TRANSF_ERROR";

    public static final String SECURITY_ERROR = "SECURITY_ERROR";

    public static final String MOVE_ERROR = "MOVE_ERROR";

    public static final String COPY_ERROR = "COPY_ERROR";

    public static final String PASTE_ERROR = "PASTE_ERROR";

    public static final String MOVE_IMPOSSIBLE = "MOVE_IMPOSSIBLE";

    public static final String MOVE_PUBLISH = "MOVE_PUBLISH";

    public static final String MOVE_OK = "MOVE_OK";

    protected static final String FILES_SCHEMA = "files";

    protected static final String FILES_PROPERTY = FILES_SCHEMA + ":files";

    // TODO NXP-13568: this should not be hardcoded on the doc type
    protected static final String SECTION_DOCTYPE = "Section";

    @In(create = true, required = false)
    protected CoreSession documentManager;

    @In(create = true)
    protected TypeManager typeManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected transient DocumentActions documentActions;

    @In(create = true)
    protected ClipboardActions clipboardActions;

    @In(create = true, required = false)
    protected UploadItemHolder fileUploadHolder;

    @In(create = true, required = false)
    protected UploadItemHolderCycleManager fileUploadHolderCycle;

    /**
     * Helper field to get the filename to remove.
     *
     * @since 5.9.2
     */
    protected String fileToRemove;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected Map<String, String> messages;

    protected FileManager fileManager;

    protected FileManager getFileManagerService() throws ClientException {
        if (fileManager == null) {
            try {
                fileManager = Framework.getService(FileManager.class);
            } catch (Exception e) {
                log.error("Unable to get FileManager service ", e);
                throw new ClientException("Unable to get FileManager service ", e);
            }
        }
        return fileManager;
    }

    @Override
    public String display() {
        return "view_documents";
    }

    /**
     * Creates a document from the file held in the fileUploadHolder. Takes responsibility for the fileUploadHolder
     * temporary file.
     */
    @Override
    public String addFile() throws ClientException {
        NxUploadedFile uploadedFile = fileUploadHolder.getUploadedFiles().iterator().next();
        File tempFile = uploadedFile.getFile();
        String fileName = uploadedFile.getName();
        if (tempFile == null || fileName == null) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get("fileImporter.error.nullUploadedFile"));
            return navigationContext.getActionResult(navigationContext.getCurrentDocument(), UserAction.AFTER_CREATE);
        }
        fileName = FileUtils.getCleanFileName(fileName);
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        String path = currentDocument.getPathAsString();
        Blob blob = FileUtils.createTemporaryFileBlob(tempFile, fileName, null);
        DocumentModel createdDoc = null;
        try {
            createdDoc = getFileManagerService().createDocumentFromBlob(documentManager, blob, path, true, fileName);
        } catch (IOException e) {
            throw new ClientException("Can not write blob for" + fileName, e);
        } catch (ClientException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            new ClientException("Caught general system exception, throwing client exception ", e);
        }
        EventManager.raiseEventsOnDocumentSelected(createdDoc);
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED, currentDocument);

        facesMessages.add(StatusMessage.Severity.INFO, messages.get("document_saved"),
                messages.get(createdDoc.getType()));
        return navigationContext.getActionResult(createdDoc, UserAction.AFTER_CREATE);
    }

    @Override
    @Deprecated
    // TODO: update the Seam remoting-based desktop plugins to stop calling
    // this method
    @WebRemote
    public boolean canWrite() throws ClientException {
        // let the FolderImporter and FileImporter plugin handle the security
        // checks to avoid hardcoded behavior
        return true;
    }

    protected String getErrorMessage(String errorType, String errorInfo) {
        return getErrorMessage(errorType, errorInfo, "message.operation.fails.generic");
    }

    protected String getErrorMessage(String errorType, String errorInfo, String errorLabel) {
        return String.format("%s |(%s)| %s", errorType, errorInfo, messages.get(errorLabel));
    }

    /**
     * @deprecated use addBinaryFileFromPlugin with a Blob argument API to avoid loading the content in memory
     */
    @Override
    @Deprecated
    @WebRemote
    public String addFileFromPlugin(String content, String mimetype, String fullName, String morePath,
            Boolean UseBase64) throws ClientException {
        try {
            byte[] bcontent;
            if (UseBase64.booleanValue()) {
                bcontent = Base64.decode(content);
            } else {
                bcontent = content.getBytes();
            }
            return addBinaryFileFromPlugin(bcontent, mimetype, fullName, morePath);
        } catch (ClientException e) {
            throw new RecoverableClientException("Cannot validate, caught client exception",
                    "message.operation.fails.generic", null, e);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof RecoverableClientException) {
                throw e;
            }
            throw new RecoverableClientException("Cannot validate, caught runtime", "error.db.fs", null, e);
        }
    }

    @Override
    @WebRemote
    public String addBinaryFileFromPlugin(Blob blob, String fullName, String morePath) throws ClientException {
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        String curPath = currentDocument.getPathAsString();

        String path = curPath + morePath;
        return createDocumentFromBlob(blob, fullName, path);
    }

    @Override
    @WebRemote
    public String addBinaryFileFromPlugin(Blob blob, String fullName, DocumentModel targetContainer)
            throws ClientException {
        return createDocumentFromBlob(blob, fullName, targetContainer.getPathAsString());
    }

    protected String createDocumentFromBlob(Blob blob, String fullName, String path) throws ClientException {
        DocumentModel createdDoc;
        try {
            createdDoc = getFileManagerService().createDocumentFromBlob(documentManager, blob, path, true, fullName);
        } catch (Throwable t) {
            Throwable unwrappedError = ExceptionHelper.unwrapException(t);
            if (ExceptionHelper.isSecurityError(unwrappedError)) {
                // security check failed
                log.debug("No permissions creating " + fullName);
                return getErrorMessage(SECURITY_ERROR, fullName, "Error.Insuffisant.Rights");
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
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument.getRef().equals(createdDoc.getRef())) {
            navigationContext.updateDocumentContext(createdDoc);
        }
        Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED, currentDocument);
        EventManager.raiseEventsOnDocumentSelected(createdDoc);
        return createdDoc.getName();
    }

    /**
     * @deprecated Use addBinaryFileFromPlugin(Blob, String, String) to avoid loading the data in memory as a Bytes
     *             array
     */
    @Deprecated
    public String addBinaryFileFromPlugin(byte[] content, String mimetype, String fullName, String morePath)
            throws ClientException {
        Blob blob = StreamingBlob.createFromByteArray(content, null);
        return addBinaryFileFromPlugin(blob, fullName, morePath);
    }

    @Override
    @WebRemote
    public String addFolderFromPlugin(String fullName, String morePath) throws ClientException {
        try {
            DocumentModel currentDocument = navigationContext.getCurrentDocument();

            String curPath = currentDocument.getPathAsString();
            if (!currentDocument.isFolder()) {
                curPath = curPath.substring(0, curPath.lastIndexOf('/'));
            }
            String path = curPath + morePath;

            DocumentModel createdDoc;
            try {
                createdDoc = getFileManagerService().createFolder(documentManager, fullName, path, true);
            } catch (Throwable t) {

                Throwable unwrappedError = ExceptionHelper.unwrapException(t);
                if (ExceptionHelper.isSecurityError(unwrappedError)) {
                    // security check failed
                    log.debug("No permissions creating folder " + fullName);
                    return getErrorMessage(SECURITY_ERROR, fullName, "Error.Insuffisant.Rights");
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
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED, currentDocument);
            return createdDoc.getName();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof RecoverableClientException) {
                throw e;
            }
            throw new RecoverableClientException("Cannot validate, caught runtime", "error.db.fs", null, e);
        }
    }

    @WebRemote
    protected String checkMoveAllowed(DocumentRef docRef, DocumentRef containerRef) throws ClientException {

        DocumentModel doc = documentManager.getDocument(docRef);
        DocumentModel container = documentManager.getDocument(containerRef);

        // check that we are not trying to move a folder inside itself

        if ((container.getPathAsString() + "/").startsWith(doc.getPathAsString() + "/")) {
            facesMessages.add(StatusMessage.Severity.WARN, messages.get("move_impossible"));
            return MOVE_IMPOSSIBLE;
        }
        if (!doc.isProxy() && container.hasFacet(FacetNames.PUBLISH_SPACE) && !doc.hasFacet(FacetNames.PUBLISH_SPACE)) {
            // we try to do a publication check browse in sections
            if (!documentManager.hasPermission(containerRef, SecurityConstants.ADD_CHILDREN)) {
                // only publish via D&D if this can be done directly (no wf)
                // => need to have write access
                facesMessages.add(StatusMessage.Severity.WARN, messages.get("move_insuffisant_rights"));
                // TODO: this should be PUBLISH_IMPOSSIBLE
                return MOVE_IMPOSSIBLE;
            }

            if (doc.hasFacet(FacetNames.PUBLISHABLE)) {
                return MOVE_PUBLISH;
            } else {
                facesMessages.add(StatusMessage.Severity.WARN, messages.get("publish_impossible"));
                // TODO: this should be PUBLISH_IMPOSSIBLE
                return MOVE_IMPOSSIBLE;
            }
        }
        // this is a real move operation (not a publication)

        // check the right to remove the document from the source container
        if (!documentManager.hasPermission(doc.getParentRef(), SecurityConstants.REMOVE_CHILDREN)
                || !documentManager.hasPermission(doc.getRef(), SecurityConstants.REMOVE)) {
            facesMessages.add(StatusMessage.Severity.WARN, messages.get("move_impossible"));
            return MOVE_IMPOSSIBLE;
        }

        // check that we have the right to create the copy in the target
        if (!documentManager.hasPermission(containerRef, SecurityConstants.ADD_CHILDREN)) {
            facesMessages.add(StatusMessage.Severity.WARN, messages.get("move_insuffisant_rights"));
            return MOVE_IMPOSSIBLE;
        }

        if (doc.isProxy()) {
            if (!container.hasFacet(FacetNames.PUBLISH_SPACE)) {
                // do not allow to move a published document back in a
                // workspace
                facesMessages.add(StatusMessage.Severity.WARN, messages.get("move_impossible"));
                return MOVE_IMPOSSIBLE;
            }
        } else {
            // check allowed content types constraints for non-proxy documents
            if (!typeManager.isAllowedSubType(doc.getType(), container.getType(), container)) {
                facesMessages.add(StatusMessage.Severity.WARN, messages.get("move_impossible"));
                return MOVE_IMPOSSIBLE;
            }
        }

        return MOVE_OK;
    }

    @Override
    @WebRemote
    public String moveWithId(String docId, String containerId) throws ClientException {
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
            EventManager.raiseEventsOnDocumentChildrenChange(currentDocument);

            // notify current container
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED, currentDocument);
            // notify the other container
            DocumentModel otherContainer = documentManager.getDocument(dstRef);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED, otherContainer);

            facesMessages.add(StatusMessage.Severity.INFO, messages.get(action),
                    messages.get(documentManager.getDocument(srcRef).getType()));

            return debug;
        } catch (ClientException e) {
            throw new RecoverableClientException("Cannot validate, caught client exception",
                    "message.operation.fails.generic", null, e);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof RecoverableClientException) {
                throw e;
            }
            throw new RecoverableClientException("Cannot validate, caught runtime", "error.db.fs", null, e);
        }
    }

    @Override
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
            clipboardActions.putSelectionInWorkList(docsToAdd, Boolean.TRUE);
            return debug;
        } catch (ClientException e) {
            throw new RecoverableClientException("Cannot validate, caught client exception",
                    "message.operation.fails.generic", null, e);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof RecoverableClientException) {
                throw e;
            }
            throw new RecoverableClientException("Cannot validate, caught runtime", "error.db.fs", null, e);
        }

    }

    @Override
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
        } catch (ClientException e) {
            throw new RecoverableClientException("Cannot validate, caught client exception",
                    "message.operation.fails.generic", null, e);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof RecoverableClientException) {
                throw e;
            }
            throw new RecoverableClientException("Cannot validate, caught runtime", "error.db.fs", null, e);
        }
    }

    public void processUpload(FileUploadEvent uploadEvent) {
        try {
            if (fileUploadHolder != null) {
                Collection<NxUploadedFile> temp = fileUploadHolder.getUploadedFiles();
                File file = null;
                String jstTmpFileDir = Framework.getProperty(NUXEO_JSF_TMP_DIR_PROP);
                if (StringUtils.isNotBlank(jstTmpFileDir)) {
                    file = File.createTempFile("FileManageActionsFile", null, new File(jstTmpFileDir));
                } else {
                    file = File.createTempFile("FileManageActionsFile", null);
                }
                InputStream in = uploadEvent.getUploadedFile().getInputStream();
                org.nuxeo.common.utils.FileUtils.copyToFile(in, file);
                temp.add(new NxUploadedFile(uploadEvent.getUploadedFile().getName(),
                        uploadEvent.getUploadedFile().getContentType(), file));
                fileUploadHolder.setUploadedFiles(temp);
            } else {
                log.error("Unable to reach fileUploadHolder");
            }
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    public void validateMultiplesUpload() throws ClientException, FileNotFoundException, IOException {
        DocumentModel current = navigationContext.getCurrentDocument();
        validateMultipleUploadForDocument(current);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void validateMultipleUploadForDocument(DocumentModel current)
            throws ClientException, FileNotFoundException, IOException {
        if (!current.hasSchema(FILES_SCHEMA)) {
            return;
        }
        Collection<NxUploadedFile> nxuploadFiles = getUploadedFiles();
        try {
            ArrayList files = (ArrayList) current.getPropertyValue(FILES_PROPERTY);
            if (nxuploadFiles != null) {
                for (NxUploadedFile uploadItem : nxuploadFiles) {
                    String filename = FileUtils.getCleanFileName(uploadItem.getName());
                    Blob blob = FileUtils.createTemporaryFileBlob(uploadItem.getFile(), filename,
                            uploadItem.getContentType());
                    HashMap<String, Object> fileMap = new HashMap<String, Object>(2);
                    fileMap.put("file", blob);
                    fileMap.put("filename", filename);
                    if (!files.contains(fileMap)) {
                        files.add(fileMap);
                    }
                }
            }
            current.setPropertyValue(FILES_PROPERTY, files);
            documentActions.updateDocument(current, Boolean.TRUE);
        } finally {
            if (nxuploadFiles != null) {
                for (NxUploadedFile uploadItem : nxuploadFiles) {
                    File tempFile = uploadItem.getFile();
                    if (tempFile != null && tempFile.exists()) {
                        Framework.trackFile(tempFile, tempFile);
                    }
                }
            }
        }
    }

    @SuppressWarnings({ "rawtypes" })
    public void performAction(ActionEvent event) {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext eContext = context.getExternalContext();
        String index = eContext.getRequestParameterMap().get("index");

        try {
            DocumentModel current = navigationContext.getCurrentDocument();
            if (!current.hasSchema(FILES_SCHEMA)) {
                return;
            }
            ArrayList files = (ArrayList) current.getPropertyValue(FILES_PROPERTY);
            Object file = CollectionUtils.get(files, Integer.valueOf(index).intValue());
            files.remove(file);
            current.setPropertyValue(FILES_PROPERTY, files);
            documentActions.updateDocument(current, Boolean.TRUE);
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    public String validate() throws ClientException {

        NxUploadedFile uploadedFile;
        if (fileUploadHolder == null || fileUploadHolder.getUploadedFiles().isEmpty()
                || (uploadedFile = fileUploadHolder.getUploadedFiles().iterator().next()) == null) {
            facesMessages.add(StatusMessage.Severity.ERROR, messages.get("fileImporter.error.nullUploadedFile"));
            return null;
        }
        try {
            return addFile();
        } catch (ClientException e) {
            throw new RecoverableClientException("Cannot validate, caught client exception",
                    "message.operation.fails.generic", null, e);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof RecoverableClientException) {
                throw e;
            }
            throw new RecoverableClientException("Cannot validate, caught runtime", "error.db.fs", null, e);
        } finally {
            if (uploadedFile != null && uploadedFile.getFile().exists()) {
                Framework.trackFile(uploadedFile.getFile(), uploadedFile.getFile());
            }
        }
    }

    @Override
    public InputStream getFileUpload() {
        if (fileUploadHolder != null) {
            return fileUploadHolder.getFileUpload();
        } else {
            return null;
        }
    }

    @Override
    public void setFileUpload(InputStream fileUpload) {
        if (fileUploadHolder != null) {
            fileUploadHolder.setFileUpload(fileUpload);
        }
    }

    @Override
    public String getFileName() {
        if (fileUploadHolder != null) {
            return fileUploadHolder.getFileName();
        }
        return null;
    }

    @Override
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

    public Collection<NxUploadedFile> getUploadedFiles() {
        if (fileUploadHolder != null) {
            return fileUploadHolder.getUploadedFiles();
        } else {
            return null;
        }
    }

    public void setUploadedFiles(Collection<NxUploadedFile> uploadedFiles) {
        if (fileUploadHolder != null) {
            fileUploadHolder.setUploadedFiles(uploadedFiles);
        }
    }

    @Override
    @WebRemote
    public String removeSingleUploadedFile() throws ClientException {
        return removeAllUploadedFile();
    }

    @Override
    public void setFileToRemove(String fileToRemove) {
        this.fileToRemove = fileToRemove;
    }

    @Override
    public String removeOneOrAllUploadedFiles(ActionEvent action) throws ClientException {
        if (StringUtils.isBlank(fileToRemove)) {
            return removeAllUploadedFile();
        } else {
            return removeUploadedFile(fileToRemove);
        }
    }

    @Override
    @WebRemote
    public String removeAllUploadedFile() throws ClientException {
        if (fileUploadHolder != null) {
            Collection<NxUploadedFile> files = getUploadedFiles();
            if (files != null) {
                for (NxUploadedFile item : files) {
                    item.getFile().delete();
                }
            }
            setUploadedFiles(new ArrayList<NxUploadedFile>());
        }
        return "";
    }

    @Override
    @WebRemote
    public String removeUploadedFile(String fileName) throws ClientException {
        NxUploadedFile fileToDelete = null;

        // Retrieve only the real filename
        // IE stores the full path of the file as the filename (ie.
        // Z:\\path\\to\\file)
        fileName = FilenameUtils.getName(fileName);
        Collection<NxUploadedFile> files = getUploadedFiles();
        if (files != null) {
            for (NxUploadedFile file : files) {
                String uploadedFileName = file.getName();
                if (fileName.equals(uploadedFileName)) {
                    fileToDelete = file;
                    break;
                }
            }
        }
        if (fileToDelete != null) {
            fileToDelete.getFile().delete();
            files.remove(fileToDelete);
            setUploadedFiles(files);
        }
        return "";
    }

    /**
     * A Blob based on a File but whose contract says that the file is allowed to be moved to another filesystem
     * location if needed. (The move is done by getting the StreamSource from the Blob, casting to FileSource,
     *
     * @since 5.6.0-HF19
     * @deprecated Since 5.7.2. See {@link org.nuxeo.ecm.platform.ui.web.util.files.FileUtils.TemporaryFileBlob}
     */
    @Deprecated
    public static class TemporaryFileBlob extends StreamingBlob {

        private static final long serialVersionUID = 1L;

        public TemporaryFileBlob(File file, String mimeType, String encoding, String filename, String digest) {
            super(new FileSource(file), mimeType, encoding, filename, digest);
        }

        @Override
        public boolean isTemporary() {
            return true; // for SQLSession#getBinary
        }

        @Override
        public FileSource getStreamSource() {
            return (FileSource) src;
        }
    }

    /**
     * Creates a TemporaryFileBlob.
     *
     * @since 5.6.0-HF19
     * @deprecated Since 5.7.2. See {@link org.nuxeo.ecm.platform.ui.web.util.files.FileUtils#createTemporaryFileBlob}
     */
    @Deprecated
    protected static Blob createTemporaryFileBlob(File file, String filename, String mimeType) {
        return FileUtils.createTemporaryFileBlob(file, filename, mimeType);
    }

}
