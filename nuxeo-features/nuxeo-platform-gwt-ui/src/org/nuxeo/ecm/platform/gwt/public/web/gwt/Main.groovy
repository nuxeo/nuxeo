package gwt;

import java.io.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;
import org.nuxeo.ecm.webengine.*;
import org.nuxeo.ecm.webengine.util.GWTHelper;
import org.nuxeo.ecm.core.api.*;
import net.sf.json.*;


@WebModule(name="gwt", guard="user=Administrator", base="base")
@Path("/gwt")
@Produces(["text/html", "*/*"])
public class Main extends DefaultModule {
  

  @Path("repository")
  public Object getRepository() {
    return new DocumentRoot(ctx, "/");
  }

  @GET
  public Object getIndex() {
    return getView("index");
  }

  @GET
  @Path("help")
  public Object getHelp() {
    return getTemplate("help/help.ftl");
  }

  @GET
  @Path("about")
  public Object getAbout() {
    return getTemplate("help/about.ftl");
  }

  public DocumentRef getDocumentRef(String ref) {
    return ref.startsWith("/") ? new PathRef(ref) : new IdRef(ref);
  }

  @GET
  @Path("tree")
  public String getTree(@QueryParam("parentId") String parent) {
    String result = null;
    try {
      System.out.println("getFolders for "+  parent);
      CoreSession session = ctx.getCoreSession();
      DocumentModel doc = null;
      if (parent == null || parent.length() == 0) {
        doc = session.getRootDocument();
      } else {
        def ref = getDocumentRef(parent);
        doc = session.getDocument(ref);
      }
      result = dataSourceResponse(GWTHelper.getChildrenFolders(ctx.getCoreSession(), doc, parent)).toString();
    } catch (Exception e) {
      e.printStackTrace();
      WebException we = WebException.wrap(e);
      if (we instanceof WebSecurityException) {
        result = dataSourceError(-7, "Authentication Required");
      } else {
        result = dataSourceError(-1, e.getMessage());
      }
    }
    System.out.println("> "+result);
    return result;
  }

  @GET
  @Path("files")
  public String getFiles(@QueryParam("parentId") String parent) {
    String result = null;
    try {
      System.out.println("getFiles for "+  parent);
      CoreSession session = ctx.getCoreSession();
      DocumentModel doc = null;
      if (parent == null || parent.length() == 0) {
        doc = session.getRootDocument();
      } else {
        def ref = getDocumentRef(parent);
        doc = session.getDocument(ref);
      }
      result = dataSourceResponse(GWTHelper.getChildrenFiles(ctx.getCoreSession(), doc, parent)).toString();
    } catch (Exception e) {
      e.printStackTrace();
      WebException we = WebException.wrap(e);
      if (we instanceof WebSecurityException) {
        result = dataSourceError(-7, "Authentication Required");
      } else {
        result = dataSourceError(-1, e.getMessage());
      }
    }
    System.out.println("> "+result);
    return result;
  }

  @GET
  @Path("doc")
  public String getDocument(@QueryParam("ref") String ref) {
    String result = null;
    System.out.println("getDocument for "+  ref);
    try {
      CoreSession session = ctx.getCoreSession();
      def docRef = getDocumentRef(ref);
      def doc = session.getDocument(docRef);
      def obj = GWTHelper.doc2JSon(doc);
      JSONArray ar = new JSONArray();
      ar.element(obj);
      result = dataSourceResponse(ar);
    } catch (Exception e) {
      e.printStackTrace();
      WebException we = WebException.wrap(e);
      if (we instanceof WebSecurityException) {
        result = dataSourceError(-7, "Authentication Required");
      } else {
        result = dataSourceError(-1, e.getMessage());
      }  
    }
    System.out.println("> "+result);
    return result;
  }

  // handle errors
  public Object handleError(WebApplicationException e) {
    if (e instanceof WebSecurityException) {
      return Response.status(401).entity(getTemplate("error/error_401.ftl")).build();
    } else if (e instanceof WebResourceNotFoundException) {
      return Response.status(404).entity(getTemplate("error/error_404.ftl")).build();
    } else {
      return super.handleError(e);
    }
  }

    public static JSONObject dataSourceResponse(JSONArray data) {
        return new JSONObject().element("response", new JSONObject().element("status", 0).element("data", data));
    }
    
    public static JSONObject dataSourceError(int status, String ... errors) {
        return new JSONObject().element("response", new JSONObject().element("status", status).element("data", JSONArray.fromObject(errors)));
    }
  
}

