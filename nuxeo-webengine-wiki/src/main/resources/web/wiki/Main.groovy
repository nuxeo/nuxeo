package wiki;

import java.io.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;
import org.nuxeo.ecm.webengine.*;

@WebModule(name="wiki")
@Path("/")
@Produces(["text/html", "*/*"])

public class Main extends DefaultModule {

  @GET
  public Object getIndex() {
    return getTemplate("index.ftl");
  }  
}

