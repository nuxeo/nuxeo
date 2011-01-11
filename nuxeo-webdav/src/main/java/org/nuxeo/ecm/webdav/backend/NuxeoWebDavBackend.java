package org.nuxeo.ecm.webdav.backend;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.schema.FacetNames;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Principal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Based on org.nuxeo.ecm.platform.wss.backend.SimpleNuxeoBackend
 *
 * @author Organization: Gagnavarslan ehf
 */
public class NuxeoWebDavBackend extends AbstractCoreBackend implements WebDavBackend {

    private static final Log log = LogFactory.getLog(NuxeoWebDavBackend.class);

    protected String corePathPrefix;
    protected String urlRoot;

    public NuxeoWebDavBackend(String corePathPrefix, String urlRoot) {
        this.corePathPrefix = corePathPrefix;
        this.urlRoot = urlRoot;
    }

    public DocumentModel resolveLocation(String location) throws ClientException {
        Path strPath = new Path(corePathPrefix);
        location = cleanName(location);
        strPath = strPath.append(location);
        DocumentRef docRef = new PathRef(strPath.toString());
        DocumentModel doc = null;

        if (getSession().exists(docRef)) {
            doc = getSession().getDocument(docRef);
        } else {
            Path path = new Path(location);
            String parentSubPath = path.removeLastSegments(1).toString();
            Path parentPath = new Path(corePathPrefix);

            String filename = path.lastSegment();

            // first try with spaces (for create New Folder)
            String folderName = filename.replace(" ", "-");
            DocumentRef folderRef = new PathRef(parentPath.append(folderName).toString());
            if (getSession().exists(folderRef)) {
                return getSession().getDocument(folderRef);
            }
            // look for a child
            parentPath = parentPath.append(parentSubPath);
            docRef = new PathRef(parentPath.toString());
            if (!getSession().exists(docRef)) {
                throw new ClientException("Unable to find parent for item " + location);
            }
            List<DocumentModel> children = getSession().getChildren(docRef);
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
                    } else if (blob != null && encode(blob.getFilename().getBytes(), "ISO-8859-1").equals(filename)) {
                        doc = child;
                        break;
                    }
                }
            }
        }
        return doc;
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
            getSession().removeDocument(ref);
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
            getSession().move(source.getRef(), source.getParentRef(), cleanName(destinationName));
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
                getSession().move(source.getRef(), source.getParentRef(), cleanName(destinationName));
            }
        }
    }

    public void moveItem(DocumentModel source, PathRef targetParentRef) throws ClientException {
        try {
            getSession().move(source.getRef(), targetParentRef, source.getName());
        } catch (ClientException e) {
            discardChanges();
            throw new ClientException("Error while doing move", e);
        }
    }

    public void copyItem(DocumentModel source, PathRef targetParentRef) throws ClientException {
        try {
            getSession().copy(source.getRef(), targetParentRef, source.getName());
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
        String nodeName = cleanName(name);

        try {
            DocumentModel folder = getSession().createDocumentModel(parent.getPathAsString(), nodeName, targetType);
            folder.setPropertyValue("dc:title", name);
            folder = getSession().createDocument(folder);
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
        String nodeName = cleanName(name);
        try {
            DocumentModel file = getSession().createDocumentModel(parent.getPathAsString(), nodeName, targetType);
            file.setPropertyValue("dc:title", name);
            file.getProperty("file:content").setValue(content);
            file = getSession().createDocument(file);
            return file;
        } catch (Exception e) {
            discardChanges();
            throw new ClientException("Error child creating new folder", e);
        }
    }

    public String getDisplayName(DocumentModel doc) {
        if (doc.isFolder()) {
            return doc.getName();
        } else {
            String fileName = getFileName(doc);
            if (fileName==null) {
                fileName = doc.getName();
            }
            return fileName;
        }
    }

    public List<DocumentModel> getChildren(DocumentRef ref) throws ClientException {
        List<DocumentModel> result = new ArrayList<DocumentModel>();
        List<DocumentModel> children = getSession().getChildren(ref);
        for (DocumentModel child : children) {
            if (child.hasFacet(FacetNames.HIDDEN_IN_NAVIGATION)) {
                log.debug("Skipping hidden doc");
            } else if (LifeCycleConstants.DELETED_STATE.equals(child.getCurrentLifeCycleState())) {
                log.debug("Skipping deleted doc");
            } else if(child.hasSchema("dc")){
                log.debug("Skipping doc without dublincore schema");
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

        return principal.getName().equals(getCheckoutUser(ref));

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

    protected String cleanName(String name) {
        // XXX
        String s = name.replaceAll(" ", "-");
        /*s = s.replaceAll("[????]", "e");
        s = s.replaceAll("[??]", "u");
        s = s.replaceAll("[??]", "i");
        s = s.replaceAll("[??]", "a");
        s = s.replaceAll("?", "o");
        s = s.replaceAll("?", "c");
        s = s.replaceAll("[????]", "E");
        s = s.replaceAll("[??]", "U");
        s = s.replaceAll("[??]", "I");
        s = s.replaceAll("[??]", "A");
        s = s.replaceAll("?", "O");
        s = s.replaceAll("?", "C");*/
        return s;
    }

    public String encode(byte[] bytes, String encoding) throws ClientException {
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new ClientException("Unsupported encoding " + encoding);
        }
    }

    protected String getFileName(DocumentModel doc) {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null) {
            try {
                Blob blob = bh.getBlob();
                if (blob!=null) {
                    return blob.getFilename();
                }
            } catch (ClientException e) {
                log.error("Unable to get filename", e);
            }
        }
        return null;
    }

}
