/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *     vpasquier <vpasquier@nuxeo.com>
 *
 */
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.jaxrs.io.operations.ExecutionRequest;
import org.nuxeo.ecm.automation.server.jaxrs.ResponseHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestCleanupHandler;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Exposes {@link Batch} as a JAX-RS resource
 *
 * @deprecated since 7.4, use {@link org.nuxeo.ecm.restapi.server.jaxrs.BatchUploadObject} instead.
 * @author Tiry (tdelprat@nuxeo.com)
 * @author Antoine Taillefer
 */
@Deprecated
@WebObject(type = "batch")
public class BatchResource extends AbstractResource<ResourceTypeImpl> {

    private static final String REQUEST_BATCH_ID = "batchId";

    private static final String REQUEST_FILE_IDX = "fileIdx";

    protected static final Log log = LogFactory.getLog(BatchResource.class);

    public CoreSession getCoreSession(HttpServletRequest request) {
        return SessionFactory.getSession(request);
    }

    protected Response buildFromString(String message) {
        return Response.ok(message, MediaType.APPLICATION_JSON).header("Content-Length", message.length()).build();
    }

    protected Response buildHtmlFromString(String message) {
        message = "<html>" + message + "</html>";
        return Response.ok(message, MediaType.TEXT_HTML_TYPE).header("Content-Length", message.length()).build();
    }

    protected Response buildFromMap(Map<String, String> map) throws IOException {
        return buildFromMap(map, false);
    }

    protected Response buildFromMap(Map<String, String> map, boolean html) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ByteArrayOutputStream out = new ByteArrayOutputStream(128);
        mapper.writeValue(out, map);
        String result = out.toString("UTF-8");
        if (html) {
            // for msie with iframe transport : we need to return html !
            return buildHtmlFromString(result);
        } else {
            return buildFromString(result);
        }
    }

    /**
     * @deprecated since 7.4, use {@link BatchUploadObject#upload(HttpServletRequest, String, String)} instead.
     */
    @Deprecated
    @POST
    @Path("/upload")
    public Object doPost(@Context HttpServletRequest request) throws IOException {
        TransactionHelper.commitOrRollbackTransaction();
        try {
            return uploadNoTransaction(request);
        } finally {
            TransactionHelper.startTransaction();
        }
    }

    protected Object uploadNoTransaction(@Context HttpServletRequest request) throws IOException {
        boolean useIFrame = false;

        // Parameters are passed as request header
        // the request body is the stream
        String fileName = request.getHeader("X-File-Name");
        String fileSize = request.getHeader("X-File-Size");
        String batchId = request.getHeader("X-Batch-Id");
        String mimeType = request.getHeader("X-File-Type");
        String idx = request.getHeader("X-File-Idx");
        InputStream is = null;

        // handle multipart case : mainly MSIE with jQueryFileupload
        String contentType = request.getHeader("Content-Type");
        if (contentType != null && contentType.contains("multipart")) {
            useIFrame = true;
            FormData formData = new FormData(request);
            if (formData.getString("batchId") != null) {
                batchId = formData.getString("batchId");
            }
            if (formData.getString("fileIdx") != null) {
                idx = formData.getString("fileIdx");
            }
            if (idx == null || "".equals(idx.trim())) {
                idx = "0";
            }
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

        log.debug("Uploading " + fileName + " (" + fileSize + "b)");
        BatchManager bm = Framework.getLocalService(BatchManager.class);
        if (StringUtils.isEmpty(batchId)) {
            batchId = bm.initBatch();
        } else if (!bm.hasBatch(batchId)) {
            if (!Framework.getService(ConfigurationService.class)
                          .isBooleanPropertyTrue(BatchManagerComponent.CLIENT_BATCH_ID_FLAG)) {
                String errorMsg = String.format(
                        "Cannot upload a file with a client-side generated batch id, please use new upload API or set configuration property %s to true (not recommended)",
                        BatchManagerComponent.CLIENT_BATCH_ID_FLAG);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                               .entity("{\"error\" : \"" + errorMsg + "\"}")
                               .build();
            } else {
                log.warn(String.format(
                        "Allowing to initialize upload batch with a client-side generated id since configuration property %s is set to true but this is not recommended, please use new upload API instead",
                        BatchManagerComponent.CLIENT_BATCH_ID_FLAG));
            }
        }
        bm.addStream(batchId, idx, is, fileName, mimeType);

        Map<String, String> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("uploaded", "true");
        return buildFromMap(result, useIFrame);
    }

    @Deprecated
    @POST
    @Produces("application/json")
    @Path("/execute")
    public Object exec(@Context HttpServletRequest request, ExecutionRequest xreq) {
        Map<String, Object> params = xreq.getParams();
        String batchId = (String) params.get(REQUEST_BATCH_ID);
        String fileIdx = (String) params.get(REQUEST_FILE_IDX);
        String operationId = (String) params.get("operationId");
        params.remove(REQUEST_BATCH_ID);
        params.remove("operationId");

        final BatchManager bm = Framework.getLocalService(BatchManager.class);
        // register commit hook for cleanup
        request.setAttribute(REQUEST_BATCH_ID, batchId);
        RequestContext.getActiveContext(request).addRequestCleanupHandler(req -> {
            String bid = (String) req.getAttribute(REQUEST_BATCH_ID);
            bm.clean(bid);
        });

        try {
            OperationContext ctx = xreq.createContext(request, getCoreSession(request));

            Object result;
            if (StringUtils.isEmpty(fileIdx)) {
                result = bm.execute(batchId, operationId, getCoreSession(request), ctx, params);
            } else {
                result = bm.execute(batchId, fileIdx, operationId, getCoreSession(request), ctx, params);
            }
            return ResponseHelper.getResponse(result, request);
        } catch (NuxeoException | MessagingException | IOException e) {
            log.error("Error while executing automation batch ", e);
            if (WebException.isSecurityError(e)) {
                return Response.status(Status.FORBIDDEN).entity("{\"error\" : \"" + e.getMessage() + "\"}").build();
            } else {
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                               .entity("{\"error\" : \"" + e.getMessage() + "\"}")
                               .build();
            }
        }
    }

    /**
     * @deprecated since 7.4, use {@link BatchUploadObject#getBatchInfo(String)} instead.
     */
    @Deprecated
    @GET
    @Path("/files/{batchId}")
    public Object getFilesBatch(@PathParam(REQUEST_BATCH_ID) String batchId) throws IOException {
        BatchManager bm = Framework.getLocalService(BatchManager.class);
        List<Blob> blobs = bm.getBlobs(batchId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Blob blob : blobs) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", blob.getFilename());
            map.put("size", blob.getLength());
            result.add(map);
        }

        ObjectMapper mapper = new ObjectMapper();
        ByteArrayOutputStream out = new ByteArrayOutputStream(128);
        mapper.writeValue(out, result);
        return buildFromString(out.toString("UTF-8"));
    }

    /**
     * @deprecated since 7.4, use {@link BatchUploadObject#dropBatch(String)} instead.
     */
    @Deprecated
    @GET
    @Path("/drop/{batchId}")
    public Object dropBatch(@PathParam(REQUEST_BATCH_ID) String batchId) throws IOException {
        BatchManager bm = Framework.getLocalService(BatchManager.class);
        bm.clean(batchId);

        Map<String, String> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("dropped", "true");
        return buildFromMap(result);
    }

}
