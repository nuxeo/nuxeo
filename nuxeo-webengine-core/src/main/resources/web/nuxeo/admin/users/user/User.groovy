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
    return getView("index.ftl").arg("user", principal);
  }

  @POST
  public Object doPost() {
    
  }

  @PUT
  public Object doPut() {
    
  }

  
}

