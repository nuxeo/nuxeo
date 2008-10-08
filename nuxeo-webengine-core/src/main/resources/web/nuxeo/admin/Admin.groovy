package nuxeo.admin;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.*;

@WebObject(name="Admin", facets=["domain"], guard="user=Administrator")
@Produces(["text/html", "*/*"])
public class Admin extends DefaultObject {

  @GET
  @Path("users")  
  public Object getUserManagement() {
    return ctx.newService(this, "UserService");
  }

  @GET
  public Object getIndex() {
    return new Template(this).fileName("index.ftl"); 
  }

}

