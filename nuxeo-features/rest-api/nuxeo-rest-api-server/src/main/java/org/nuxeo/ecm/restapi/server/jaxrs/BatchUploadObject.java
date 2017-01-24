/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.jaxrs.io.operations.ExecutionRequest;
import org.nuxeo.ecm.automation.server.jaxrs.ResponseHelper;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchFileEntry;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManagerConstants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Batch upload endpoint.
 * <p>
 * Replaces the deprecated endpoints listed below:
 * <ul>
 * <li>POST /batch/upload, see org.nuxeo.ecm.automation.server.jaxrs.batch.BatchResource#doPost(HttpServletRequest), use
 * POST /upload/{batchId}/{fileIdx} instead, see {@link #upload(HttpServletRequest, String, String)}</li>
 * <li>GET /batch/files/{batchId}, see org.nuxeo.ecm.automation.server.jaxrs.batch.BatchResource#getFilesBatch(String),
 * use GET /upload/{batchId} instead, see {@link #getBatchInfo(String)} instead</li>
 * <li>GET /batch/drop/{batchId}, see org.nuxeo.ecm.automation.server.jaxrs.batch.BatchResource#dropBatch(String), use
 * DELETE /upload/{batchId} instead, see {@link #dropBatch(String)}</li>
 * </ul>
 * Also provides new endpoints:
 * <ul>
 * <li>POST /upload, see {@link #initBatch()}</li>
 * <li>GET /upload/{batchId}/{fileIdx}, see {@link #getFileInfo(String, String)}</li>
 * </ul>
 * Largely inspired by the excellent Google Drive REST API documentation about <a
 * href="https://developers.google.com/drive/web/manage-uploads#resumable">resumable upload</a>.
 *
 * @since 7.4
 */
@WebObject(type = "upload")
public class BatchUploadObject extends AbstractResource<ResourceTypeImpl> {

    protected static final Log log = LogFactory.getLog(BatchUploadObject.class);

    protected static final String REQUEST_BATCH_ID = "batchId";

    protected static final String REQUEST_FILE_IDX = "fileIdx";

    protected static final String OPERATION_ID = "operationId";

    public static final String UPLOAD_TYPE_NORMAL = "normal";

    public static final String UPLOAD_TYPE_CHUNKED = "chunked";

    @POST
    public Response initBatch() throws IOException {
        BatchManager bm = Framework.getService(BatchManager.class);
        String batchId = bm.initBatch();
        Map<String, String> result = new HashMap<String, String>();
        result.put("batchId", batchId);
        return buildResponse(Status.CREATED, result);
    }

    @POST
    @Path("{batchId}/{fileIdx}")
    public Response upload(@Context HttpServletRequest request, @PathParam(REQUEST_BATCH_ID) String batchId,
            @PathParam(REQUEST_FILE_IDX) String fileIdx) throws IOException {
        TransactionHelper.commitOrRollbackTransaction();
        try {
            return uploadNoTransaction(request, batchId, fileIdx);
        } finally {
            TransactionHelper.startTransaction();
        }
    }

    protected Response uploadNoTransaction(@Context HttpServletRequest request,
            @PathParam(REQUEST_BATCH_ID) String batchId, @PathParam(REQUEST_FILE_IDX) String fileIdx)
            throws IOException {

        BatchManager bm = Framework.getService(BatchManager.class);
        if (!bm.hasBatch(batchId)) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }

        // Check file index parameter
        if (!NumberUtils.isDigits(fileIdx)) {
            return buildTextResponse(Status.BAD_REQUEST, "fileIdx request path parameter must be a number");
        }

        boolean isMultipart = false;

        // Parameters are passed as request header, the request body is the stream
        String contentType = request.getHeader("Content-Type");
        String uploadType = request.getHeader("X-Upload-Type");
        String contentLength = request.getHeader("Content-Length");
        String uploadChunkIndex = request.getHeader("X-Upload-Chunk-Index");
        String chunkCount = request.getHeader("X-Upload-Chunk-Count");
        String fileName = request.getHeader("X-File-Name");
        String fileSize = request.getHeader("X-File-Size");
        String mimeType = request.getHeader("X-File-Type");
        InputStream is = null;

        BatchFileEntry fileEntry = null;
        String uploadedSize = "0";
        long contentLengthAsLong = -1;
        if (contentLength != null) {
            contentLengthAsLong = Long.valueOf(contentLength);
        }
        if (contentLengthAsLong > -1) {
            uploadedSize = contentLength;
            // Handle multipart case: mainly MSIE with jQueryFileupload
            if (contentType != null && contentType.contains("multipart")) {
                isMultipart = true;
                FormData formData = new FormData(request);
                Blob blob = formData.getFirstBlob();
                if (blob != null) {
                    is = blob.getStream();
                    if (!UPLOAD_TYPE_CHUNKED.equals(uploadType)) {
                        fileName = blob.getFilename();
                    }
                    mimeType = blob.getMimeType();
                    uploadedSize = String.valueOf(blob.getLength());
                }
            } else {
                if (fileName != null) {
                    fileName = URLDecoder.decode(fileName, "UTF-8");
                }
                is = request.getInputStream();
            }

            if (UPLOAD_TYPE_CHUNKED.equals(uploadType)) {
                try {
                    log.debug(String.format("Uploading chunk [index=%s / total=%s] (%sb) for file %s",
                            uploadChunkIndex, chunkCount, uploadedSize, fileName));
                    bm.addStream(batchId, fileIdx, is, Integer.parseInt(chunkCount),
                            Integer.parseInt(uploadChunkIndex), fileName, mimeType, Long.parseLong(fileSize));
                } catch (NumberFormatException e) {
                    return buildTextResponse(Status.BAD_REQUEST,
                            "X-Upload-Chunk-Index, X-Upload-Chunk-Count and X-File-Size headers must be numbers");
                }
            } else {
                // Use non chunked mode by default if X-Upload-Type header is not provided
                uploadType = UPLOAD_TYPE_NORMAL;
                log.debug(String.format("Uploading file %s (%sb)", fileName, uploadedSize));
                bm.addStream(batchId, fileIdx, is, fileName, mimeType);
            }
        } else {
            fileEntry = bm.getFileEntry(batchId, fileIdx);
            if (fileEntry == null) {
                return buildEmptyResponse(Status.NOT_FOUND);
            }
        }

        if (fileEntry == null && UPLOAD_TYPE_CHUNKED.equals(uploadType)) {
            fileEntry = bm.getFileEntry(batchId, fileIdx);
        }

        StatusType status = Status.CREATED;
        Map<String, Object> result = new HashMap<>();
        result.put("uploaded", "true");
        result.put("batchId", batchId);
        result.put("fileIdx", fileIdx);
        result.put("uploadedSize", uploadedSize);
        if (fileEntry != null && fileEntry.isChunked()) {
            result.put("uploadType", UPLOAD_TYPE_CHUNKED);
            result.put("uploadedChunkIds", fileEntry.getOrderedChunkIndexes());
            result.put("chunkCount", fileEntry.getChunkCount());
            if (!fileEntry.isChunksCompleted()) {
                status = new ResumeIncompleteStatusType();
            }
        } else {
            result.put("uploadType", UPLOAD_TYPE_NORMAL);
        }
        return buildResponse(status, result, isMultipart);
    }

    @GET
    @Path("{batchId}")
    public Response getBatchInfo(@PathParam(REQUEST_BATCH_ID) String batchId) throws IOException {
        BatchManager bm = Framework.getService(BatchManager.class);
        if (!bm.hasBatch(batchId)) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }
        List<BatchFileEntry> fileEntries = bm.getFileEntries(batchId);
        if (CollectionUtils.isEmpty(fileEntries)) {
            return buildEmptyResponse(Status.NO_CONTENT);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (BatchFileEntry fileEntry : fileEntries) {
            result.add(getFileInfo(fileEntry));
        }
        return buildResponse(Status.OK, result);
    }

    @GET
    @Path("{batchId}/{fileIdx}")
    public Response getFileInfo(@PathParam(REQUEST_BATCH_ID) String batchId, @PathParam(REQUEST_FILE_IDX) String fileIdx)
            throws IOException {
        BatchManager bm = Framework.getService(BatchManager.class);
        if (!bm.hasBatch(batchId)) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }
        BatchFileEntry fileEntry = bm.getFileEntry(batchId, fileIdx);
        if (fileEntry == null) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }
        StatusType status = Status.OK;
        if (fileEntry.isChunked() && !fileEntry.isChunksCompleted()) {
            status = new ResumeIncompleteStatusType();
        }
        Map<String, Object> result = getFileInfo(fileEntry);
        return buildResponse(status, result);
    }

    @DELETE
    @Path("{batchId}")
    public Response cancel(@PathParam(REQUEST_BATCH_ID) String batchId) throws IOException {
        BatchManager bm = Framework.getLocalService(BatchManager.class);
        if (!bm.hasBatch(batchId)) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }
        bm.clean(batchId);
        return buildEmptyResponse(Status.NO_CONTENT);
    }

    /**
     * @since 8.4
     */
    @DELETE
    @Path("{batchId}/{fileIdx}")
    public Response removeFile(@PathParam(REQUEST_BATCH_ID) String batchId, @PathParam(REQUEST_FILE_IDX) String fileIdx)
        throws IOException {
        BatchManager bm = Framework.getLocalService(BatchManager.class);
        if (!bm.removeFileEntry(batchId, fileIdx)) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }
        return buildEmptyResponse(Status.NO_CONTENT);
    }

    @Context
    HttpServletRequest request;

    @Context
    HttpServletResponse response;

    @POST
    @Produces("application/json")
    @Path("{batchId}/execute/{operationId}")
    public Object execute(@PathParam(REQUEST_BATCH_ID) String batchId, @PathParam(OPERATION_ID) String operationId,
            ExecutionRequest xreq) throws UnsupportedEncodingException {
        return executeBatch(batchId, null, operationId, request, xreq);
    }

    @POST
    @Produces("application/json")
    @Path("{batchId}/{fileIdx}/execute/{operationId}")
    public Object execute(@PathParam(REQUEST_BATCH_ID) String batchId, @PathParam(REQUEST_FILE_IDX) String fileIdx,
            @PathParam(OPERATION_ID) String operationId, ExecutionRequest xreq)
            throws UnsupportedEncodingException {
        return executeBatch(batchId, fileIdx, operationId, request, xreq);
    }

    protected Object executeBatch(String batchId, String fileIdx, String operationId, HttpServletRequest request,
            ExecutionRequest xreq) throws UnsupportedEncodingException {

        if (!Boolean.parseBoolean(
            RequestContext.getActiveContext(request).getRequest().getHeader(BatchManagerConstants.NO_DROP_FLAG))) {
            RequestContext.getActiveContext(request).addRequestCleanupHandler(req -> {
                BatchManager bm = Framework.getService(BatchManager.class);
                bm.clean(batchId);
            });
        }

        try {
            CoreSession session = ctx.getCoreSession();
            OperationContext ctx = xreq.createContext(request, response, session);
            Map<String, Object> params = xreq.getParams();
            BatchManager bm = Framework.getLocalService(BatchManager.class);
            Object result;
            if (StringUtils.isBlank(fileIdx)) {
                result = bm.execute(batchId, operationId, session, ctx, params);
            } else {
                result = bm.execute(batchId, fileIdx, operationId, session, ctx, params);
            }
            return ResponseHelper.getResponse(result, request);
        } catch (NuxeoException | MessagingException | IOException e) {
            log.error("Error while executing automation batch ", e);
            if (WebException.isSecurityError(e)) {
                return buildJSONResponse(Status.FORBIDDEN, "{\"error\" : \"" + e.getMessage() + "\"}");
            } else {
                return buildJSONResponse(Status.INTERNAL_SERVER_ERROR, "{\"error\" : \"" + e.getMessage() + "\"}");
            }
        }
    }

    protected Response buildResponse(StatusType status, Object object) throws IOException {
        return buildResponse(status, object, false);
    }

    protected Response buildResponse(StatusType status, Object object, boolean html) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String result = mapper.writeValueAsString(object);
        if (html) {
            // For MSIE with iframe transport: we need to return HTML!
            return buildHTMLResponse(status, result);
        } else {
            return buildJSONResponse(status, result);
        }
    }

    protected Response buildJSONResponse(StatusType status, String message) throws UnsupportedEncodingException {
        return buildResponse(status, MediaType.APPLICATION_JSON, message);
    }

    protected Response buildHTMLResponse(StatusType status, String message) throws UnsupportedEncodingException {
        message = "<html>" + message + "</html>";
        return buildResponse(status, MediaType.TEXT_HTML, message);
    }

    protected Response buildTextResponse(StatusType status, String message) throws UnsupportedEncodingException {
        return buildResponse(status, MediaType.TEXT_PLAIN, message);
    }

    protected Response buildEmptyResponse(StatusType status) {
        return Response.status(status).build();
    }

    protected Response buildResponse(StatusType status, String type, String message)
            throws UnsupportedEncodingException {
        return Response.status(status)
                       .header("Content-Length", message.getBytes("UTF-8").length)
                       .type(type + "; charset=UTF-8")
                       .entity(message)
                       .build();
    }

    protected Map<String, Object> getFileInfo(BatchFileEntry fileEntry) throws UnsupportedEncodingException {
        Map<String, Object> info = new HashMap<>();
        boolean chunked = fileEntry.isChunked();
        String uploadType;
        if (chunked) {
            uploadType = UPLOAD_TYPE_CHUNKED;
        } else {
            uploadType = UPLOAD_TYPE_NORMAL;
        }
        info.put("name", fileEntry.getFileName());
        info.put("size", fileEntry.getFileSize());
        info.put("uploadType", uploadType);
        if (chunked) {
            info.put("uploadedChunkIds", fileEntry.getOrderedChunkIndexes());
            info.put("chunkCount", fileEntry.getChunkCount());
        }
        return info;
    }

    public final class ResumeIncompleteStatusType implements StatusType {

        @Override
        public int getStatusCode() {
            return 308;
        }

        @Override
        public String getReasonPhrase() {
            return "Resume Incomplete";
        }

        @Override
        public Family getFamily() {
            // Technically we don't use 308 Resume Incomplete as a redirection but it is the default family for 3xx
            // status codes defined by Response$Status
            return Family.REDIRECTION;
        }
    }

}
