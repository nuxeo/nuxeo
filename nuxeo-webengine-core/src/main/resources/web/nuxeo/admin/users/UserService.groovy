package nuxeo.admin.users;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.*;
import org.nuxeo.runtime.api.*;
import org.nuxeo.ecm.platform.usermanager.*;

@WebService(name="users", targetTypes=["Admin"])
@Produces(["text/html", "*/*"])
public class UserService extends DefaultService {

  @GET  
  public Object getIndex() {
    return new Template(this).fileName("index.ftl").resolve();
  }

  @GET @POST
  @Path("search")
  public Object search(@QueryParam("query") String query, @QueryParam("group") String group) {
    return "ass@@@s33eewwwwwww waddd33";
  }

  @Path("test")
  @GET
  public Object test() {
  return "aaaaaaaaaaddddyyyyeeeeeyy";
  }

  
}

