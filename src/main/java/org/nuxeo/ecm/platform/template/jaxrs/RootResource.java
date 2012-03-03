package org.nuxeo.ecm.platform.template.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

@WebObject(type = "templateRoot")
@Path("/templates")
public class RootResource extends ModuleRoot{

    @GET
    public String index() {
        String sid = getContext().getCoreSession().getSessionId();
        return "ok :sid =" + sid;
    }
    
    @GET
    @Path("ping")
    public String getPong() {
        return "pong";
    }
    
    @Path("resources")
    public ResourceService getResourceService() {
        return new ResourceService(getContext().getCoreSession());
    }
    
    @Path("xdocresources")
    public XDocReportResourceService getXDocResourceService() {
        return new XDocReportResourceService(getContext().getCoreSession());
    }
    
    @Path("reports")
    public ReportService getReportService() {
        return new ReportService();
    }
    
}
