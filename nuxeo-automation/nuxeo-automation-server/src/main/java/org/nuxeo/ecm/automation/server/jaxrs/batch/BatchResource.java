package org.nuxeo.ecm.automation.server.jaxrs.batch;

import java.io.InputStream;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.server.jaxrs.ExecutionRequest;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.ecm.webengine.jaxrs.views.TemplateView;
import org.nuxeo.ecm.webengine.jaxrs.views.View;
import org.nuxeo.runtime.api.Framework;

public class BatchResource {

    //@Context
    //protected HttpServletRequest request;

    public CoreSession getCoreSession(HttpServletRequest request) {
        return SessionFactory.getSession(request);
    }

    @POST
    @Produces("text/html")
    @Path(value = "upload")
    public Object doPost(@Context HttpServletRequest request) throws Exception {
        String fileName = request.getHeader("X-File-Name");
        String fileSize = request.getHeader("X-File-Size");
        String batchId = request.getHeader("X-Batch-Id");
        String idx = request.getHeader("X-File-Idx");

        InputStream is = request.getInputStream();

        System.out.println(" uploaded " + fileName + " (" + fileSize + "b)");

        BatchManager bm = Framework.getLocalService(BatchManager.class);
        bm.addStream(batchId, idx, is, fileName, null); // XXX Mime Type
        return "uploaded";
    }


    @GET
    @Produces("text/html")
    @Path(value = "chooseOperation/{batchId}")
    public View getOperationChoice(@PathParam("batchId") String batchId, @QueryParam("context") String context) throws Exception {

        List<OperationDocumentation> operations = new ArrayList<OperationDocumentation>();

        AutomationService as = Framework.getLocalService(AutomationService.class);

        for (OperationDocumentation od : as.getDocumentation()) {
            for (int i = 0; i < od.signature.length; i += 2) {
                if ("bloblist".equals(od.signature[i])) {
                    operations.add(od);
                }
            }
        }

        if (context!=null && !context.isEmpty()) {
         // XXX operations list should depend on the context (i.e. where the drop occurs)
        }
        // XXX Sort to put the default operation on top

        //return new TemplateView(this,"operationSelector.ftl").arg("operations", operations).arg("batchId", batchId).arg("context", context);
        return new TemplateView(this,"operationDescriptor.ftl").arg("operations", operations);
    }

    @POST
    @Produces("application/json")
    @Path(value = "execute")
    public Object exec(@Context HttpServletRequest request, ExecutionRequest xreq) throws Exception {

        Map<String, Object> params = xreq.getParams();
        String batchId = (String)params.get("batchId");
        String operationId = (String)params.get("operationId");
        params.remove("batchId");
        params.remove("operationId");

        BatchManager bm = Framework.getLocalService(BatchManager.class);

        List<Blob> blobs = bm.getBlobs(batchId);
        xreq.setInput(new BlobList(blobs));

        OperationContext ctx = xreq.createContext(request, getCoreSession(request));
        AutomationService as = Framework.getLocalService(AutomationService.class);

        try {
            if (operationId.startsWith("Chain.")) {
                return as.run(ctx, operationId.substring(6));
            } else {
                OperationChain chain = new OperationChain("operation");
                OperationParameters oparams = new OperationParameters(operationId,params);
                chain.add(oparams);
                return as.run(ctx, chain);
            }
        }
        catch (Exception e) {
            return "{ error:'" + e.getMessage() + "'}";
        }
        finally {
            bm.clean(batchId); // XXX should move to a commit hook
        }
    }

}
