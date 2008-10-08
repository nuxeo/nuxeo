package nuxeo.admin.users;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.*;


@WebService(name="UserService", targetTypes=["Admin"])
@Produces(["text/html", "*/*"])
public class UserService extends DefaultService {

  @GET  
  public String getInfo() {
    return "I am the user service";
  }

  
}

