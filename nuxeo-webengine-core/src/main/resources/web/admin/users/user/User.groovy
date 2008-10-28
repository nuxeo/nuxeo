package nuxeo.admin.users.user;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.*;
import org.nuxeo.runtime.api.*;
import org.nuxeo.ecm.platform.usermanager.*;

@WebObject(type="User")
@Produces(["text/html", "*/*"])
public class User extends DefaultObject {

  def principal;

  protected void initialize(Object ... args) {
    assert args != null && args.length > 0;
    principal = args[0];
  }

  @GET  
  public Object doGet() {
    return getView("index").arg("user", principal);
  }

  @POST
  public Response doPost() {
    return redirect(getPrevious().getPath());
  }

  @PUT
  public Response doPut() {
    def userManager = Framework.getService(UserManager.class);
    def req = ctx.getRequest();
            // update
            principal.firstName = req.getParameter("firstName")
            principal.lastName = req.getParameter("lastName")
            principal.password = req.getParameter("password")
            
            def selectedGroups = req.getParameterValues("groups")
            def listGroups = Arrays.asList(selectedGroups)
            principal.setGroups(listGroups)
            
            userManager.updatePrincipal(principal)        
    return redirect(getPath());
  }

  @DELETE
  public Response doDelete() {
    def userManager = Framework.getService(UserManager.class)
    userManager.deletePrincipal(principal)    
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

