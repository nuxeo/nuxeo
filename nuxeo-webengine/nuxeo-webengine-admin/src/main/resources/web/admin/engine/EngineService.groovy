package admin.engine;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;
import org.nuxeo.runtime.api.*;
import org.nuxeo.ecm.platform.usermanager.*;

@WebObject(type="Engine")
@Produces(["text/html", "*/*"])
public class EngineService extends DefaultObject {

  @GET
  public Object getIndex() {
    return getView("index");
  }

  @GET
  @Path("reload")
  public Response doReload() {
    ctx.getEngine().reload();
    return redirect(path);
  }

}

