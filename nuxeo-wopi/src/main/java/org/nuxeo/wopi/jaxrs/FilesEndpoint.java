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

import static javax.servlet.http.HttpServletResponse.SC_NOT_IMPLEMENTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.nuxeo.ecm.core.api.CoreSession.SOURCE;
import static org.nuxeo.wopi.Constants.ACCESS_TOKEN_PARAMETER;
import static org.nuxeo.wopi.Constants.ACTION_CONVERT;
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
import static org.nuxeo.wopi.Constants.LICENSE_CHECK_FOR_EDIT_IS_ENABLED;
import static org.nuxeo.wopi.Constants.NAME;
import static org.nuxeo.wopi.Constants.NOTIFICATION_DOCUMENT_ID_CODEC_NAME;
import static org.nuxeo.wopi.Constants.OPERATION_CHECK_FILE_INFO;
import static org.nuxeo.wopi.Constants.OPERATION_GET_FILE;
import static org.nuxeo.wopi.Constants.OPERATION_GET_LOCK;
import static org.nuxeo.wopi.Constants.OPERATION_GET_SHARE_URL;
import static org.nuxeo.wopi.Constants.OPERATION_LOCK;
import static org.nuxeo.wopi.Constants.OPERATION_PUT_FILE;
import static org.nuxeo.wopi.Constants.OPERATION_PUT_RELATIVE_FILE;
import static org.nuxeo.wopi.Constants.OPERATION_REFRESH_LOCK;
import static org.nuxeo.wopi.Constants.OPERATION_RENAME_FILE;
import static org.nuxeo.wopi.Constants.OPERATION_UNLOCK;
import static org.nuxeo.wopi.Constants.OPERATION_UNLOCK_AND_RELOCK;
import static org.nuxeo.wopi.Constants.OWNER_ID;
import static org.nuxeo.wopi.Constants.READ_ONLY;
import static org.nuxeo.wopi.Constants.SHARE_URL;
import static org.nuxeo.wopi.Constants.SHARE_URL_READ_ONLY;
import static org.nuxeo.wopi.Constants.SHARE_URL_READ_WRITE;
import static org.nuxeo.wopi.Constants.SIGNOUT_URL;
import static org.nuxeo.wopi.Constants.SIZE;
import static org.nuxeo.wopi.Constants.SUPPORTED_SHARE_URL_TYPES;
import static org.nuxeo.wopi.Constants.SUPPORTS_EXTENDED_LOCK_LENGTH;
import static org.nuxeo.wopi.Constants.SUPPORTS_GET_LOCK;
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
import static org.nuxeo.wopi.Constants.WOPI_BASE_URL_PROPERTY;
import static org.nuxeo.wopi.Constants.WOPI_SOURCE;
import static org.nuxeo.wopi.Headers.FILE_CONVERSION;
import static org.nuxeo.wopi.Headers.ITEM_VERSION;
import static org.nuxeo.wopi.Headers.LOCK;
import static org.nuxeo.wopi.Headers.MAX_EXPECTED_SIZE;
import static org.nuxeo.wopi.Headers.OLD_LOCK;
import static org.nuxeo.wopi.Headers.OVERRIDE;
import static org.nuxeo.wopi.Headers.RELATIVE_TARGET;
import static org.nuxeo.wopi.Headers.REQUESTED_NAME;
import static org.nuxeo.wopi.Headers.SUGGESTED_TARGET;
import static org.nuxeo.wopi.Headers.URL_TYPE;
import static org.nuxeo.wopi.Operation.PUT;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.nuxeo.wopi.WOPIService;
import org.nuxeo.wopi.exception.BadRequestException;
import org.nuxeo.wopi.exception.ConflictException;
import org.nuxeo.wopi.exception.NotImplementedException;
import org.nuxeo.wopi.exception.PreConditionFailedException;
import org.nuxeo.wopi.lock.LockHelper;

/**
 * Implementation of the Files endpoint.
 * <p>
 * See <a href="https://wopirest.readthedocs.io/en/latest/endpoints.html#files-endpoint">Files endpoint</a>.
 *
 * @since 10.3
 */
