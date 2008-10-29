package sample7;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.core.rest.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;

/**
 * Managing links.
 *
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebModule(name="sample7")
@Path("/sample7")
@Produces(["text/html", "*/*"])
public class Main extends DefaultModule {

  /**
   * Get the index page
   */
  @GET
  public Object docGet() {
    return getView("index");
  }

}

