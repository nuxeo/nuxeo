package org.nuxeo.template.xdocreport.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
@WebObject(type = "xdocRestRoot")
@Path("/xdoctemplates")
public class RootResource extends ModuleRoot {

    @GET
    public String index() {
        String sid = getContext().getCoreSession().getSessionId();
        return "ok :sid =" + sid;
    }

    protected CoreSession getCoreSession() {
        TransactionHelper.startTransaction();

        return getContext().getCoreSession();
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
