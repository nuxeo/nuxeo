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
@Path("/")
@Produces(["text/html", "*/*"])

public class Main extends DefaultModule {

  public DocumentModel getDocument(String path) {
    try {
        PathRef pathRef = new PathRef(path);
	return ctx.getCoreSession().getDocument(pathRef);
    } catch (Exception e) {
        throw WebException.wrap(e);
    }
  }
    
  @GET
  public Object doGet() {
    return getView("index");
  } 
  
  @Path("wikis")
  public DocumentObject wikis(){
    try{
      return new DocumentRoot(ctx, "/default-domain/workspaces/wikis/");
    }
    catch(Exception e){
      throw WebException.wrap(e);
    }
  }
  
  @GET
  @Path("create/{segment}")
  public Response createPage(@PathParam("segment") String segment) {
    try{
    def session = ctx.getCoreSession();
    def newDoc = session.createDocumentModel("/default-domain/workspaces/", segment, "Workspace");
    if (newDoc.getTitle().length() == 0) {
      newDoc.getPart("dublincore").get("title").setValue(newDoc.getName());
    }
    newDoc = session.createDocument(newDoc);
    session.save();
    return redirect(path+"/"+segment);
    }
    catch(Exception e){
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

/**
 * Get only the wikis object from the list
 */
class WikiFilter implements Filter{
  public boolean accept(DocumentModel doc) {
    return "Wiki".equals(doc.getType());
  }
}
    



