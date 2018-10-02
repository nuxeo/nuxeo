/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 *     Thomas Roger
 */

package org.nuxeo.wopi.jaxrs;

import static org.nuxeo.ecm.core.api.CoreSession.SOURCE;
import static org.nuxeo.wopi.Constants.ACCESS_TOKEN;
import static org.nuxeo.wopi.Constants.ACTION_EDIT;
import static org.nuxeo.wopi.Constants.ACTION_VIEW;
import static org.nuxeo.wopi.Constants.BASE_FILE_NAME;
import static org.nuxeo.wopi.Constants.BREADCRUMB_BRAND_NAME;
import static org.nuxeo.wopi.Constants.BREADCRUMB_BRAND_URL;
import static org.nuxeo.wopi.Constants.BREADCRUMB_FOLDER_NAME;
import static org.nuxeo.wopi.Constants.BREADCRUMB_FOLDER_URL;
import static org.nuxeo.wopi.Constants.CLOSE_URL;
import static org.nuxeo.wopi.Constants.DOWNLOAD_URL;
import static org.nuxeo.wopi.Constants.FILES_ENDPOINT_PATH;
import static org.nuxeo.wopi.Constants.FILE_VERSION_URL;
import static org.nuxeo.wopi.Constants.HOST_EDIT_URL;
import static org.nuxeo.wopi.Constants.HOST_VIEW_URL;
import static org.nuxeo.wopi.Constants.IS_ANONYMOUS_USER;
import static org.nuxeo.wopi.Constants.NAME;
import static org.nuxeo.wopi.Constants.NOTIFICATION_DOCUMENT_ID_CODEC_NAME;
import static org.nuxeo.wopi.Constants.OWNER_ID;
import static org.nuxeo.wopi.Constants.READ_ONLY;
import static org.nuxeo.wopi.Constants.SHARE_URL;
import static org.nuxeo.wopi.Constants.SHARE_URL_READ_ONLY;
import static org.nuxeo.wopi.Constants.SHARE_URL_READ_WRITE;
import static org.nuxeo.wopi.Constants.SIGNOUT_URL;
import static org.nuxeo.wopi.Constants.SIZE;
import static org.nuxeo.wopi.Constants.SUPPORTED_SHARE_URL_TYPES;
import static org.nuxeo.wopi.Constants.SUPPORTS_DELETE_FILE;
import static org.nuxeo.wopi.Constants.SUPPORTS_EXTENDED_LOCK_LENGTH;
import static org.nuxeo.wopi.Constants.SUPPORTS_LOCKS;
import static org.nuxeo.wopi.Constants.SUPPORTS_RENAME;
import static org.nuxeo.wopi.Constants.SUPPORTS_UPDATE;
import static org.nuxeo.wopi.Constants.URL;
import static org.nuxeo.wopi.Constants.USER_CAN_NOT_WRITE_RELATIVE;
import static org.nuxeo.wopi.Constants.USER_CAN_RENAME;
import static org.nuxeo.wopi.Constants.USER_CAN_WRITE;
import static org.nuxeo.wopi.Constants.USER_FRIENDLY_NAME;
import static org.nuxeo.wopi.Constants.USER_ID;
import static org.nuxeo.wopi.Constants.VERSION;
import static org.nuxeo.wopi.Constants.WOPI_SOURCE;
import static org.nuxeo.wopi.Headers.ITEM_VERSION;
import static org.nuxeo.wopi.Headers.LOCK;
import static org.nuxeo.wopi.Headers.MAX_EXPECTED_SIZE;
import static org.nuxeo.wopi.Headers.OLD_LOCK;
import static org.nuxeo.wopi.Headers.OVERRIDE;
import static org.nuxeo.wopi.Headers.RELATIVE_TARGET;
import static org.nuxeo.wopi.Headers.REQUESTED_NAME;
import static org.nuxeo.wopi.Headers.SUGGESTED_TARGET;
import static org.nuxeo.wopi.Headers.URL_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.wopi.FileInfo;
import org.nuxeo.wopi.Helpers;
import org.nuxeo.wopi.Operation;
import org.nuxeo.wopi.exception.BadRequestException;
import org.nuxeo.wopi.exception.ConflictException;
import org.nuxeo.wopi.exception.NotImplementedException;
import org.nuxeo.wopi.exception.PreConditionFailedException;
import org.nuxeo.wopi.lock.LockHelper;

