package wiki;

import java.io.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;
import org.nuxeo.ecm.webengine.*;
import org.nuxeo.ecm.core.api.*;


@WebModule(name="wiki")
@Path("/wikis")
@Produces(["text/html; charset=UTF-8", "*/*; charset=UTF-8"])
public class Main extends DocumentModule {

  public Main() {
     super("/");
     doc = getRootDocument();
  }

  public static DocumentModel getRootDocument(){
      try {
          // testing if exist, and create it if doesn't exist
          DocumentRef wikisPath = new PathRef("/default-domain/workspaces/wikis");
          def ctx = WebEngine.getActiveContext();
          CoreSession session = ctx.getCoreSession();
          if( ! session.exists(wikisPath)){
              DocumentModel newDoc = session.createDocumentModel("/default-domain/workspaces/", "wikis", "Workspace");
              if (newDoc.getTitle().length() == 0) {
                  newDoc.getPart("dublincore").get("title").setValue(newDoc.getName());
              }
              newDoc = session.createDocument(newDoc);
              session.save();
              return newDoc;
          }
          return WebEngine.getActiveContext().getCoreSession().getDocument(wikisPath);
       } catch(Exception e) {
           throw WebException.wrap(e);
      }
  }
  
  @GET
  public Object doGet() {
    def docs = ctx.getCoreSession().getChildren(doc.getRef(), "Wiki");
    return getView("index").arg("wikis", docs);
  }

  @Path("{segment}")
  public Object getWiki(@PathParam("segment") String segment) {
    System.out.println("segment path");
    return DocumentFactory.newDocument(ctx, doc.getPath().append(segment).toString());
  }

  @GET
  @Path("create/{segment}")
  public Response createPage(@PathParam("segment") String segment) {
      System.out.println("segment path");
    try{
      def session = ctx.getCoreSession();
      def newDoc = session.createDocumentModel("/default-domain/workspaces/", segment, "Workspace");
      if (newDoc.getTitle().length() == 0) {
        newDoc.getPart("dublincore").get("title").setValue(newDoc.getName());
      }
      newDoc = session.createDocument(newDoc);
      session.save();
      return redirect(path + "/" + segment);
    } catch(Exception e) {
      throw WebException.wrap(e);
    }
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
}

