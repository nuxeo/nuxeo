package base;

import java.io.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;
import org.nuxeo.ecm.webengine.*;
import org.nuxeo.ecm.core.api.*;

@WebModule(name="base")
@Path("/")
@Produces(["text/html; charset=UTF-8", "*/*; charset=UTF-8"])
public class Main extends DefaultModule {
    
  @GET
  public Object doGet() {
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