/**
 * Implementation of the Files endpoint.
 * <p>
 * See <a href="https://wopirest.readthedocs.io/en/latest/endpoints.html#files-endpoint"></a>.
 *
 * @since 10.3
 */
@WebObject(type = "wopiFiles")
public class FilesEndpoint extends DefaultObject {

    @Context
    protected HttpServletRequest request;

    @Context
    protected HttpServletResponse response;

    @Context
    protected HttpHeaders httpHeaders;

    protected CoreSession session;

    protected DocumentModel doc;

    protected Blob blob;

    protected String xpath;

    protected String fileId;

    protected String baseURL;

    @Override
    public void initialize(Object... args) {
        if (args == null || args.length != 4) {
            throw new IllegalArgumentException("Invalid args: " + args);
        }
        session = (CoreSession) args[0];
        doc = (DocumentModel) args[1];
        blob = (Blob) args[2];
        xpath = (String) args[3];
        fileId = FileInfo.computeFileId(doc, xpath);
        baseURL = VirtualHostHelper.getBaseURL(request);
    }

    /**
     * Implements the CheckFileInfo operation.
     * <p>
     * See <a href="https://wopirest.readthedocs.io/en/latest/files/CheckFileInfo.html"></a>.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Object checkFileInfo() {
        return buildCheckFileInfoMap();
    }

    /**
     * Implements the GetFile operation.
     * <p>
     * See <a href="https://wopi.readthedocs.io/projects/wopirest/en/latest/files/GetFile.html"></a>.
     */
    @GET
    @Path("contents")
    public Object getFile(@HeaderParam(MAX_EXPECTED_SIZE) String maxExpectedSizeHeader) {
        int maxExpectedSize = getMaxExpectedSize(maxExpectedSizeHeader);
        if (blob.getLength() > maxExpectedSize) {
            throw new PreConditionFailedException();
        }

        response.addHeader(ITEM_VERSION, doc.getVersionLabel());
        return blob;
    }

    @POST
    public Object doPost(@HeaderParam(OVERRIDE) Operation operation) {
        switch (operation) {
        case DELETE:
            return deleteFile();
        case GET_LOCK:
            return getLock();
        case GET_SHARE_URL:
            return getShareUrl();
        case LOCK:
            return lock();
        case PUT_RELATIVE:
            return putRelativeFile();
        case REFRESH_LOCK:
            return refreshLock();
        case RENAME_FILE:
            return renameFile();
        case UNLOCK:
            return unlock();
        default:
            throw new BadRequestException();
        }
    }

    /**
     * Implements the Lock operation.
     * <p>
     * See <a href="https://wopirest.readthedocs.io/en/latest/files/Lock.html"></a>.
     */
    protected Object lock() {
        String lock = getHeader(LOCK);
        String oldLock = getHeader(OLD_LOCK, true);

        boolean isLocked = doc.isLocked();
        // document not locked or locked with another WOPI lock
        if (!isLocked || LockHelper.hasOtherLock(fileId)) {
            if (!StringUtils.isEmpty(oldLock)) {
                // cannot unlock and relock
                response.addHeader(LOCK, "");
                throw new ConflictException();
            }

            checkWritePropertiesPermission();
            // lock if needed
            if (!isLocked) {
                doc.setLock();
            }
            LockHelper.addLock(fileId, lock);

            response.addHeader(ITEM_VERSION, doc.getVersionLabel());
            return Response.ok().build();
        }

        String currentLock = getCurrentLock();
        if (StringUtils.isEmpty(oldLock)) {
            if (lock.equals(currentLock)) {
                // refresh lock
                LockHelper.refreshLock(fileId);
                response.addHeader(ITEM_VERSION, doc.getVersionLabel());
                return Response.ok().build();
            }
        } else {
            if (oldLock.equals(currentLock)) {
                // unlock and relock
                LockHelper.updateLock(fileId, lock);
                return Response.ok().build();
            }
        }

        return buildConflictResponse(currentLock);
    }

