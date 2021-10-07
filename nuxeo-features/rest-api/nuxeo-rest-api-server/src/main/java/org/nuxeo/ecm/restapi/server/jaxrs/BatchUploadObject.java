/*
 * (C) Copyright 2015-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Luís Duarte
 *     Florent Guillaume
 *     Mickaël Schoentgen
 */
package org.nuxeo.ecm.restapi.server.jaxrs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import javax.ws.rs.core.Response.StatusType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.jaxrs.io.operations.ExecutionRequest;
import org.nuxeo.ecm.automation.server.jaxrs.ResponseHelper;
import org.nuxeo.ecm.automation.server.jaxrs.batch.Batch;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchFileEntry;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchHandler;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManagerConstants;
import org.nuxeo.ecm.automation.server.jaxrs.batch.handler.BatchFileInfo;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.io.NginxConstants;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.JsonNode;
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
 * <li>POST /upload/{batchId}/refreshToken, see {@link #refreshToken(String)}</li>
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

    protected static final String REQUEST_HANDLER_NAME = "handlerName";

    /** @since 10.10-HF30 */
    protected static final String MULTIPART_DISABLED_CONFIG_KEY = "nuxeo.batch.upload.multipart.disabled";

    public static final String UPLOAD_TYPE_NORMAL = "normal";

    public static final String UPLOAD_TYPE_CHUNKED = "chunked";

    public static final String KEY = "key";

    public static final String NAME = "name";

    public static final String MIMETYPE = "mimeType";

    public static final String FILE_SIZE = "fileSize";

    public static final String MD5 = "md5";

    protected Map<String, String> mapWithName(String name) {
        return Collections.singletonMap("name", name);
    }

    @GET
    @Path("handlers")
    public Response handlers() throws IOException {
        BatchManager bm = Framework.getService(BatchManager.class);
        Set<String> supportedHandlers = bm.getSupportedHandlers();
        List<Map<String, String>> handlers = supportedHandlers.stream().map(this::mapWithName).collect(
                Collectors.toList());
        Map<String, Object> result = Collections.singletonMap("handlers", handlers);
        return buildResponse(Status.OK, result);
    }

    @GET
    @Path("handlers/{handlerName}")
    public Response getHandlerInfo(@PathParam(REQUEST_HANDLER_NAME) String handlerName) throws IOException {
        BatchManager bm = Framework.getService(BatchManager.class);
        BatchHandler handler = bm.getHandler(handlerName);
        if (handler == null) {
            return Response.status(Status.NOT_FOUND).build();
        }
        Map<String, String> result = mapWithName(handler.getName());
        return buildResponse(Status.OK, result);
    }

    @POST
    @Path("new/{handlerName}")
    public Response createNewBatch(@PathParam(REQUEST_HANDLER_NAME) String handlerName) throws IOException {
        BatchManager bm = Framework.getService(BatchManager.class);
        Batch batch = bm.initBatch(handlerName);
        return getBatchExtraInfo(batch.getKey());
    }

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
        BatchManager bm = Framework.getService(BatchManager.class);

        if (!bm.hasBatch(batchId)) {
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
        String requestBodyFile = request.getHeader(NginxConstants.X_REQUEST_BODY_FILE_HEADER);
        String contentMd5 = request.getHeader(NginxConstants.X_CONTENT_MD5_HEADER);

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
        boolean isMultipart = isMultipartEnabled() && contentType != null && contentType.contains("multipart");

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
            addBlob(uploadType, batchId, fileIdx, blob, fileName, mimeType, uploadedSize, chunkCount, uploadChunkIndex,
                    fileSize);
        } else if (Framework.isBooleanPropertyTrue(NginxConstants.X_ACCEL_ENABLED)
                && StringUtils.isNotEmpty(requestBodyFile)) {
            if (StringUtils.isNotEmpty(fileName)) {
                fileName = URLDecoder.decode(fileName, "UTF-8");
            }
            File file = new File(requestBodyFile);
            Blob blob = new FileBlob(file, true);

            if (StringUtils.isNotEmpty(contentMd5)) {
                blob.setDigest(contentMd5);
            }

            uploadedSize = file.length();
            addBlob(uploadType, batchId, fileIdx, blob, fileName, mimeType, uploadedSize, chunkCount, uploadChunkIndex,
                    fileSize);
        } else {
            if (StringUtils.isNotEmpty(fileName)) {
                fileName = URLDecoder.decode(fileName, "UTF-8");
            }
            try (InputStream is = request.getInputStream()) {
                Blob blob = Blobs.createBlob(is);
                addBlob(uploadType, batchId, fileIdx, blob, fileName, mimeType, uploadedSize, chunkCount,
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
            BatchFileEntry fileEntry = bm.getFileEntry(batchId, fileIdx);
            if (fileEntry != null) {
                result.put("uploadedChunkIds", fileEntry.getOrderedChunkIndexes());
                result.put("chunkCount", fileEntry.getChunkCount());
                if (!fileEntry.isChunksCompleted()) {
                    status = Status.ACCEPTED;
                }
            }
        }
        return buildResponse(status, result, isMultipart);
    }

    /** @since 10.10-HF30 */
    protected boolean isMultipartEnabled() {
        return Framework.getService(ConfigurationService.class).isBooleanFalse(MULTIPART_DISABLED_CONFIG_KEY);
    }

    protected long getUploadedSize(HttpServletRequest request) {
        String contentLength = request.getHeader("Content-Length");
        if (contentLength == null) {
            return -1;
        }
        return Long.parseLong(contentLength);
    }

    protected void addBlob(String uploadType, String batchId, String fileIdx, Blob blob, String fileName,
            String mimeType, long uploadedSize, int chunkCount, int uploadChunkIndex, long fileSize) {
        BatchManager bm = Framework.getService(BatchManager.class);
        String uploadedSizeDisplay = uploadedSize > -1 ? uploadedSize + "b" : "unknown size";
        Batch batch = bm.getBatch(batchId);
        if (UPLOAD_TYPE_CHUNKED.equals(uploadType)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Uploading chunk [index=%d / total=%d] (%s) for file %s", uploadChunkIndex,
                        chunkCount, uploadedSizeDisplay, fileName));
            }
            batch.addChunk(fileIdx, blob, chunkCount, uploadChunkIndex, fileName, mimeType, fileSize);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Uploading file %s (%s)", fileName, uploadedSizeDisplay));
            }
            batch.addFile(fileIdx, blob, fileName, mimeType);
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
            status = Status.ACCEPTED;
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
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{batchId}/execute/{operationId}")
    public Object execute(@PathParam(REQUEST_BATCH_ID) String batchId, @PathParam(OPERATION_ID) String operationId,
            ExecutionRequest xreq) {
        return executeBatch(batchId, null, operationId, request, xreq);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{batchId}/{fileIdx}/execute/{operationId}")
    public Object execute(@PathParam(REQUEST_BATCH_ID) String batchId, @PathParam(REQUEST_FILE_IDX) String fileIdx,
            @PathParam(OPERATION_ID) String operationId, ExecutionRequest xreq) {
        return executeBatch(batchId, fileIdx, operationId, request, xreq);
    }

    @GET
    @Path("{batchId}/info")
    public Response getBatchExtraInfo(@PathParam(REQUEST_BATCH_ID) String batchId) throws IOException {
        BatchManager bm = Framework.getService(BatchManager.class);
        if (!bm.hasBatch(batchId)) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }
        Batch batch = bm.getBatch(batchId);
        Map<String, Object> properties = batch.getProperties();
        List<BatchFileEntry> fileEntries = batch.getFileEntries();

        List<Map<String, Object>> fileInfos = new ArrayList<>();
        if (!CollectionUtils.isEmpty(fileEntries)) {
            fileEntries.stream().map(this::getFileInfo).forEach(fileInfos::add);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("provider", batch.getHandlerName());
        if (properties != null && !properties.isEmpty()) {
            result.put("extraInfo", properties);
        }

        result.put("fileEntries", fileInfos);
        result.put("batchId", batch.getKey());
        return buildResponse(Status.OK, result);
    }

    /** @since 11.1 */
    @POST
    @Path("{batchId}/refreshToken")
    public Response refreshToken(@PathParam(REQUEST_BATCH_ID) String batchId) throws IOException {
        BatchManager bm = Framework.getService(BatchManager.class);

        Batch batch = bm.getBatch(batchId);
        if (batch == null) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }

        BatchHandler handler = bm.getHandler(batch.getHandlerName());
        try {
            Map<String, Object> result = handler.refreshToken(batchId);
            result.put("batchId", batchId);
            return buildResponse(Status.OK, result);
        } catch (UnsupportedOperationException e) {
            return Response.status(HttpServletResponse.SC_NOT_IMPLEMENTED).build();
        }
    }

    @POST
    @Path("{batchId}/{fileIdx}/complete")
    public Response complete(@PathParam(REQUEST_BATCH_ID) String batchId, @PathParam(REQUEST_FILE_IDX) String fileIdx,
            String body) throws IOException {
        TransactionHelper.commitOrRollbackTransaction();
        try {
            return completeNoTransaction(batchId, fileIdx, body);
        } finally {
            TransactionHelper.startTransaction();
        }
    }

    protected Response completeNoTransaction(String batchId, String fileIdx, String body) throws IOException {
        BatchManager bm = Framework.getService(BatchManager.class);
        JsonNode jsonNode = new ObjectMapper().readTree(body);

        Batch batch = bm.getBatch(batchId);
        if (batch == null) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }

        String key = jsonNode.hasNonNull(KEY) ? jsonNode.get(KEY).asText(null) : null;
        String filename = jsonNode.hasNonNull(NAME) ? jsonNode.get(NAME).asText() : null;
        String mimeType = jsonNode.hasNonNull(MIMETYPE) ? jsonNode.get(MIMETYPE).asText(null) : null;
        Long length = jsonNode.hasNonNull(FILE_SIZE) ? jsonNode.get(FILE_SIZE).asLong() : -1L;
        String md5 = jsonNode.hasNonNull(MD5) ? jsonNode.get(MD5).asText() : null;

        BatchFileInfo batchFileInfo = new BatchFileInfo(key, filename, mimeType, length, md5);

        BatchHandler handler = bm.getHandler(batch.getHandlerName());
        if (!handler.completeUpload(batchId, fileIdx, batchFileInfo)) {
            return Response.status(Status.CONFLICT).build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("uploaded", "true");
        result.put("batchId", batchId);
        result.put("fileIdx", fileIdx);
        return buildResponse(Status.OK, result);
    }

    protected Object executeBatch(String batchId, String fileIdx, String operationId, HttpServletRequest request,
            ExecutionRequest xreq) {
        BatchManager bm = Framework.getService(BatchManager.class);

        if (!bm.hasBatch(batchId)) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }

        if (!Boolean.parseBoolean(
                RequestContext.getActiveContext(request).getRequest().getHeader(BatchManagerConstants.NO_DROP_FLAG))) {
            RequestContext.getActiveContext(request).addRequestCleanupHandler(req -> bm.clean(batchId));
        }

        try {
            CoreSession session = ctx.getCoreSession();
            Object result;
            try (OperationContext ctx = xreq.createContext(request, response, session)) {
                Map<String, Object> params = xreq.getParams();
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

}
