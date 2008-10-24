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
    
    
    @Path("{segment}")
    public DocumentObject getWiki(@PathParam("segment") String segment) {
      return newObject("Wiki", getDocument("/default-domain/workspaces/wikis/"+segment));
    }
    
  @GET
  public Object doGet() {
    return getView("index.ftl");
    //return "Wiki Index";
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

