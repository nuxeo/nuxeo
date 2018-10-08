/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Gagnavarslan ehf
 *     Florent Guillaume
 *     Benoit Delbosc
 *     Thierry Martins
 */
package org.nuxeo.ecm.webdav.backend;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.webdav.EscapeUtils;
import org.nuxeo.ecm.webdav.resource.ExistingResource;
import org.nuxeo.runtime.api.Framework;

public class SimpleBackend extends AbstractCoreBackend {

    private static final Log log = LogFactory.getLog(SimpleBackend.class);

    public static final String SOURCE_EDIT_KEYWORD = "source-edit";

    public static final String ALWAYS_CREATE_FILE_PROP = "nuxeo.webdav.always-create-file";

    public static final String TMP_EXTENSION = "tmp";

    public static final String MSOFFICE_TMP_PREFIX = "~$";

    protected String backendDisplayName;

    protected String rootPath;

    protected String rootUrl;

    protected TrashService trashService;

    protected LinkedList<String> orderedBackendNames;

    protected SimpleBackend(String backendDisplayName, String rootPath, String rootUrl, CoreSession session) {
        super(session);
        this.backendDisplayName = backendDisplayName;
        this.rootPath = rootPath;
        this.rootUrl = rootUrl;
    }

    @Override
    public String getRootPath() {
        return rootPath;
    }

    @Override
    public String getRootUrl() {
        return rootUrl;
    }

    @Override
    public String getBackendDisplayName() {
        return backendDisplayName;
    }

    @Override
    public boolean exists(String location) {
        try {
            DocumentModel doc = resolveLocation(location);
            if (doc != null && !isTrashDocument(doc)) {
                return true;
            } else {
                return false;
            }
        } catch (DocumentNotFoundException e) {
            return false;
        }
    }

    private boolean exists(DocumentRef ref) {
        if (getSession().exists(ref)) {
            DocumentModel model = getSession().getDocument(ref);
            return !isTrashDocument(model);
        }
        return false;
    }

    @Override
    public boolean hasPermission(DocumentRef docRef, String permission) {
        return getSession().hasPermission(docRef, permission);
    }

    @Override
    public DocumentModel updateDocument(DocumentModel doc, String name, Blob content) {
        FileManager fileManager = Framework.getService(FileManager.class);
        String parentPath = new Path(doc.getPathAsString()).removeLastSegments(1).toString();
        try {
            // this cannot be done before the update anymore
            // doc.putContextData(SOURCE_EDIT_KEYWORD, "webdav");
            doc = fileManager.createDocumentFromBlob(getSession(), content, parentPath, true, name); // overwrite=true
        } catch (IOException e) {
            throw new NuxeoException("Error while updating document", e);
        }
        return doc;
    }

    @Override
    public LinkedList<String> getVirtualFolderNames() {
        if (orderedBackendNames == null) {
            List<DocumentModel> children = getChildren(new PathRef(rootPath));
            orderedBackendNames = new LinkedList<String>();
            if (children != null) {
                for (DocumentModel model : children) {
                    orderedBackendNames.add(model.getName());
                }
            }
        }
        return orderedBackendNames;
    }