@WebObject(type = "wopiFiles")
public class FilesEndpoint extends DefaultObject {

    private static final Logger log = LogManager.getLogger(FilesEndpoint.class);

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

    protected String wopiBaseURL;

    @Override
    public void initialize(Object... args) {
        if (args == null || args.length != 4) {
            throw new IllegalArgumentException("Invalid args: " + Arrays.toString(args));
        }
        session = (CoreSession) args[0];
        doc = (DocumentModel) args[1];
        blob = (Blob) args[2];
        xpath = (String) args[3];
        fileId = FileInfo.computeFileId(doc, xpath);
        baseURL = VirtualHostHelper.getBaseURL(request);
        wopiBaseURL = Framework.getProperty(WOPI_BASE_URL_PROPERTY, baseURL);
    }

    /**
     * Implements the CheckFileInfo operation.
     * <p>
     * See <a href="https://wopirest.readthedocs.io/en/latest/files/CheckFileInfo.html">CheckFileInfo</a>.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkFileInfo() {
        logRequest(OPERATION_CHECK_FILE_INFO);
        Map<String, Serializable> checkFileInfoMap = buildCheckFileInfoMap();
        logResponse(OPERATION_CHECK_FILE_INFO, OK.getStatusCode(), checkFileInfoMap);
        return Response.ok(checkFileInfoMap).build();
    }

    /**
     * Implements the GetFile operation.
     * <p>
     * See <a href="https://wopi.readthedocs.io/projects/wopirest/en/latest/files/GetFile.html">GetFile</a>.
     */
    @GET
    @Path("contents")
    public Object getFile(@HeaderParam(MAX_EXPECTED_SIZE) String maxExpectedSizeHeader) {
        int maxExpectedSize = getMaxExpectedSize(maxExpectedSizeHeader);
        logRequest(OPERATION_GET_FILE, MAX_EXPECTED_SIZE, maxExpectedSizeHeader);

        long blobLength = blob.getLength();
        if (blobLength > maxExpectedSize) {
            logCondition(() -> "Blob length " + blobLength + " > max expected size " + maxExpectedSize);
            logResponse(OPERATION_GET_FILE, PRECONDITION_FAILED.getStatusCode());
            throw new PreConditionFailedException();
        }

        // ignore the return value as we need to return the blob anyway
        buildItemVersionResponse(OPERATION_GET_FILE, blob);
        return blob;
    }

