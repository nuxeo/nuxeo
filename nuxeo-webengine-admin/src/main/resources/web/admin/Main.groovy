package admin;

import java.io.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;
import org.nuxeo.ecm.webengine.*;

@WebModule(name="admin", guard="user=Administrator", base="base")

@Path("/admin")
@Produces(["text/html", "*/*"])
public class Main extends DefaultModule {
  
  @Path("users")
  public Object getUserManagement() {
    return newObject("UserManager");
  }

  @Path("engine")  
  public Object getEngine() {
    return newObject("Engine");
  }
  

  @Path("repository")
  public Object getRepository() {
    return new DocumentRoot(ctx, "/default-domain");
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

