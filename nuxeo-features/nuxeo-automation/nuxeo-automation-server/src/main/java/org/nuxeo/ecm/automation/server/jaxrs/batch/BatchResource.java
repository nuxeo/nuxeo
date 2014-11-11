/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *     vpasquier <vpasquier@nuxeo.com>
 *
 */
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestCleanupHandler;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Exposes {@link Batch} as a JAX-RS resource
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @author Antoine Taillefer
 *
 */
@WebObject(type = "batch")
public class BatchResource extends AbstractResource<ResourceTypeImpl> {

    private static final String REQUEST_BATCH_ID = "batchId";

    private static final String REQUEST_FILE_IDX = "fileIdx";

    protected static final Log log = LogFactory.getLog(BatchResource.class);

    public CoreSession getCoreSession(HttpServletRequest request) {
        return SessionFactory.getSession(request);
    }

    protected Response buildFromString(String message) {
        return Response.ok(message, MediaType.APPLICATION_JSON).header(
                "Content-Length", message.length()).build();
    }

    protected Response buildHtmlFromString(String message) {
        message = "<html>" + message + "</html>";
        return Response.ok(message, MediaType.TEXT_HTML_TYPE).header(
                "Content-Length", message.length()).build();
    }

    protected Response buildFromMap(Map<String, String> map) throws Exception {
        return buildFromMap(map, false);
    }

    protected Response buildFromMap(Map<String, String> map, boolean html)
            throws Exception {
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
     *
     * @deprecated since 5.7.2. The timeout is managed by the
     *             {@link BatchManager#execute} method.
     */
    @Deprecated
    protected int getUploadWaitTimeout() {
        String t = Framework.getProperty("org.nuxeo.batch.upload.wait.timeout",
                "5");
        return Integer.parseInt(t);
    }

    @POST
    @Path("/upload")
    public Object doPost(@Context
    HttpServletRequest request) throws Exception {

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

        log.debug("uploaded " + fileName + " (" + fileSize + "b)");
        BatchManager bm = Framework.getLocalService(BatchManager.class);
        bm.addStream(batchId, idx, is, fileName, mimeType);

        Map<String, String> result = new HashMap<String, String>();
        result.put("batchId", batchId);
        result.put("uploaded", "true");
        return buildFromMap(result, useIFrame);
    }

    @POST
    @Produces("application/json")
    @Path("/execute")
    public Object exec(@Context
    HttpServletRequest request, ExecutionRequest xreq) throws Exception {

        Map<String, Object> params = xreq.getParams();
        String batchId = (String) params.get(REQUEST_BATCH_ID);
        String fileIdx = (String) params.get(REQUEST_FILE_IDX);
        String operationId = (String) params.get("operationId");
        params.remove(REQUEST_BATCH_ID);
        params.remove("operationId");

        final BatchManager bm = Framework.getLocalService(BatchManager.class);
        // register commit hook for cleanup
        request.setAttribute(REQUEST_BATCH_ID, batchId);
        RequestContext.getActiveContext(request).addRequestCleanupHandler(
                new RequestCleanupHandler() {
                    @Override
                    public void cleanup(HttpServletRequest req) {
                        String bid = (String) req.getAttribute(REQUEST_BATCH_ID);
                        bm.clean(bid);
                    }

                });

        try {
            OperationContext ctx = xreq.createContext(request,
                    getCoreSession(request));

            Object result;
            if (StringUtils.isEmpty(fileIdx)) {
                result = bm.execute(batchId, operationId,
                        getCoreSession(request), ctx, params);
            } else {
                result = bm.execute(batchId, fileIdx, operationId,
                        getCoreSession(request), ctx, params);
            }
            return ResponseHelper.getResponse(result, request);
        } catch (Exception e) {
            log.error("Error while executing automation batch ", e);
            if (WebException.isSecurityError(e)) {
                return Response.status(Status.FORBIDDEN).entity(
                        "{\"error\" : \"" + e.getMessage() + "\"}").build();
            } else {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity(
                        "{\"error\" : \"" + e.getMessage() + "\"}").build();
            }
        }
    }

    @GET
    @Path("/files/{batchId}")
    public Object getFilesBatch(@PathParam(REQUEST_BATCH_ID)
    String batchId) throws Exception {
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

    @GET
    @Path("/drop/{batchId}")
    public Object dropBatch(@PathParam(REQUEST_BATCH_ID)
    String batchId) throws Exception {
        BatchManager bm = Framework.getLocalService(BatchManager.class);
        bm.clean(batchId);

        Map<String, String> result = new HashMap<String, String>();
        result.put("batchId", batchId);
        result.put("dropped", "true");
        return buildFromMap(result);
    }

}
