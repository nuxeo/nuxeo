package nuxeo.admin.users.group;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.*;
import org.nuxeo.runtime.api.*;
import org.nuxeo.ecm.platform.usermanager.*;

@WebObject(type="Group")
@Produces(["text/html", "*/*"])
public class UserService extends DefaultObject {

  def principal;

  protected void initialize(Object ... args) {
    assert args != null && args.length > 0;
    principal = args[0];
  }

  @GET  
  public Object doGet() {
    return getView("index").arg("group", principal);
  }


  @POST
  public Response doPost() {
    return redirect(getPrevious().getPath());
  }

  @PUT
  public Response doPut() {
    return redirect(getPath());
  }

  @DELETE
  public Response doDelete() {
    def userManager = Framework.getService(UserManager.class)
    userManager.deleteGroup(principal)
    return redirect(getPrevious().getPath());
  }

  @POST
  @Path("@put")
  public Response simulatePut() {
    return doPut();
  }

  @GET
  @Path("@delete")
  public Response simulateDelete() {
    return doDelete();
  }

}

