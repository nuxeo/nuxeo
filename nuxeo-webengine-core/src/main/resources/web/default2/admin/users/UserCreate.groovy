package app1.user;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.*;


@WebService(name="create", targetTypes=["User"], targetFacets=["rocker"])
@Produces(["text/html", "*/*"])
public class UserCreate extends DefaultService {

  @GET  
  @Path("info")
  public String getInfo() {
    return "Creating a new user";
  }

  @GET
  @WebView(name="view", guard="user=Guest")
  public Object doGet() {
    return "teeeeeeest";
  }

  
}