    /**
     * Returns the WOPI lock if not null and throws a {@link ConflictException} otherwise.
     * <p>
     * Must be called to check that a locked document is not locked by Nuxeo.
     */
    protected String getCurrentLock() {
        String currentLock = LockHelper.getLock(fileId);
        if (currentLock == null) {
            // locked by Nuxeo
            throw new ConflictException();
        }
        return currentLock;
    }

    /**
     * Builds a conflict response with the WOPI lock as a header.
     * <p>
     * Must be called to check that a document is locked by another WOPI client.
     */
    protected Response buildConflictResponse(String currentLock) {
        // locked by another WOPI client
        response.addHeader(LOCK, currentLock);
        return Response.status(Response.Status.CONFLICT).build();
    }

    /**
     * Implements the GetLock operation.
     * <p>
     * See <a href="https://wopi.readthedocs.io/projects/wopirest/en/latest/files/GetLock.html"></a>.
     */
    protected Object getLock() {
        if (!doc.isLocked()) {
            response.addHeader(LOCK, "");
            return Response.ok().build();
        }

        String currentLock = getCurrentLock();
        response.addHeader(LOCK, currentLock);
        return Response.ok().build();
    }

    protected Object unlockOrRefresh(String lock, boolean unlock) {
        boolean isLocked = doc.isLocked();
        if (!isLocked) {
            // not locked
            response.addHeader(LOCK, "");
            throw new ConflictException();
        }

        String currentLock = getCurrentLock();
        if (lock.equals(currentLock)) {
            checkWritePropertiesPermission();
            if (unlock) {
                // remove WOPI lock
                LockHelper.removeLock(fileId);
                if (!LockHelper.isLocked(doc.getRepositoryName(), doc.getId())) {
                    // no more WOPI lock on the document, unlock the doc
                    // use a privileged session since the document might have been locked by another user
                    CoreInstance.doPrivileged(doc.getRepositoryName(), privilegedSession -> { // NOSONAR
                        return privilegedSession.removeLock(doc.getRef());
                    });
                }
                response.addHeader(ITEM_VERSION, doc.getVersionLabel());
            } else {
                // refresh lock
                LockHelper.refreshLock(fileId);
            }
            return Response.ok().build();
        }

        return buildConflictResponse(currentLock);
    }

