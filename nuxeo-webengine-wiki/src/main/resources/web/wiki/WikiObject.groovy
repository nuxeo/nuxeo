package wiki;

import java.util.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.*;
import org.nuxeo.ecm.core.api.impl.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;
import org.nuxeo.runtime.api.*;
import org.nuxeo.ecm.platform.usermanager.*;
import org.nuxeo.ecm.core.rest.*;

@WebObject(type="Wiki")
@Produces(["text/html", "*/*"])
public class WikiObject extends DocumentRoot {

  @GET @POST
  public Response getIndex(@QueryParam("query") String query, @QueryParam("group") String group) {
    System.out.println("aaaaaaaaaaaaaaaaaaa");  
    return redirect(path+"/FrontPage");
  }


}

