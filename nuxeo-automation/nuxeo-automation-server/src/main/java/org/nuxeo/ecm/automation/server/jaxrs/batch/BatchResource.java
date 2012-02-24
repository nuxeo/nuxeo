/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */
package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.io.InputStream;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.server.jaxrs.ExceptionHandler;
import org.nuxeo.ecm.automation.server.jaxrs.ExecutionRequest;
import org.nuxeo.ecm.automation.server.jaxrs.ResponseHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestCleanupHandler;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * Exposes {@link Batch} as a JAX-RS resource
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class BatchResource {

    private static final String REQUEST_BATCH_ID = "batchId";

    protected static final Log log = LogFactory.getLog(BatchResource.class);

    public CoreSession getCoreSession(HttpServletRequest request) {
        return SessionFactory.getSession(request);
    }

    @POST
    @Produces("text/html")
    @Path("upload")
    public Object doPost(@Context HttpServletRequest request) throws Exception {
        String fileName = request.getHeader("X-File-Name");
        String fileSize = request.getHeader("X-File-Size");
        String batchId = request.getHeader("X-Batch-Id");
        String mimeType = request.getHeader("X-File-Type");
        String idx = request.getHeader("X-File-Idx");

        fileName = URLDecoder.decode(fileName, "UTF-8");
        InputStream is = request.getInputStream();
        log.debug("uploaded " + fileName + " (" + fileSize + "b)");

        BatchManager bm = Framework.getLocalService(BatchManager.class);
        bm.addStream(batchId, idx, is, fileName, mimeType);
        return "uploaded";
    }

    @POST
    @Produces("application/json")
    @Path("execute")
    public Object exec(@Context HttpServletRequest request,
            ExecutionRequest xreq) throws Exception {

        Map<String, Object> params = xreq.getParams();
        String batchId = (String) params.get(REQUEST_BATCH_ID);
        String operationId = (String) params.get("operationId");
        params.remove(REQUEST_BATCH_ID);
        params.remove("operationId");

        BatchManager bm = Framework.getLocalService(BatchManager.class);

        List<Blob> blobs = bm.getBlobs(batchId);
        xreq.setInput(new BlobList(blobs));

        OperationContext ctx = xreq.createContext(request,
                getCoreSession(request));
        AutomationService as = Framework.getLocalService(AutomationService.class);

        request.setAttribute(REQUEST_BATCH_ID, batchId);

        // register commit hook for cleanup
        RequestContext.getActiveContext(request).addRequestCleanupHandler(
                new RequestCleanupHandler() {
                    @Override
                    public void cleanup(HttpServletRequest req) {
                        String bid = (String) req.getAttribute(REQUEST_BATCH_ID);
                        BatchManager bm = Framework.getLocalService(BatchManager.class);
                        bm.clean(bid);
                    }

                });
        try {
            Object result=null;
            if (operationId.startsWith("Chain.")) {
                // Copy params in the Chain context
                ctx.putAll(xreq.getParams());
                result = as.run(ctx, operationId.substring(6));
            } else {
                OperationChain chain = new OperationChain("operation");
                OperationParameters oparams = new OperationParameters(
                        operationId, params);
                chain.add(oparams);
                result = as.run(ctx, chain);
            }
            if ("true".equals(request.getHeader("X-NXVoidOperation"))) {
                return ResponseHelper.emptyContent(); // void response
            } else {
                return result;
            }
        } catch (Exception e) {
            log.error("Error while executing automation batch ", e);
            if (ExceptionHandler.isSecurityError(e)) {
                return Response.status(Status.FORBIDDEN).entity("{\"error\" : \"" + e.getMessage() + "\"}").build();
            }
            else {
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("{\"error\" : \"" + e.getMessage() + "\"}").build();
            }
        }
    }

    @GET
    @Produces("text/html")
    @Path("drop/{batchId}")
    public String dropBatch(@PathParam(REQUEST_BATCH_ID) String batchId)
            throws Exception {
        BatchManager bm = Framework.getLocalService(BatchManager.class);
        bm.clean(batchId);
        return "Batch droped";
    }

}
