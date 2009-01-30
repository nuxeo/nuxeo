package org.nuxeo.webengine.sites;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

@WebObject(type = "site", guard = "user=Administrator", facets={"Site"})
@Produces("text/html; charset=UTF-8")
public class Site extends DefaultObject {
    String url;

    DocumentModel ws = null;

    public void initialize(Object... args) {
        assert args != null && args.length == 1;
        url = (String) args[0];
        ws = getWorkspaceByUrl(url);
    }

    @GET
    public Object doGet() {
    	ctx.getRequest().setAttribute("org.nuxeo.theme.theme", "sites/default"); 
        if (ws == null) {
            return getTemplate("no_site.ftl").arg("url", url);
        }
        return getSiteTemplate(ws).args(getSiteArgs(ws));
    }

    @Path("{page}")
    public Object doGet(@PathParam("page") String page) {
    	ctx.getRequest().setAttribute("org.nuxeo.theme.theme", "sites/workspace"); 
        try {
            DocumentModel pageDoc = ctx.getCoreSession().getChild(ws.getRef(),
                    page);
            return (DocumentObject) ctx.newObject(pageDoc.getType(), pageDoc);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @GET
    @Path("logo")
    public Response getLogo() {
        try {
            Blob blob = (Blob) SiteHelper.getBlob(ws, "webc:logo");
            return Response.ok().entity(blob).type(blob.getMimeType()).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // TODO return a default image
        return null;
    }

    @GET
    @Path("welcomeMedia")
    public Response getWelcomeMedia() {
        try {
            Blob blob = (Blob) SiteHelper.getBlob(ws, "webc:welcomeMedia");
            return Response.ok().entity(blob).type(blob.getMimeType()).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // TODO return a default image
        return null;
    }

    protected Template getSiteTemplate(DocumentModel doc) {
        String siteType = SiteHelper.getString(doc, "webc:template", null);
        // TODO make this configurable
        if ("wiki".equals(siteType)) {
            return getTemplate("template_wiki.ftl");
        }
        if ("blog".equals(siteType)) {
            return getTemplate("template_blog.ftl");
        }
        return getTemplate("template_default.ftl");
    }

    protected Map<String, Object> getSiteArgs(DocumentModel doc) {
        Map<String, Object> root = new HashMap<String, Object>();
        root.put("welcomeText", SiteHelper.getString(doc, "webc:welcomeText",
                null));
        return root;
    }

    protected DocumentModel getWorkspaceByUrl(String url) {
        WebContext context = WebEngine.getActiveContext();
        CoreSession session = context.getCoreSession();
        try {
            DocumentModelList list = session.query(String.format(
                    "SELECT * FROM Workspace WHERE webc:url = \"%s\"", url));
            // DocumentModelList list =
            // session.query(String.format("SELECT * FROM Workspace ", url));
            if (list.size() != 0) {
                return list.get(0);
            }
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DocumentModel getWorkspace(){
        return ws;
    }



}
