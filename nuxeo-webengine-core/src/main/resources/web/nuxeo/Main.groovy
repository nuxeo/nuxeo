package nuxeo;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
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
  
}