    @Override
    public final boolean isVirtual() {
        return false;
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public final Backend getBackend(String path) {
        return this;
    }

    @Override
    public DocumentModel resolveLocation(String location) {
        Path resolvedLocation = parseLocation(location);

        DocumentModel doc = null;
        DocumentRef docRef = new PathRef(resolvedLocation.toString());
        if (exists(docRef)) {
            doc = getSession().getDocument(docRef);
        } else {
            String encodedPath = EscapeUtils.encodePath(resolvedLocation.toString());
            if (!resolvedLocation.toString().equals(encodedPath)) {
                DocumentRef encodedPathRef = new PathRef(encodedPath);
                if (exists(encodedPathRef)) {
                    doc = getSession().getDocument(encodedPathRef);
                }
            }

            if (doc == null) {
                String filename = resolvedLocation.lastSegment();
                Path parentLocation = resolvedLocation.removeLastSegments(1);

                // first try with spaces (for create New Folder)
                String folderName = filename;
                DocumentRef folderRef = new PathRef(parentLocation.append(folderName).toString());
                if (exists(folderRef)) {
                    doc = getSession().getDocument(folderRef);
                }
                // look for a child
                DocumentModel parentDocument = resolveParent(parentLocation.toString());
                if (parentDocument == null) {
                    // parent doesn't exist, no use looking for a child
                    return null;
                }
                List<DocumentModel> children = getChildren(parentDocument.getRef());
                for (DocumentModel child : children) {
                    BlobHolder bh = child.getAdapter(BlobHolder.class);
                    if (bh != null) {
                        Blob blob = bh.getBlob();
                        if (blob != null) {
                            try {
                                String blobFilename = blob.getFilename();
                                if (filename.equals(blobFilename)) {
                                    doc = child;
                                    break;
                                } else if (EscapeUtils.encodePath(filename).equals(blobFilename)) {
                                    doc = child;
                                    break;
                                } else if (URLEncoder.encode(filename, "UTF-8").equals(blobFilename)) {
                                    doc = child;
                                    break;
                                } else if (encode(blobFilename.getBytes(), "ISO-8859-1").equals(filename)) {
                                    doc = child;
                                    break;
                                }
                            } catch (UnsupportedEncodingException e) {
                                // cannot happen for UTF-8
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }
        return doc;
    }

    protected DocumentModel resolveParent(String location) {
        DocumentModel doc = null;
        DocumentRef docRef = new PathRef(location.toString());
        if (exists(docRef)) {
            doc = getSession().getDocument(docRef);
        } else {
            Path locationPath = new Path(location);
            String filename = locationPath.lastSegment();
            Path parentLocation = locationPath.removeLastSegments(1);

            // first try with spaces (for create New Folder)
            String folderName = filename;
            DocumentRef folderRef = new PathRef(parentLocation.append(folderName).toString());
            if (exists(folderRef)) {
                doc = getSession().getDocument(folderRef);
            }
        }
        return doc;
    }

    @Override
    public Path parseLocation(String location) {
        Path finalLocation = new Path(rootPath);
        Path rootUrlPath = new Path(rootUrl);
        Path urlLocation = new Path(location);
        Path cutLocation = urlLocation.removeFirstSegments(rootUrlPath.segmentCount());
        finalLocation = finalLocation.append(cutLocation);
        String fileName = finalLocation.lastSegment();
        String parentPath = finalLocation.removeLastSegments(1).toString();
        return new Path(parentPath).append(fileName);
    }

    @Override
    public void removeItem(String location) {
        DocumentModel docToRemove = resolveLocation(location);
        if (docToRemove == null) {
            throw new NuxeoException("Document path not found: " + location);
        }
        removeItem(docToRemove.getRef());
    }

    @Override
    public void removeItem(DocumentRef ref) {
        DocumentModel doc = getSession().getDocument(ref);
        if (doc != null) {
            if (isTemporaryFile(doc)) {
                session.removeDocument(ref);
            } else {
                getTrashService().trashDocuments(Arrays.asList(doc));
            }
        } else {
            log.warn("Can't move document " + ref.toString() + " to trash. Document did not found.");
        }
    }

    protected boolean isTemporaryFile(DocumentModel doc) {
        String name = doc.getName();
        return FilenameUtils.getExtension(name).equalsIgnoreCase(TMP_EXTENSION) || name.startsWith(MSOFFICE_TMP_PREFIX);
    }

    @Override
    public boolean isRename(String source, String destination) {
        Path sourcePath = new Path(source);
        Path destinationPath = new Path(destination);
        return sourcePath.removeLastSegments(1).toString().equals(destinationPath.removeLastSegments(1).toString());
    }

    @Override
    public void renameItem(DocumentModel source, String destinationName) {
        source.putContextData(SOURCE_EDIT_KEYWORD, "webdav");
        if (source.isFolder()) {
            source.setPropertyValue("dc:title", destinationName);
            moveItem(source, source.getParentRef(), destinationName);
            source.putContextData("renameSource", "webdav");
            getSession().saveDocument(source);
        } else {
            source.setPropertyValue("dc:title", destinationName);
            BlobHolder bh = source.getAdapter(BlobHolder.class);
            boolean blobUpdated = false;
            if (bh != null) {
                Blob blob = bh.getBlob();
                if (blob != null) {
                    blob.setFilename(destinationName);
                    // as the name may have changed, reset the mime type so that the correct one will be computed
                    blob.setMimeType(null);
                    blobUpdated = true;
                    bh.setBlob(blob);
                    getSession().saveDocument(source);
                }
            }
            if (!blobUpdated) {
                source.setPropertyValue("dc:title", destinationName);
                moveItem(source, source.getParentRef(), destinationName);
                getSession().saveDocument(source);
            }
        }
    }

    @Override
    public DocumentModel moveItem(DocumentModel source, PathRef targetParentRef) {
        return moveItem(source, targetParentRef, source.getName());
    }

    @Override
    public DocumentModel moveItem(DocumentModel source, DocumentRef targetParentRef, String name)
            {
        cleanTrashPath(targetParentRef, name);
        BlobHolder blobHolder = source.getAdapter(BlobHolder.class);
        if (blobHolder != null) {
            Blob blob = blobHolder.getBlob();
            if (blob != null) {
                blob.setFilename(name);
                blobHolder.setBlob(blob);
                source = getSession().saveDocument(source);
            }
        }
        DocumentModel model = getSession().move(source.getRef(), targetParentRef, name);
        return model;
    }

    @Override
    public DocumentModel copyItem(DocumentModel source, PathRef targetParentRef) {
        DocumentModel model = getSession().copy(source.getRef(), targetParentRef, source.getName());
        return model;
    }

    @Override
    public DocumentModel createFolder(String parentPath, String name) {
        DocumentModel parent = resolveLocation(parentPath);
        if (!parent.isFolder()) {
            throw new NuxeoException("Can not create a child in a non folderish node");
        }

        String targetType = "Folder";
        if ("WorkspaceRoot".equals(parent.getType())) {
            targetType = "Workspace";
        }
        // name = cleanName(name);
        cleanTrashPath(parent, name);
        DocumentModel folder = getSession().createDocumentModel(parent.getPathAsString(), name, targetType);
        folder.setPropertyValue("dc:title", name);
        folder = getSession().createDocument(folder);
        return folder;
    }

    @Override
    public DocumentModel createFile(String parentPath, String name, Blob content) {
        DocumentModel parent = resolveLocation(parentPath);
        if (!parent.isFolder()) {
            throw new NuxeoException("Can not create a child in a non folderish node");
        }
        try {
            cleanTrashPath(parent, name);
            DocumentModel file;
            if (Framework.isBooleanPropertyTrue(ALWAYS_CREATE_FILE_PROP)) {
                // compat for older versions, always create a File
                file = getSession().createDocumentModel(parent.getPathAsString(), name, "File");
                file.setPropertyValue("dc:title", name);
                if (content != null) {
                    BlobHolder bh = file.getAdapter(BlobHolder.class);
                    if (bh != null) {
                        bh.setBlob(content);
                    }
                }
                file = getSession().createDocument(file);
            } else {
                // use the FileManager to create the file
                FileManager fileManager = Framework.getService(FileManager.class);
                file = fileManager.createDocumentFromBlob(getSession(), content, parent.getPathAsString(), false, name);
            }
            return file;
        } catch (IOException e) {
            throw new NuxeoException("Error child creating new folder", e);
        }
    }

    @Override
    public DocumentModel createFile(String parentPath, String name) {
        Blob blob = Blobs.createBlob("", "application/octet-stream");
        return createFile(parentPath, name, blob);
    }

    @Override
    public String getDisplayName(DocumentModel doc) {
        if (doc.isFolder()) {
            return doc.getName();
        } else {
            String fileName = getFileName(doc);
            if (fileName == null) {
                fileName = doc.getName();
            }
            return fileName;
        }
    }

    @Override
    public List<DocumentModel> getChildren(DocumentRef ref) {
        List<DocumentModel> result = new ArrayList<DocumentModel>();
        List<DocumentModel> children = getSession(true).getChildren(ref);
        for (DocumentModel child : children) {
            if (child.hasFacet(FacetNames.HIDDEN_IN_NAVIGATION)) {
                continue;
            }
            if (child.isTrashed()) {
                continue;
            }
            if (!child.hasSchema("dublincore")) {
                continue;
            }
            if (child.hasFacet(FacetNames.FOLDERISH) || child.getAdapter(BlobHolder.class) != null) {
                result.add(child);
            }
        }
        return result;
    }

    @Override
    public boolean isLocked(DocumentRef ref) {
        Lock lock = getSession().getLockInfo(ref);
        return lock != null;
    }

    @Override
    public boolean canUnlock(DocumentRef ref) {
        Principal principal = getSession().getPrincipal();
        if (principal == null || StringUtils.isEmpty(principal.getName())) {
            log.error("Empty session principal. Error while canUnlock check.");
            return false;
        }
        String checkoutUser = getCheckoutUser(ref);
        return principal.getName().equals(checkoutUser);
    }

    @Override
    public String lock(DocumentRef ref) {
        if (getSession().hasPermission(ref, SecurityConstants.WRITE_PROPERTIES)) {
            Lock lock = getSession().setLock(ref);
            return lock.getOwner();
        }
        return ExistingResource.READONLY_TOKEN;
    }

    @Override
    public boolean unlock(DocumentRef ref) {
        if (!canUnlock(ref)) {
            return false;
        }
        getSession().removeLock(ref);
        return true;
    }

    @Override
    public String getCheckoutUser(DocumentRef ref) {
        Lock lock = getSession().getLockInfo(ref);
        if (lock != null) {
            return lock.getOwner();
        }
        return null;
    }

    @Override
    public String getVirtualPath(String path) {
        if (path.startsWith(this.rootPath)) {
            return rootUrl + path.substring(this.rootPath.length());
        } else {
            return null;
        }
    }

    @Override
    public DocumentModel getDocument(String location) {
        return resolveLocation(location);
    }

    protected String getFileName(DocumentModel doc) {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null) {
            Blob blob = bh.getBlob();
            if (blob != null) {
                return blob.getFilename();
            }
        }
        return null;
    }

    protected boolean isTrashDocument(DocumentModel model) {
        return model == null || model.isTrashed();
    }

    protected TrashService getTrashService() {
        if (trashService == null) {
            trashService = Framework.getService(TrashService.class);
        }
        return trashService;
    }

    protected boolean cleanTrashPath(DocumentModel parent, String name) {
        Path checkedPath = new Path(parent.getPathAsString()).append(name);
        if (getSession().exists(new PathRef(checkedPath.toString()))) {
            DocumentModel model = getSession().getDocument(new PathRef(checkedPath.toString()));
            if (model != null && model.isTrashed()) {
                name = name + "." + System.currentTimeMillis();
                getSession().move(model.getRef(), parent.getRef(), name);
                return true;
            }
        }
        return false;
    }

    protected boolean cleanTrashPath(DocumentRef parentRef, String name) {
        DocumentModel parent = getSession().getDocument(parentRef);
        return cleanTrashPath(parent, name);
    }

    protected String encode(byte[] bytes, String encoding) {
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new NuxeoException("Unsupported encoding " + encoding);
        }
    }

}