    @POST
    public Object doPost(@HeaderParam(OVERRIDE) Operation operation) {
        switch (operation) {
        case GET_LOCK:
            return getLock();
        case GET_SHARE_URL:
            return getShareUrl();
        case LOCK:
            return lockOrUnlockAndRelock();
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
     * Implements the Lock and UnlockAndRelock operations.
     * <p>
     * See <a href="https://wopirest.readthedocs.io/en/latest/files/Lock.html">Lock</a> and
     * <a href="https://wopi.readthedocs.io/projects/wopirest/en/latest/files/UnlockAndRelock.html">UnlockAndRelock</a>.
     */
    protected Object lockOrUnlockAndRelock() {
        String lock = getHeader(OPERATION_LOCK, LOCK);
        String oldLock = getHeader(OPERATION_LOCK, OLD_LOCK, true);
        return StringUtils.isEmpty(oldLock) ? lock(lock) : unlockAndRelock(lock, oldLock);
    }

    protected Object lock(String lock) {
        logRequest(OPERATION_LOCK, LOCK, lock);
        boolean isLocked = doc.isLocked();
        // document not locked or no WOPI lock for this file id
        if (!isLocked || !LockHelper.isLocked(fileId)) {
            logCondition("Document isn't locked or there is no WOPI lock for this file id");
            checkWritePropertiesPermission(OPERATION_LOCK);
            // lock if needed
            if (!isLocked) {
                logCondition("Document isn't locked"); // NOSONAR
                logNuxeoAction("Locking document");
                doc.setLock();
            }
            LockHelper.addLock(fileId, lock);
            return buildItemVersionResponse(OPERATION_LOCK, blob);
        }

        logCondition("Document is locked and there is a WOPI lock for this file id");
        String currentLock = getCurrentLock(OPERATION_LOCK);
        if (lock.equals(currentLock)) {
            logCondition(() -> LOCK + " header is equal to current WOPI lock"); // NOSONAR
            // refresh lock
            LockHelper.refreshLock(fileId);
            return buildItemVersionResponse(OPERATION_LOCK, blob);
        } else {
            logCondition(() -> LOCK + " header is not equal to current WOPI lock"); // NOSONAR
            return buildConflictResponse(OPERATION_LOCK, currentLock);
        }
    }

    protected Response buildItemVersionResponse(String operation, Blob blob) {
        String itemVersion = blob.getDigest();
        response.addHeader(ITEM_VERSION, itemVersion);
        logResponse(operation, OK.getStatusCode(), ITEM_VERSION, itemVersion);
        return Response.ok().build();
    }

    protected Object unlockAndRelock(String lock, String oldLock) {
        logRequest(OPERATION_UNLOCK_AND_RELOCK, LOCK, lock, OLD_LOCK, oldLock);
        boolean isLocked = doc.isLocked();
        // document not locked
        if (!isLocked) {
            logCondition("Document isn't locked");
            // cannot unlock and relock
            buildConflictResponse(OPERATION_UNLOCK_AND_RELOCK, "");
        }

        logCondition("Document is locked");
        String currentLock = getCurrentLock(OPERATION_UNLOCK_AND_RELOCK);
        if (oldLock.equals(currentLock)) {
            logCondition(() -> OLD_LOCK + " header is equal to current WOPI lock");
            // unlock and relock
            LockHelper.updateLock(fileId, lock);
            logResponse(OPERATION_UNLOCK_AND_RELOCK, OK.getStatusCode());
            return Response.ok().build();
        } else {
            logCondition(() -> OLD_LOCK + " header is not equal to current WOPI lock");
            return buildConflictResponse(OPERATION_UNLOCK_AND_RELOCK, currentLock);
        }
    }

    /**
     * Returns the WOPI lock if not null and throws a {@link ConflictException} otherwise.
     * <p>
     * Must be called to check that a locked document is not locked by Nuxeo.
     */
    protected String getCurrentLock(String operation) {
        String currentLock = LockHelper.getLock(fileId);
        if (currentLock == null) {
            logCondition("Current WOPI lock not found");
            // locked by Nuxeo
            logResponse(operation, CONFLICT.getStatusCode());
            throw new ConflictException();
        }
        return currentLock;
    }

    /**
     * Builds a conflict response with the given WOPI lock as a header.
     * <p>
     * Must be called in case of "lock mismatch", for instance when a document is locked by another WOPI client.
     */
    protected Response buildConflictResponse(String operation, String lock) {
        response.addHeader(LOCK, lock);
        logResponse(operation, CONFLICT.getStatusCode(), LOCK, lock);
        return Response.status(CONFLICT).build();
    }

    /**
     * Implements the GetLock operation.
     * <p>
     * See <a href="https://wopi.readthedocs.io/projects/wopirest/en/latest/files/GetLock.html">GetLock</a>.
     */
    protected Object getLock() {
        logRequest(OPERATION_GET_LOCK);

        if (!doc.isLocked()) {
            logCondition("Document isn't locked");
            String lockHeader = "";
            response.addHeader(LOCK, lockHeader);
            logResponse(OPERATION_GET_LOCK, OK.getStatusCode(), LOCK, lockHeader);
            return Response.ok().build();
        }

        String currentLock = getCurrentLock(OPERATION_GET_LOCK);
        response.addHeader(LOCK, currentLock);
        logResponse(OPERATION_GET_LOCK, OK.getStatusCode(), LOCK, currentLock);
        return Response.ok().build();
    }

    protected Object unlockOrRefresh(String operation, String lock, boolean unlock) {
        if (!doc.isLocked()) {
            logCondition("Document isn't locked");
            // not locked
            buildConflictResponse(operation, "");
        }

        String currentLock = getCurrentLock(operation);
        if (lock.equals(currentLock)) {
            logCondition(() -> LOCK + " header is equal to current WOPI lock");
            checkWritePropertiesPermission(operation);
            if (unlock) {
                // remove WOPI lock
                LockHelper.removeLock(fileId);
                if (!LockHelper.isLocked(doc.getRepositoryName(), doc.getId())) {
                    logCondition("Found no WOPI lock");
                    // no more WOPI lock on the document, unlock the doc
                    // use a privileged session since the document might have been locked by another user
                    logNuxeoAction("Unlocking document with a privileged session");
                    CoreInstance.doPrivileged(doc.getRepositoryName(), privilegedSession -> { // NOSONAR
                        return privilegedSession.removeLock(doc.getRef());
                    });
                }
                return buildItemVersionResponse(operation, blob);
            } else {
                // refresh lock
                LockHelper.refreshLock(fileId);
                logResponse(operation, OK.getStatusCode());
            }
            return Response.ok().build();
        } else {
            logCondition(() -> LOCK + " header is not equal to current WOPI lock");
            return buildConflictResponse(operation, currentLock);
        }
    }

    /**
     * Implements the PutRelativeFile operation.
     * <p>
     * New file creation is not supported, only the binary document conversion is supported.
     * <p>
     * See
     * <a href="https://wopi.readthedocs.io/projects/wopirest/en/latest/files/PutRelativeFile.html">PutRelativeFile</a>.
     */
    public Object putRelativeFile() {
        String suggestedTarget = getHeader(OPERATION_PUT_RELATIVE_FILE, SUGGESTED_TARGET, true);
        if (suggestedTarget != null) {
            suggestedTarget = Helpers.readUTF7String(suggestedTarget);
        }
        String relativeTarget = getHeader(OPERATION_PUT_RELATIVE_FILE, RELATIVE_TARGET, true);
        if (relativeTarget != null) {
            relativeTarget = Helpers.readUTF7String(relativeTarget);
        }
        String fileConversion = getHeader(OPERATION_PUT_RELATIVE_FILE, FILE_CONVERSION, true);
        logRequest(OPERATION_PUT_RELATIVE_FILE, SUGGESTED_TARGET, suggestedTarget, RELATIVE_TARGET, relativeTarget,
                FILE_CONVERSION, fileConversion);

        // exactly one should be empty
        if (StringUtils.isEmpty(suggestedTarget) == StringUtils.isEmpty(relativeTarget)) {
            logCondition(() -> SUGGESTED_TARGET + " and " + RELATIVE_TARGET
                    + " headers are both present or not present, yet they are mutually exclusive");
            logResponse(OPERATION_PUT_RELATIVE_FILE, SC_NOT_IMPLEMENTED);
            throw new NotImplementedException();
        }

        final String newFileName;
        if (StringUtils.isNotEmpty(suggestedTarget)) {
            logCondition(() -> SUGGESTED_TARGET + " header is present");
            newFileName = suggestedTarget.startsWith(".")
                    ? FilenameUtils.getBaseName(blob.getFilename()) + suggestedTarget
                    : suggestedTarget;
        } else {
            newFileName = relativeTarget;
        }

        // throw NotImplementedException for a new file creation request as it is not supported
        if (StringUtils.isEmpty(fileConversion)) {
            logCondition(() -> FILE_CONVERSION + " header is not present, yet new file creation is not supported");
            logResponse(OPERATION_PUT_RELATIVE_FILE, SC_NOT_IMPLEMENTED);
            throw new NotImplementedException();
        }

        // handle binary file conversion
        logCondition(() -> FILE_CONVERSION + " header is present, handling file conversion");
        DocumentModel newDoc = createVersionFromRequestBody(newFileName);

        String token = Helpers.getJWTToken(request);
        String newFileId = FileInfo.computeFileId(newDoc, xpath);
        String wopiSrc = String.format("%s%s%s?%s=%s", wopiBaseURL, FILES_ENDPOINT_PATH, newFileId,
                ACCESS_TOKEN_PARAMETER, token);
        String hostViewUrl = Helpers.getWOPIURL(baseURL, ACTION_VIEW, newDoc, xpath);
        String hostEditUrl = Helpers.getWOPIURL(baseURL, ACTION_EDIT, newDoc, xpath);

        Map<String, Serializable> map = new HashMap<>();
        map.put(NAME, newFileName);
        map.put(URL, wopiSrc);
        map.put(HOST_VIEW_URL, hostViewUrl);
        map.put(HOST_EDIT_URL, hostEditUrl);
        logResponse(OPERATION_PUT_RELATIVE_FILE, OK.getStatusCode(), map);
        return Response.ok(map).type(MediaType.APPLICATION_JSON).build();
    }

    protected DocumentModel createVersionFromRequestBody(String filename) {
        Blob newBlob = createBlobFromRequestBody(filename, null);
        doc.setPropertyValue(xpath, (Serializable) newBlob);
        doc.putContextData(SOURCE, WOPI_SOURCE);
        doc = session.saveDocument(doc);
        logNuxeoAction(() -> "Created a version of document " + doc.getId() + " with filename " + filename);
        return doc;
    }

    /**
     * Implements the RenameFile operation.
     * <p>
     * See <a href="https://wopi.readthedocs.io/projects/wopirest/en/latest/files/RenameFile.html">RenameFile</a>.
     */
    public Object renameFile() {
        checkWritePropertiesPermission(OPERATION_RENAME_FILE);

        String requestedName = Helpers.readUTF7String(getHeader(OPERATION_RENAME_FILE, REQUESTED_NAME));
        if (!doc.isLocked()) {
            logCondition("Document isn't locked");
            logRequest(OPERATION_RENAME_FILE, REQUESTED_NAME, requestedName);
            return renameBlob(requestedName);
        }

        String currentLock = getCurrentLock(OPERATION_RENAME_FILE);
        String lock = getHeader(OPERATION_RENAME_FILE, LOCK);
        logRequest(OPERATION_RENAME_FILE, REQUESTED_NAME, requestedName, LOCK, lock);
        if (lock.equals(currentLock)) {
            logCondition(() -> LOCK + " header is equal to current WOPI lock");
            return renameBlob(requestedName);
        } else {
            logCondition(() -> LOCK + " header is not equal to current WOPI lock");
            return buildConflictResponse(OPERATION_RENAME_FILE, currentLock);
        }
    }

    /**
     * Renames the blob with the {@code requestedName}.
     *
     * @return the expected JSON response for the RenameFile operation.
     */
    protected Response renameBlob(String requestedName) {
        String extension = FilenameUtils.getExtension(blob.getFilename());
        String fullFilename = requestedName + (extension != null ? "." + extension : "");
        logNuxeoAction(() -> "Renaming blob to " + fullFilename);
        blob.setFilename(fullFilename);
        doc.setPropertyValue(xpath, (Serializable) blob);
        session.saveDocument(doc);

        Map<String, Serializable> map = new HashMap<>();
        map.put(NAME, requestedName);
        logResponse(OPERATION_RENAME_FILE, OK.getStatusCode(), map);
        return Response.ok(map).type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * Implements the GetShareUrl operation.
     * <p>
     * See <a href="https://wopi.readthedocs.io/projects/wopirest/en/latest/files/GetShareUrl.html">GetShareUrl</a>.
     */
    public Object getShareUrl() {
        String urlType = getHeader(OPERATION_GET_SHARE_URL, URL_TYPE, true);
        logRequest(OPERATION_GET_SHARE_URL, URL_TYPE, urlType);

        if (!SHARE_URL_READ_ONLY.equals(urlType) && !SHARE_URL_READ_WRITE.equals(urlType)) {
            logCondition(
                    () -> URL_TYPE + " header should be either " + SHARE_URL_READ_ONLY + " or " + SHARE_URL_READ_WRITE);
            logResponse(OPERATION_GET_SHARE_URL, SC_NOT_IMPLEMENTED);
            throw new NotImplementedException();
        }

        String shareURL = Helpers.getWOPIURL(baseURL, urlType.equals(SHARE_URL_READ_ONLY) ? ACTION_VIEW : ACTION_EDIT,
                doc, xpath);

        Map<String, Serializable> map = new HashMap<>();
        map.put(SHARE_URL, shareURL);
        logResponse(OPERATION_GET_SHARE_URL, OK.getStatusCode(), map);
        return Response.ok(map).type(MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("contents")
    public Object doPostContents(@HeaderParam(OVERRIDE) Operation operation) {
        if (PUT.equals(operation)) {
            return putFile();
        }
        logCondition(() -> "Invalid value " + operation + " for " + OVERRIDE + " header, should be " + PUT.name());
        throw new BadRequestException();
    }

    /**
     * Implements the PutFile operation.
     * <p>
     * See <a href="https://wopi.readthedocs.io/projects/wopirest/en/latest/files/PutFile.html">PutFile</a>.
     */
    public Object putFile() {
        checkWritePropertiesPermission(OPERATION_PUT_FILE);

        if (!doc.isLocked()) {
            logRequest(OPERATION_PUT_FILE);
            logCondition("Document isn't locked");
            if (blob.getLength() == 0) {
                logCondition("Blob is empty");
                return updateBlob();
            }
            logCondition("Blob is not empty");
            buildConflictResponse(OPERATION_PUT_FILE, "");
        }

        String currentLock = getCurrentLock(OPERATION_PUT_FILE);
        String lock = getHeader(OPERATION_PUT_FILE, LOCK);
        logRequest(OPERATION_PUT_FILE, LOCK, lock);
        if (lock.equals(currentLock)) {
            logCondition(() -> LOCK + " header is equal to current WOPI lock");
            return updateBlob();
        } else {
            logCondition(() -> LOCK + " header is not equal to current WOPI lock");
            return buildConflictResponse(OPERATION_PUT_FILE, currentLock);
        }
    }

    /**
     * Updates the document's blob from a new one.
     *
     * @return the expected response for the PutFile operation, with the 'X-WOPI-ItemVersion' header set.
     */
    protected Response updateBlob() {
        logNuxeoAction("Updating blob");
        Blob newBlob = createBlobFromRequestBody(blob.getFilename(), blob.getMimeType());
        doc.setPropertyValue(xpath, (Serializable) newBlob);
        doc = session.saveDocument(doc);
        newBlob = (Blob) doc.getPropertyValue(xpath);
        return buildItemVersionResponse(OPERATION_PUT_FILE, newBlob);
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
     * See <a href="https://wopirest.readthedocs.io/en/latest/files/Unlock.html">Unlock</a>.
     */
    protected Object unlock() {
        String lock = getHeader(OPERATION_UNLOCK, LOCK);
        logRequest(OPERATION_UNLOCK, LOCK, lock);
        return unlockOrRefresh(OPERATION_UNLOCK, lock, true);
    }

    /**
     * Implements the RefreshLock operation.
     * <p>
     * See <a href="https://wopirest.readthedocs.io/en/latest/files/RefreshLock.html">RefreshLock</a>.
     */
    protected Object refreshLock() {
        String lock = getHeader(OPERATION_REFRESH_LOCK, LOCK);
        logRequest(OPERATION_REFRESH_LOCK, LOCK, lock);
        return unlockOrRefresh(OPERATION_REFRESH_LOCK, lock, false);
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

    protected String getHeader(String operation, String headerName) {
        return getHeader(operation, headerName, false);
    }

    protected String getHeader(String operation, String headerName, boolean nullable) {
        String header = Helpers.getHeader(httpHeaders, headerName);
        if (StringUtils.isEmpty(header) && !nullable) {
            logCondition(() -> "Header " + headerName + " is not present yet not nullable");
            logResponse(operation, BAD_REQUEST.getStatusCode());
            throw new BadRequestException();
        }
        return header;
    }

    protected void checkWritePropertiesPermission(String operation) {
        if (!session.hasPermission(doc.getRef(), SecurityConstants.WRITE_PROPERTIES)) {
            logCondition("Write permission check failed");
            // cannot rename blob
            logResponse(operation, CONFLICT.getStatusCode());
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
        NuxeoPrincipal principal = session.getPrincipal();
        map.put(BASE_FILE_NAME, blob.getFilename());
        map.put(OWNER_ID, doc.getPropertyValue("dc:creator"));
        map.put(SIZE, blob.getLength());
        map.put(USER_ID, principal.getName());
        map.put(VERSION, blob.getDigest());
    }

    protected void addHostCapabilitiesProperties(Map<String, Serializable> map) {
        map.put(SUPPORTS_EXTENDED_LOCK_LENGTH, true);
        map.put(SUPPORTS_GET_LOCK, true);
        map.put(SUPPORTS_LOCKS, true);
        map.put(SUPPORTS_RENAME, true);
        map.put(SUPPORTS_UPDATE, true);
        map.put(SUPPORTED_SHARE_URL_TYPES, (Serializable) Arrays.asList(SHARE_URL_READ_ONLY, SHARE_URL_READ_WRITE));
    }

    protected void addUserMetadataProperties(Map<String, Serializable> map) {
        NuxeoPrincipal principal = session.getPrincipal();
        map.put(IS_ANONYMOUS_USER, principal.isAnonymous());
        map.put(LICENSE_CHECK_FOR_EDIT_IS_ENABLED, true);
        map.put(USER_FRIENDLY_NAME, Helpers.principalFullName(principal));
    }

    protected void addUserPermissionsProperties(Map<String, Serializable> map) {
        boolean hasWriteProperties = session.hasPermission(doc.getRef(), SecurityConstants.WRITE_PROPERTIES);
        boolean canConvert = Framework.getService(WOPIService.class).getActionURL(blob, ACTION_CONVERT) != null;
        map.put(READ_ONLY, !hasWriteProperties);
        map.put(USER_CAN_RENAME, hasWriteProperties);
        map.put(USER_CAN_WRITE, hasWriteProperties);
        map.put(USER_CAN_NOT_WRITE_RELATIVE, !canConvert);
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

    protected void logRequest(String operation, String... headers) {
        log.debug("Request: repository={} docId={} xpath={} user={} fileId={} operation={}{}", doc::getRepositoryName,
                doc::getId, () -> xpath, session::getPrincipal, () -> fileId, () -> operation,
                () -> getHeaderString(headers));
    }

    protected void logCondition(String condition) {
        logCondition(() -> condition);
    }

    protected void logCondition(Supplier<String> condition) {
        log.debug("Condition: repository={} docId={} xpath={} user={} fileId={} {}", doc::getRepositoryName, doc::getId,
                () -> xpath, session::getPrincipal, () -> fileId, condition::get);
    }

    protected void logNuxeoAction(String action) {
        logNuxeoAction(() -> action);
    }

    protected void logNuxeoAction(Supplier<String> action) {
        log.debug("Nuxeo action: repository={} docId={} xpath={} user={} fileId={} {}", doc::getRepositoryName,
                doc::getId, () -> xpath, session::getPrincipal, () -> fileId, action::get);
    }

    protected void logResponse(String operation, int status, String... headers) {
        logResponse(operation, status, null, headers);
    }

    protected void logResponse(String operation, int status, Object entity, String... headers) {
        log.debug("Response: repository={} docId={} xpath={} user={} fileId={} operation={} status={}{}{}",
                doc::getRepositoryName, doc::getId, () -> xpath, session::getPrincipal, () -> fileId, () -> operation,
                () -> status, () -> getEntityString(entity), () -> getHeaderString(headers));
    }

    protected String getHeaderString(String... headers) {
        if (ArrayUtils.isEmpty(headers)) {
            return "";
        }
        Map<String, String> headerMap = new HashMap<>();
        for (int i = 0; i < headers.length; i += 2) {
            headerMap.put(headers[i], headers[i + 1]);
        }
        return " headers=" + headerMap;
    }

    protected String getEntityString(Object entity) {
        return entity == null ? "" : " body=" + entity.toString();
    }

}
