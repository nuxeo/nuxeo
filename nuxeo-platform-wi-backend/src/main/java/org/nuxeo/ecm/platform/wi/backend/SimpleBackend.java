package org.nuxeo.ecm.platform.wi.backend;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.webdav.Util;
import org.nuxeo.ecm.webdav.backend.WebDavBackend;
import org.nuxeo.runtime.api.Framework;

import java.net.URLEncoder;
import java.security.Principal;
import java.text.DateFormat;
import java.util.*;

import static org.nuxeo.ecm.webdav.Util.cleanName;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class SimpleBackend extends AbstractCoreBackend {

    private static final Log log = LogFactory.getLog(SimpleBackend.class);
    private static final int PATH_CACHE_SIZE = 255;

    protected String backendDisplayName;
    protected String rootPath;
    protected String rootUrl;
    protected TrashService trashService;
    protected PathCache pathCache;

    protected SimpleBackend(String backendDisplayName, String rootPath, String rootUrl, CoreSession session) {
        super(session);
        this.backendDisplayName = backendDisplayName;
        this.rootPath = rootPath;
        this.rootUrl = rootUrl;
    }

    protected SimpleBackend(String backendDisplayName, String rootPath, String rootUrl) {
        this(backendDisplayName, rootPath, rootUrl, null);
    }

    protected PathCache getPathCache() throws ClientException {
        if (pathCache == null) {
            pathCache = new PathCache(getSession(), PATH_CACHE_SIZE);
        }
        return pathCache;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public String getBackendDisplayName() {
        return backendDisplayName;
    }

    public boolean exists(String location) {
        try {
            DocumentModel doc = resolveLocation(location);
            if (doc != null && !isTrashDocument(doc)) {
                return true;
            } else {
                return false;
            }
        } catch (ClientException e) {
            return false;
        }
    }

    private boolean exists(DocumentRef ref) throws ClientException {
        if (getSession().exists(ref)) {
            DocumentModel model = getSession().getDocument(ref);
            return !isTrashDocument(model);
        }
        return false;
    }

    public boolean hasPermission(DocumentRef docRef, String permission) throws ClientException {
        return getSession().hasPermission(docRef, permission);
    }

    public DocumentModel saveDocument(DocumentModel doc) throws ClientException {
        return getSession().saveDocument(doc);
    }

    public LinkedList<String> getVirtualFolderNames() {
        return new LinkedList<String>();
    }

    public final boolean isVirtual() {
        return false;
    }

    public boolean isRoot() {
        return false;
    }

    public final Backend getBackend(String path) {
        return this;
    }

    public DocumentModel resolveLocation(String location) throws ClientException {
        Path resolvedLocation = parseLocation(location);

        DocumentModel doc = null;
        doc = getPathCache().get(resolvedLocation.toString());
        if (doc != null) {
            return doc;
        }

        DocumentRef docRef = new PathRef(resolvedLocation.toString());
        if (exists(docRef)) {
            doc = getSession().getDocument(docRef);
        } else {

            String filename = resolvedLocation.lastSegment();
            Path parentLocation = resolvedLocation.removeLastSegments(1);

            // first try with spaces (for create New Folder)
            String folderName = cleanName(filename);
            DocumentRef folderRef = new PathRef(parentLocation.append(folderName).toString());
            if (exists(folderRef)) {
                doc = getSession().getDocument(folderRef);
            }
            // look for a child
            DocumentModel parentDocument = resolveParent(parentLocation.toString());
            if (parentDocument == null) {
                log.warn("Unable to find parent for item " + location);
                throw new ClientException("Unable to find parent for item " + location);
            }
            List<DocumentModel> children = getChildren(parentDocument.getRef());
            for (DocumentModel child : children) {
                BlobHolder bh = child.getAdapter(BlobHolder.class);
                if (bh != null) {
                    Blob blob = bh.getBlob();
                    if (blob != null && filename.equals(blob.getFilename())) {
                        doc = child;
                        break;
                    } else if (blob != null && URLEncoder.encode(filename).equals(blob.getFilename())) {
                        doc = child;
                        break;
                    } else if (blob != null && Util.encode(blob.getFilename().getBytes(), "ISO-8859-1").equals(filename)) {
                        doc = child;
                        break;
                    }
                }

            }
        }
        getPathCache().put(resolvedLocation.toString(), doc);
        return doc;
    }

    protected DocumentModel resolveParent(String location) throws ClientException {
        DocumentModel doc = null;
        doc = getPathCache().get(location.toString());
        if (doc != null) {
            return doc;
        }

        DocumentRef docRef = new PathRef(location.toString());
        if (exists(docRef)) {
            doc = getSession().getDocument(docRef);
        } else {
            Path locationPath = new Path(location);
            String filename = locationPath.lastSegment();
            Path parentLocation = locationPath.removeLastSegments(1);

            // first try with spaces (for create New Folder)
            String folderName = cleanName(filename);
            DocumentRef folderRef = new PathRef(parentLocation.append(folderName).toString());
            if (exists(folderRef)) {
                doc = getSession().getDocument(folderRef);
            }
        }
        getPathCache().put(location.toString(), doc);
        return doc;
    }

    public Path parseLocation(String location) {
        Path finalLocation = new Path(rootPath);
        Path rootUrlPath = new Path(rootUrl);
        Path urlLocation = new Path(location);
        Path cutLocation = urlLocation.removeFirstSegments(rootUrlPath.segmentCount());
        finalLocation = finalLocation.append(cutLocation);
        String fileName = finalLocation.lastSegment();
        String parentPath = cleanName(finalLocation.removeLastSegments(1).toString());
        return new Path(parentPath).append(fileName);
    }

    public void removeItem(String location) throws ClientException {
        DocumentModel docToRemove = null;
        try {
            docToRemove = resolveLocation(location);
        } catch (Exception e) {
            throw new ClientException("Error while resolving document path", e);
        }
        if (docToRemove == null) {
            throw new ClientException("Document path not found");
        }
        removeItem(docToRemove.getRef());
    }

    public void removeItem(DocumentRef ref) throws ClientException {
        try {
            DocumentModel doc = getSession().getDocument(ref);
            if (doc != null) {
                getTrashService().trashDocuments(Arrays.asList(doc));
                getPathCache().remove(doc.getPathAsString());
            } else {
                log.warn("Can't move document " + ref.toString() + " to trash. Document did not found.");
            }
        } catch (Exception e) {
            throw new ClientException("Error while deleting doc " + ref, e);
        }
    }

    public boolean isRename(String source, String destination) {
        Path sourcePath = new Path(source);
        Path destinationPath = new Path(destination);
        return sourcePath.removeLastSegments(1).toString().equals(destinationPath.removeLastSegments(1).toString());
    }

    public void renameItem(DocumentModel source, String destinationName) throws ClientException {
        if (source.isFolder()) {
            source.setPropertyValue("dc:title", destinationName);
            getSession().saveDocument(source);
            moveItem(source, source.getParentRef(), cleanName(destinationName));
        } else {
            source.setPropertyValue("dc:title", destinationName);
            BlobHolder bh = source.getAdapter(BlobHolder.class);
            boolean blobUpdated = false;
            if (bh != null) {
                Blob blob = bh.getBlob();
                if (blob != null) {
                    blob.setFilename(destinationName);
                    blobUpdated = true;
                    // XXXX should be done via blob holder !!!
                    if (source.hasSchema("file")) {
                        source.setProperty("file", "content", blob);
                        source.setProperty("file", "filename", destinationName);
                    }
                    getSession().saveDocument(source);
                }
            }
            if (!blobUpdated) {
                source.setPropertyValue("dc:title", destinationName);
                source = getSession().saveDocument(source);
                moveItem(source, source.getParentRef(), cleanName(destinationName));
            }
        }
    }

    public DocumentModel moveItem(DocumentModel source, PathRef targetParentRef) throws ClientException {
        return moveItem(source, targetParentRef, source.getName());
    }

    public DocumentModel moveItem(DocumentModel source, DocumentRef targetParentRef, String name) throws ClientException {
        try {
            cleanTrashPath(targetParentRef, name);
            DocumentModel model = getSession().move(source.getRef(), targetParentRef, name);
            getPathCache().put(parseLocation(targetParentRef.toString()) + "/" + name, model);
            getPathCache().remove(source.getPathAsString());
            return model;
        } catch (ClientException e) {
            discardChanges();
            throw new ClientException("Error while doing move", e);
        }
    }

    public DocumentModel copyItem(DocumentModel source, PathRef targetParentRef) throws ClientException {
        try {
            DocumentModel model = getSession().copy(source.getRef(), targetParentRef, source.getName());
            getPathCache().put(parseLocation(targetParentRef.toString()) + "/" + source.getName(), model);
            return model;
        } catch (ClientException e) {
            discardChanges();
            throw new ClientException("Error while doing move", e);
        }
    }

    public DocumentModel createFolder(String parentPath, String name) throws ClientException {
        DocumentModel parent = resolveLocation(parentPath);
        if (!parent.isFolder()) {
            throw new ClientException("Can not create a child in a non folderish node");
        }

        String targetType = "Folder";
        if ("WorkspaceRoot".equals(parent.getType())) {
            targetType = "Workspace";
        }
        name = cleanName(name);
        try {
            cleanTrashPath(parent, name);
            DocumentModel folder = getSession().createDocumentModel(parent.getPathAsString(), name,
                    targetType);
            folder.setPropertyValue("dc:title", name);
            folder = getSession().createDocument(folder);
            getPathCache().put(parseLocation(parentPath) + "/" + name, folder);
            return folder;
        } catch (Exception e) {
            discardChanges();
            throw new ClientException("Error child creating new folder", e);
        }
    }

    public DocumentModel createFile(String parentPath, String name, Blob content) throws ClientException {
        DocumentModel parent = resolveLocation(parentPath);
        if (!parent.isFolder()) {
            throw new ClientException("Can not create a child in a non folderish node");
        }

        String targetType = "File";
        name = cleanName(name);
        try {
            cleanTrashPath(parent, name);
            DocumentModel file = getSession().createDocumentModel(parent.getPathAsString(), name,
                    targetType);
            file.setPropertyValue("dc:title", name);
            if (content != null) {
                file.setProperty("file", "content", content);
                file.setProperty("file", "filename", name);
            }
            file = getSession().createDocument(file);
            getPathCache().put(parseLocation(parentPath) + "/" + name, file);
            return file;
        } catch (Exception e) {
            discardChanges();
            throw new ClientException("Error child creating new folder", e);
        }
    }

    public DocumentModel createFile(String parentPath, String name) throws ClientException {
        return createFile(parentPath, name, null);
    }

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

    public List<DocumentModel> getChildren(DocumentRef ref) throws ClientException {
        List<DocumentModel> result = new ArrayList<DocumentModel>();
        List<DocumentModel> children = getSession(true).getChildren(ref);
        for (DocumentModel child : children) {
            if (child.hasFacet(FacetNames.HIDDEN_IN_NAVIGATION)) {
                log.debug("Skipping hidden doc");
            } else if (LifeCycleConstants.DELETED_STATE.equals(child.getCurrentLifeCycleState())) {
                log.debug("Skipping deleted doc");
            } else if (!child.hasSchema("dublincore")) {
                log.debug("Skipping doc without dublincore schema");
            } else if (!child.hasSchema("file") && !child.hasFacet(FacetNames.FOLDERISH)) {
                log.debug("Skipping doc without file schema");
            } else {
                result.add(child);
            }
        }
        return result;
    }

    public boolean isLocked(DocumentRef ref) throws ClientException {
        String lock = getSession().getLock(ref);
        return StringUtils.isNotEmpty(lock);
    }

    public boolean canUnlock(DocumentRef ref) throws ClientException {
        Principal principal = getSession().getPrincipal();
        if (principal == null || StringUtils.isEmpty(principal.getName())) {
            log.error("Empty session principal. Error while canUnlock check.");
            return false;
        }
        String checkoutUser = getCheckoutUser(ref);
        return principal.getName().equals(checkoutUser);
    }

    public String lock(DocumentRef ref) throws ClientException {
        Principal principal = getSession().getPrincipal();
        if (principal == null || StringUtils.isEmpty(principal.getName())) {
            log.error("Empty session principal. Error while locking.");
            return "";
        }
        String lockDate = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date());
        String lockToken = principal.getName() + ":" + lockDate;
        getSession().setLock(ref, lockToken);
        saveChanges();
        return lockToken;
    }

    public boolean unlock(DocumentRef ref) throws ClientException {
        if (!canUnlock(ref)) {
            return false;
        }
        getSession().unlock(ref);
        saveChanges();
        return true;
    }

    public String getCheckoutUser(DocumentRef ref) throws ClientException {
        String existingLock = getSession().getLock(ref);
        if (existingLock != null) {
            String[] info = existingLock.split(":");
            return info[0];
        }
        return null;
    }

    public String getVirtualPath(String path){
        if (path.startsWith(this.rootPath)) {
            return rootUrl + path.substring(this.rootPath.length());
        } else {
            return null;
        }
    }

    protected String getFileName(DocumentModel doc) {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null) {
            try {
                Blob blob = bh.getBlob();
                if (blob != null) {
                    return blob.getFilename();
                }
            } catch (ClientException e) {
                log.error("Unable to get filename", e);
            }
        }
        return null;
    }

    protected boolean isTrashDocument(DocumentModel model) throws ClientException {
        if (model == null) {
            return true;
        } else if (LifeCycleConstants.DELETED_STATE.equals(model.getCurrentLifeCycleState())) {
            return true;
        } else {
            return false;
        }
    }

    private TrashService getTrashService() throws Exception {
        if (trashService == null) {
            trashService = Framework.getService(TrashService.class);
        }
        return trashService;
    }

    private boolean cleanTrashPath(DocumentModel parent, String name) throws ClientException {
        Path checkedPath = new Path(parent.getPathAsString()).append(name);
        if (getSession().exists(new PathRef(checkedPath.toString()))) {
            DocumentModel model = getSession().getDocument(new PathRef(checkedPath.toString()));
            if(model != null && LifeCycleConstants.DELETED_STATE.equals(model.getCurrentLifeCycleState())){
                name = name + "." + System.currentTimeMillis();
                getSession().move(model.getRef(), parent.getRef(), name);
                return true;
            }
        }
        return false;
    }

    private boolean cleanTrashPath(DocumentRef parentRef, String name) throws ClientException {
        DocumentModel parent = getSession().getDocument(parentRef);
        return cleanTrashPath(parent, name);
    }

}
