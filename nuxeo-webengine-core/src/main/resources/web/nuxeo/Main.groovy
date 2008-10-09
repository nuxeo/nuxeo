package nuxeo;

import java.io.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.exceptions.*;
import org.nuxeo.ecm.webengine.*;

@WebModule(name="default")
@Path("/")
@Produces(["text/html", "*/*"])
public class Main extends WebApplication {
  
  @Path("admin")
  public Object getAdmin() {
    return ctx.newObject("Admin");
  }

  @Path("repository")
  public Object getRepository() {
    return new DocumentRoot(ctx, "/default-domain");
  }

  @GET
  public Object getIndex() {
    return new Template(ctx).fileName("index.ftl")
  }

  @GET
  @Path("help")
  public Object getHelp() {
    return new Template(ctx).fileName("help/help.ftl")
  }

  @GET
  @Path("about")
  public Object getAbout() {
    return new Template(ctx).fileName("help/about.ftl")
  }

  // handle errors
  public Object getErrorView(WebApplicationException e) {
    if (e instanceof WebSecurityException) {
      return Response.status(401).entity(new Template(ctx, getFile("error/error_401.ftl"))).build();
    } else {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      pw.close();
      return Response.status(500).entity(sw.toString()).build();
    }
  }
  
}