    /**
     * Implements the PutRelativeFile operation.
     * <p>
     * We do not handle any conflict or overwrite here. Nuxeo can have more than one document with the same title and
     * blob file name.
     * <p>
     * See <a href="https://wopi.readthedocs.io/projects/wopirest/en/latest/files/PutRelativeFile.html"></a>.
     */
    public Object putRelativeFile() {
        String suggestedTarget = getHeader(SUGGESTED_TARGET, true);
        if (suggestedTarget != null) {
            suggestedTarget = Helpers.readUTF7String(suggestedTarget);
        }
        String relativeTarget = getHeader(RELATIVE_TARGET, true);
        if (relativeTarget != null) {
            relativeTarget = Helpers.readUTF7String(relativeTarget);
        }

        // exactly one should be empty
        if (StringUtils.isEmpty(suggestedTarget) == StringUtils.isEmpty(relativeTarget)) {
            throw new NotImplementedException();
        }

        DocumentRef parentRef = doc.getParentRef();
        if (!session.exists(parentRef) || !session.hasPermission(parentRef, SecurityConstants.ADD_CHILDREN)) {
            throw new NotImplementedException();
        }

        String newFileName = relativeTarget;
        if (StringUtils.isNotEmpty(suggestedTarget)) {
            newFileName = suggestedTarget.startsWith(".")
                    ? FilenameUtils.getBaseName(blob.getFilename()) + suggestedTarget
                    : suggestedTarget;
        }

        DocumentModel parent = session.getDocument(parentRef);

        DocumentModel newDoc = session.createDocumentModel(parent.getPathAsString(), newFileName, doc.getType());
        newDoc.copyContent(doc);
        newDoc.setPropertyValue("dc:title", newFileName);

        Blob newBlob = createBlobFromRequestBody(newFileName, null);
        newDoc.setPropertyValue(xpath, (Serializable) newBlob);
        newDoc = session.createDocument(newDoc);

        String token = Helpers.createJWTToken();
        String newFileId = FileInfo.computeFileId(newDoc, xpath);
        String wopiSrc = String.format("%s%s%s?%s=%s", baseURL, FILES_ENDPOINT_PATH, newFileId, ACCESS_TOKEN, token);
        String hostViewUrl = Helpers.getWOPIURL(baseURL, ACTION_VIEW, newDoc, xpath);
        String hostEditUrl = Helpers.getWOPIURL(baseURL, ACTION_EDIT, newDoc, xpath);

        Map<String, Serializable> map = new HashMap<>();
        map.put(NAME, newFileName);
        map.put(URL, wopiSrc);
        map.put(HOST_VIEW_URL, hostViewUrl);
        map.put(HOST_EDIT_URL, hostEditUrl);
        return Response.ok(map).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * Implements the RenameFile operation.
     * <p>
     * See <a href="https://wopi.readthedocs.io/projects/wopirest/en/latest/files/RenameFile.html"></a>.
     */
    @Produces(MediaType.APPLICATION_JSON)
    public Object renameFile() {
        checkWritePropertiesPermission();

        String requestedName = Helpers.readUTF7String(getHeader(REQUESTED_NAME));
        if (!doc.isLocked()) {
            return renameBlob(requestedName);
        }

        String currentLock = getCurrentLock();
        String lock = getHeader(LOCK);
        if (lock.equals(currentLock)) {
            return renameBlob(requestedName);
        }

        return buildConflictResponse(currentLock);
    }

    /**
     * Renames the blob with the {@code requestedName}.
     *
     * @return the expected JSON response for the RenameFile operation.
     */
    protected Response renameBlob(String requestedName) {
        String extension = FilenameUtils.getExtension(blob.getFilename());
        String fullFilename = requestedName + (extension != null ? "." + extension : "");
        blob.setFilename(fullFilename);
        doc.setPropertyValue(xpath, (Serializable) blob);
        doc.putContextData(SOURCE, WOPI_SOURCE);
        session.saveDocument(doc);

        Map<String, Serializable> map = new HashMap<>();
        map.put(NAME, requestedName);
        return Response.ok(map).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * Implements the DeleteFile operation.
     * <p>
     * See <a href="https://wopi.readthedocs.io/projects/wopirest/en/latest/files/DeleteFile.html"></a>.
     */
    public Object deleteFile() {
        if (doc.isLocked()) {
            String currentLock = getCurrentLock();
            return buildConflictResponse(currentLock);
        }

        if (!session.hasPermission(doc.getRef(), SecurityConstants.REMOVE)) {
            // cannot delete
            throw new ConflictException();
        }

        session.removeDocument(doc.getRef());
        return Response.ok().build();
    }

    /**
     * Implements the GetShareUrl operation.
     * <p>
     * See <a href="https://wopi.readthedocs.io/projects/wopirest/en/latest/files/GetShareUrl.html"></a>.
     */
    @Produces(MediaType.APPLICATION_JSON)
    public Object getShareUrl() {
        String urlType = getHeader(URL_TYPE, true);
        if (!SHARE_URL_READ_ONLY.equals(urlType) && !SHARE_URL_READ_WRITE.equals(urlType)) {
            throw new NotImplementedException();
        }

        String shareURL = Helpers.getWOPIURL(baseURL, urlType.equals(SHARE_URL_READ_ONLY) ? ACTION_VIEW : ACTION_EDIT,
                doc, xpath);
        Map<String, Serializable> map = new HashMap<>();
        map.put(SHARE_URL, shareURL);
        return Response.ok(map).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @POST
    @Path("contents")
    public Object doPostContents(@HeaderParam(OVERRIDE) Operation operation) {
        if (Operation.PUT.equals(operation)) {
            return putFile();
        }
        throw new BadRequestException();
    }

    /**
     * Implements the PutFile operation.
     * <p>
     * See <a href="https://wopi.readthedocs.io/projects/wopirest/en/latest/files/PutFile.html"></a>.
     */
    public Object putFile() {
        checkWritePropertiesPermission();

        if (!doc.isLocked()) {
            if (blob.getLength() == 0) {
                return updateBlob();
            }
            response.addHeader(LOCK, "");
            throw new ConflictException();
        }

        String currentLock = getCurrentLock();
        String lock = getHeader(LOCK);
        if (lock.equals(currentLock)) {
            return updateBlob();
        }

        return buildConflictResponse(currentLock);
    }

    /**
     * Updates the document's blob from a new one.
     *
     * @return the expected response for the PutFile operation, with the 'X-WOPI-ItemVersion' header set.
     */
    protected Response updateBlob() {
        Blob newBlob = createBlobFromRequestBody(blob.getFilename(), blob.getMimeType());
        doc.setPropertyValue(xpath, (Serializable) newBlob);
        doc.putContextData(SOURCE, WOPI_SOURCE);
        doc = session.saveDocument(doc);
        response.addHeader(ITEM_VERSION, doc.getVersionLabel());
        return Response.ok().build();
    }

    /**
     * Creates a new blob from the request body, given a {@code filename} and an optional {@code mimeType}.
     *
     * @return the new blob
     */
    protected Blob createBlobFromRequestBody(String filename, String mimeType) {
        try (InputStream is = request.getInputStream()) {
            Blob newBlob = Blobs.createBlob(is);
            newBlob.setFilename(filename);
            newBlob.setMimeType(mimeType);
            return newBlob;
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    /**
     * Implements the Unlock operation.
     * <p>
     * See <a href="https://wopirest.readthedocs.io/en/latest/files/Unlock.html"></a>.
     */
    protected Object unlock() {
        String lock = getHeader(LOCK);
        return unlockOrRefresh(lock, true);
    }

    /**
     * Implements the RefreshLock operation.
     * <p>
     * See <a href="https://wopirest.readthedocs.io/en/latest/files/RefreshLock.html"></a>.
     */
    protected Object refreshLock() {
        String lock = getHeader(LOCK);
        return unlockOrRefresh(lock, false);
    }

    protected int getMaxExpectedSize(String maxExpectedSizeHeader) {
        if (!StringUtils.isEmpty(maxExpectedSizeHeader)) {
            try {
                return Integer.parseInt(maxExpectedSizeHeader, 10);
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        return Integer.MAX_VALUE;
    }

    protected String getHeader(String headerName) {
        return getHeader(headerName, false);
    }

    protected String getHeader(String headerName, boolean nullable) {
        List<String> headers = httpHeaders.getRequestHeader(headerName);
        String header = headers == null || headers.isEmpty() ? null : headers.get(0);
        if (StringUtils.isEmpty(header) && !nullable) {
            throw new BadRequestException();
        }
        return header;
    }

    protected void checkWritePropertiesPermission() {
        if (!session.hasPermission(doc.getRef(), SecurityConstants.WRITE_PROPERTIES)) {
            // cannot rename blob
            throw new ConflictException();
        }
    }

    protected Map<String, Serializable> buildCheckFileInfoMap() {
        Map<String, Serializable> map = new HashMap<>();
        addRequiredProperties(map);
        addHostCapabilitiesProperties(map);
        addUserMetadataProperties(map);
        addUserPermissionsProperties(map);
        addFileURLProperties(map);
        addBreadcrumbProperties(map);
        return map;
    }

    protected void addRequiredProperties(Map<String, Serializable> map) {
        NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
        map.put(BASE_FILE_NAME, blob.getFilename());
        map.put(OWNER_ID, doc.getPropertyValue("dc:creator"));
        map.put(SIZE, blob.getLength());
        map.put(USER_ID, principal.getName());
        map.put(VERSION, doc.getVersionLabel());
    }

    protected void addHostCapabilitiesProperties(Map<String, Serializable> map) {
        map.put(SUPPORTS_EXTENDED_LOCK_LENGTH, true);
        map.put(SUPPORTS_LOCKS, true);
        map.put(SUPPORTS_RENAME, true);
        map.put(SUPPORTS_UPDATE, true);
        map.put(SUPPORTS_DELETE_FILE, true);
        map.put(SUPPORTED_SHARE_URL_TYPES, new String[] { SHARE_URL_READ_ONLY, SHARE_URL_READ_WRITE });
    }

    protected void addUserMetadataProperties(Map<String, Serializable> map) {
        NuxeoPrincipal principal = (NuxeoPrincipal) session.getPrincipal();
        map.put(IS_ANONYMOUS_USER, principal.isAnonymous());
        map.put(USER_FRIENDLY_NAME, Helpers.principalFullName(principal));
    }

    protected void addUserPermissionsProperties(Map<String, Serializable> map) {
        boolean hasAddChildren = session.exists(doc.getParentRef())
                && session.hasPermission(doc.getParentRef(), SecurityConstants.ADD_CHILDREN);
        boolean hasWriteProperties = session.hasPermission(doc.getRef(), SecurityConstants.WRITE_PROPERTIES);
        map.put(READ_ONLY, !hasWriteProperties);
        map.put(USER_CAN_RENAME, hasWriteProperties);
        map.put(USER_CAN_WRITE, hasWriteProperties);
        map.put(USER_CAN_NOT_WRITE_RELATIVE, !hasAddChildren);
    }

    protected void addFileURLProperties(Map<String, Serializable> map) {
        String docURL = getDocumentURL(doc);
        if (docURL != null) {
            map.put(CLOSE_URL, docURL);
            map.put(FILE_VERSION_URL, docURL);
        }
        String downloadURL = baseURL
                + Framework.getService(DownloadService.class).getDownloadUrl(doc, xpath, blob.getFilename());
        map.put(DOWNLOAD_URL, downloadURL);
        map.put(HOST_EDIT_URL, Helpers.getWOPIURL(baseURL, ACTION_EDIT, doc, xpath));
        map.put(HOST_VIEW_URL, Helpers.getWOPIURL(baseURL, ACTION_VIEW, doc, xpath));
        String signoutURL = baseURL + NXAuthConstants.LOGOUT_PAGE;
        map.put(SIGNOUT_URL, signoutURL);
    }

    protected void addBreadcrumbProperties(Map<String, Serializable> map) {
        map.put(BREADCRUMB_BRAND_NAME, Framework.getProperty(Environment.PRODUCT_NAME));
        map.put(BREADCRUMB_BRAND_URL, baseURL);

        DocumentRef parentRef = doc.getParentRef();
        if (session.exists(parentRef)) {
            DocumentModel parent = session.getDocument(parentRef);
            map.put(BREADCRUMB_FOLDER_NAME, parent.getTitle());
            String url = getDocumentURL(parent);
            if (url != null) {
                map.put(BREADCRUMB_FOLDER_URL, url);
            }
        }
    }

    protected String getDocumentURL(DocumentModel doc) {
        TypeInfo adapter = doc.getAdapter(TypeInfo.class);
        if (adapter != null) {
            DocumentLocation docLoc = new DocumentLocationImpl(doc);
            DocumentView docView = new DocumentViewImpl(docLoc, adapter.getDefaultView());
            return Framework.getService(DocumentViewCodecManager.class)
                            .getUrlFromDocumentView(NOTIFICATION_DOCUMENT_ID_CODEC_NAME, docView, true, baseURL);
        }
        return null;
    }

}
