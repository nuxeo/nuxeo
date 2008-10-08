package admin;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.*;

@WebObject(name="Admin", facets=["domain"])
@Produces(["text/html", "*/*"])
public class Admin extends DefaultObject {

  @GET
  @Path("users")
  @WebView(name="users")
  public Object getUserManagement() {
    return null; // the interceptor will return the result
  }

  @GET
  @WebView(name="index")
  public Object getIndex() {
    return null; // handled by interceptor
  }

}

