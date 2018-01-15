/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.jaxrs.io.operations.ExecutionRequest;
import org.nuxeo.ecm.automation.server.jaxrs.ResponseHelper;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchFileEntry;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManagerConstants;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Batch upload endpoint.
 * <p>
 * Provides the APIs listed below:
 * <ul>
 * <li>POST /upload, see {@link #initBatch()}</li>
 * <li>POST /upload/{batchId}/{fileIdx}, see {@link #upload(HttpServletRequest, String, String)}</li>
 * <li>GET /upload/{batchId}, see {@link #getBatchInfo(String)}</li>
 * <li>GET /upload/{batchId}/{fileIdx}, see {@link #getFileInfo(String, String)}</li>
 * <li>POST /upload/{batchId}/execute/{operationId}, see {@link #execute(String, String, ExecutionRequest)}</li>
 * <li>POST /upload/{batchId}/{fileIdx}/execute/{operationId}, see
 * {@link #execute(String, String, String, ExecutionRequest)}</li>
 * <li>DELETE /upload/{batchId}, see {@link #cancel(String)}</li>
 * <li>DELETE /upload/{batchId}/{fileIdx}, see {@link #removeFile(String, String)}</li>
 * </ul>
 * Largely inspired by the excellent Google Drive REST API documentation about
 * <a href="https://developers.google.com/drive/web/manage-uploads#resumable">resumable upload</a>.
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
        Map<String, String> result = new HashMap<>();
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

        if (!Framework.getService(BatchManager.class).hasBatch(batchId)) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }

        // Check file index parameter
        if (!NumberUtils.isDigits(fileIdx)) {
            return buildTextResponse(Status.BAD_REQUEST, "fileIdx request path parameter must be a number");
        }

        // Parameters are passed as request header, the request body is the stream
        String contentType = request.getHeader("Content-Type");
        String uploadType = request.getHeader("X-Upload-Type");
        // Use non chunked mode by default if X-Upload-Type header is not provided
        if (!UPLOAD_TYPE_CHUNKED.equals(uploadType)) {
            uploadType = UPLOAD_TYPE_NORMAL;
        }
        String uploadChunkIndexHeader = request.getHeader("X-Upload-Chunk-Index");
        String chunkCountHeader = request.getHeader("X-Upload-Chunk-Count");
        String fileName = request.getHeader("X-File-Name");
        String fileSizeHeader = request.getHeader("X-File-Size");
        String mimeType = request.getHeader("X-File-Type");

        int chunkCount = -1;
        int uploadChunkIndex = -1;
        long fileSize = -1;
        if (UPLOAD_TYPE_CHUNKED.equals(uploadType)) {
            try {
                chunkCount = Integer.parseInt(chunkCountHeader);
                uploadChunkIndex = Integer.parseInt(uploadChunkIndexHeader);
                fileSize = Long.parseLong(fileSizeHeader);
            } catch (NumberFormatException e) {
                throw new IllegalParameterException(
                        "X-Upload-Chunk-Index, X-Upload-Chunk-Count and X-File-Size headers must be numbers");
            }
        }

        // TODO NXP-18247: should be set to the actual number of bytes uploaded instead of relying on the Content-Length
        // header which is not necessarily set
        long uploadedSize = getUploadedSize(request);
        boolean isMultipart = contentType != null && contentType.contains("multipart");

        // Handle multipart case: mainly MSIE with jQueryFileupload
        if (isMultipart) {
            FormData formData = new FormData(request);
            Blob blob = formData.getFirstBlob();
            if (blob == null) {
                throw new NuxeoException("Cannot upload in multipart with no blobs");
            }
            if (!UPLOAD_TYPE_CHUNKED.equals(uploadType)) {
                fileName = blob.getFilename();
            }
            // Don't change the mime-type if it was forced via the X-File-Type header
            if (StringUtils.isBlank(mimeType)) {
                mimeType = blob.getMimeType();
            }
            uploadedSize = blob.getLength();
            try (InputStream is = blob.getStream()) {
                addStream(uploadType, batchId, fileIdx, is, fileName, mimeType, uploadedSize, chunkCount,
                        uploadChunkIndex, fileSize);
            }
        } else {
            if (fileName != null) {
                fileName = URLDecoder.decode(fileName, "UTF-8");
            }
            try (InputStream is = request.getInputStream()) {
                addStream(uploadType, batchId, fileIdx, is, fileName, mimeType, uploadedSize, chunkCount,
                        uploadChunkIndex, fileSize);
            }
        }

        StatusType status = Status.CREATED;
        Map<String, Object> result = new HashMap<>();
        result.put("uploaded", "true");
        result.put("batchId", batchId);
        result.put("fileIdx", fileIdx);
        result.put("uploadType", uploadType);
        result.put("uploadedSize", String.valueOf(uploadedSize));
        if (UPLOAD_TYPE_CHUNKED.equals(uploadType)) {
            BatchFileEntry fileEntry = Framework.getService(BatchManager.class).getFileEntry(batchId, fileIdx);
            if (fileEntry != null) {
                result.put("uploadedChunkIds", fileEntry.getOrderedChunkIndexes());
                result.put("chunkCount", fileEntry.getChunkCount());
                if (!fileEntry.isChunksCompleted()) {
                    status = new ResumeIncompleteStatusType();
                }
            }
        }
        return buildResponse(status, result, isMultipart);
    }

    protected long getUploadedSize(HttpServletRequest request) {
        String contentLength = request.getHeader("Content-Length");
        if (contentLength == null) {
            return -1;
        }
        return Long.parseLong(contentLength);
    }

    protected void addStream(String uploadType, String batchId, String fileIdx, InputStream is, String fileName,
            String mimeType, long uploadedSize, int chunkCount, int uploadChunkIndex, long fileSize)
                    throws IOException {
        String uploadedSizeDisplay = uploadedSize > -1 ? uploadedSize + "b" : "unknown size";
        if (UPLOAD_TYPE_CHUNKED.equals(uploadType)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Uploading chunk [index=%d / total=%d] (%s) for file %s", uploadChunkIndex,
                        chunkCount, uploadedSizeDisplay, fileName));
            }
            Framework.getService(BatchManager.class).addStream(batchId, fileIdx, is, chunkCount, uploadChunkIndex,
                    fileName, mimeType, fileSize);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Uploading file %s (%s)", fileName, uploadedSizeDisplay));
            }
            Framework.getService(BatchManager.class).addStream(batchId, fileIdx, is, fileName, mimeType);
        }
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
    public Response getFileInfo(@PathParam(REQUEST_BATCH_ID) String batchId,
            @PathParam(REQUEST_FILE_IDX) String fileIdx) throws IOException {
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
    public Response cancel(@PathParam(REQUEST_BATCH_ID) String batchId) {
        BatchManager bm = Framework.getService(BatchManager.class);
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
    public Response removeFile(@PathParam(REQUEST_BATCH_ID) String batchId,
            @PathParam(REQUEST_FILE_IDX) String fileIdx) {
        BatchManager bm = Framework.getService(BatchManager.class);
        if (!bm.removeFileEntry(batchId, fileIdx)) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }
        return buildEmptyResponse(Status.NO_CONTENT);
    }

    @Context
    protected HttpServletRequest request;

    @Context
    protected HttpServletResponse response;

    @POST
    @Produces("application/json")
    @Path("{batchId}/execute/{operationId}")
    public Object execute(@PathParam(REQUEST_BATCH_ID) String batchId, @PathParam(OPERATION_ID) String operationId,
            ExecutionRequest xreq) {
        return executeBatch(batchId, null, operationId, request, xreq);
    }

    @POST
    @Produces("application/json")
    @Path("{batchId}/{fileIdx}/execute/{operationId}")
    public Object execute(@PathParam(REQUEST_BATCH_ID) String batchId, @PathParam(REQUEST_FILE_IDX) String fileIdx,
            @PathParam(OPERATION_ID) String operationId, ExecutionRequest xreq) {
        return executeBatch(batchId, fileIdx, operationId, request, xreq);
    }

    protected Object executeBatch(String batchId, String fileIdx, String operationId, HttpServletRequest request,
            ExecutionRequest xreq) {

        if (!Framework.getService(BatchManager.class).hasBatch(batchId)) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }

        if (!Boolean.parseBoolean(
                RequestContext.getActiveContext(request).getRequest().getHeader(BatchManagerConstants.NO_DROP_FLAG))) {
            RequestContext.getActiveContext(request).addRequestCleanupHandler(req -> {
                BatchManager bm = Framework.getService(BatchManager.class);
                bm.clean(batchId);
            });
        }

        try {
            CoreSession session = ctx.getCoreSession();
            Object result;
            try (OperationContext ctx = xreq.createContext(request, response, session)) {
                Map<String, Object> params = xreq.getParams();
                BatchManager bm = Framework.getService(BatchManager.class);
                if (StringUtils.isBlank(fileIdx)) {
                    result = bm.execute(batchId, operationId, session, ctx, params);
                } else {
                    result = bm.execute(batchId, fileIdx, operationId, session, ctx, params);
                }
            }
            return ResponseHelper.getResponse(result, request);
        } catch (MessagingException | IOException e) {
            log.error("Error while executing automation batch ", e);
            throw new NuxeoException(e);
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

    protected Map<String, Object> getFileInfo(BatchFileEntry fileEntry) {
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
