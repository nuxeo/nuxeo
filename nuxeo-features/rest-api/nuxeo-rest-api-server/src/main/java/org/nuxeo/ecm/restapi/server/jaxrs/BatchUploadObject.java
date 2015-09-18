/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchResource;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Batch upload endpoint.
 * <p>
 * Replaces the deprecated endpoints listed below:
 * <ul>
 * <li>POST /batch/upload, see {@link BatchResource#doPost(HttpServletRequest)}, use POST /upload/{batchId}/{fileIdx}
 * instead, see {@link #upload(HttpServletRequest, String, String)}</li>
 * <li>GET /batch/files/{batchId}, see {@link BatchResource#getFilesBatch(String)}, use GET /upload/{batchId} instead,
 * see {@link #getBatchInfo(String)} instead</li>
 * <li>GET /batch/drop/{batchId}, see {@link BatchResource#dropBatch(String)}, use DELETE /upload/{batchId} instead, see
 * {@link #dropBatch(String)}</li>
 * </ul>
 * Also provides new endpoints:
 * <ul>
 * <li>POST /upload, see {@link #initBatch()}</li>
 * <li>GET /upload/{batchId}/{fileIdx}, see {@link #getFileInfo(String, String)}</li>
 * </ul>
 *
 * @since 7.4
 */
@WebObject(type = "upload")
public class BatchUploadObject extends AbstractResource<ResourceTypeImpl> {

    protected static final Log log = LogFactory.getLog(BatchUploadObject.class);

    protected static final String REQUEST_BATCH_ID = "batchId";

    protected static final String REQUEST_FILE_IDX = "fileIdx";

    @POST
    public Response initBatch() throws IOException {
        BatchManager bm = Framework.getService(BatchManager.class);
        String batchId = bm.initBatch();
        Map<String, String> result = new HashMap<String, String>();
        result.put("batchId", batchId);
        return buildResponse(Status.OK, result);
    }

    @POST
    @Path("{batchId}/{fileIdx}")
    public Response upload(@Context HttpServletRequest request, @PathParam(REQUEST_BATCH_ID) String batchId,
            @PathParam(REQUEST_FILE_IDX) String fileIdx) throws IOException {

        BatchManager bm = Framework.getService(BatchManager.class);
        if (!bm.hasBatch(batchId)) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }

        boolean isMultipart = false;

        // Parameters are passed as request header, the request body is the stream
        String contentType = request.getHeader("Content-Type");
        String uploadType = request.getHeader("X-Upload-Type");
        String contentLength = request.getHeader("Content-Length");
        String uploadChunk = request.getHeader("X-Upload-Chunk-Idx");
        String chunkCount = request.getHeader("X-Upload-Chunk-Count");
        String fileName = request.getHeader("X-File-Name");
        String fileSize = request.getHeader("X-File-Size");
        String mimeType = request.getHeader("X-File-Type");
        InputStream is = null;

        // Handle multipart case: mainly MSIE with jQueryFileupload
        if (contentType != null && contentType.contains("multipart")) {
            isMultipart = true;
            FormData formData = new FormData(request);
            Blob blob = formData.getFirstBlob();
            if (blob != null) {
                is = blob.getStream();
                fileName = blob.getFilename();
                mimeType = blob.getMimeType();
            }
        } else {
            fileName = URLDecoder.decode(fileName, "UTF-8");
            is = request.getInputStream();
        }

        // TODO https://jira.nuxeo.com/browse/NXP-16951
        // Handle chunked mode
        log.debug(String.format("Uploading %s (%sb)", fileName, fileSize));
        bm.addStream(batchId, fileIdx, is, fileName, mimeType);

        Map<String, String> result = new HashMap<String, String>();
        result.put("batchId", batchId);
        result.put("fileIdx", fileIdx);
        result.put("uploadType", uploadType);
        // TODO https://jira.nuxeo.com/browse/NXP-16951
        // Use effective uploaded size returned by bm
        result.put("uploadedSize", fileSize);
        // TODO https://jira.nuxeo.com/browse/NXP-16951
        // If chunked send 308 Resume Incomplete status (or 201 if last chunk) and put chunkCompletion in result
        return buildResponse(Status.CREATED, result, isMultipart);
    }

    @GET
    @Path("{batchId}")
    public Response getBatchInfo(@PathParam(REQUEST_BATCH_ID) String batchId) throws IOException {
        BatchManager bm = Framework.getService(BatchManager.class);
        if (!bm.hasBatch(batchId)) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }
        List<Blob> blobs = bm.getBlobs(batchId);
        if (blobs.isEmpty()) {
            return buildEmptyResponse(Status.NO_CONTENT);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        // TODO https://jira.nuxeo.com/browse/NXP-16951
        // Send chunking info
        for (Blob blob : blobs) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", blob.getFilename());
            map.put("size", blob.getLength());
            result.add(map);
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
        Blob blob = bm.getBlob(batchId, fileIdx);
        if (blob == null) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }
        Map<String, String> result = new HashMap<String, String>();
        result.put("uploadType", "normal");
        result.put("uploadedSize", String.valueOf(blob.getLength()));
        // TODO https://jira.nuxeo.com/browse/NXP-16951
        // If chunked send 308 Resume Incomplete status (or 200 if all chunks uploaded) and put "chunked"
        // uploadType + chunkCompletion in result instead of uploadedSize
        return buildResponse(Status.OK, result);
    }

    @DELETE
    @Path("{batchId}")
    public Response dropBatch(@PathParam(REQUEST_BATCH_ID) String batchId) throws IOException {
        BatchManager bm = Framework.getLocalService(BatchManager.class);
        if (!bm.hasBatch(batchId)) {
            return buildEmptyResponse(Status.NOT_FOUND);
        }
        bm.clean(batchId);
        Map<String, String> result = new HashMap<String, String>();
        result.put("batchId", batchId);
        result.put("dropped", "true");
        return buildResponse(Status.OK, result);
    }

    protected Response buildResponse(Status status, Object object) throws IOException {
        return buildResponse(status, object, false);
    }

    protected Response buildResponse(Status status, Object object, boolean html) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String result = mapper.writeValueAsString(object);
        if (html) {
            // For MSIE with iframe transport: we need to return HTML!
            return buildHTMLResponse(status, result);
        } else {
            return buildJSONResponse(status, result);
        }
    }

    protected Response buildJSONResponse(Status status, String message) {
        return Response.status(status).header("Content-Length", message.length()).type(MediaType.APPLICATION_JSON).entity(
                message).build();
    }

    protected Response buildHTMLResponse(Status status, String message) {
        message = "<html>" + message + "</html>";
        return Response.status(status).header("Content-Length", message.length()).type(MediaType.TEXT_HTML_TYPE).entity(
                message).build();
    }

    protected Response buildEmptyResponse(Status status) {
        return Response.status(status).build();
    }
}
