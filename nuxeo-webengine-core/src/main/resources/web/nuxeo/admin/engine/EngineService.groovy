package nuxeo.admin.engine;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;
import org.nuxeo.ecm.webengine.*;
import org.nuxeo.ecm.webengine.model.exceptions.*;
import org.nuxeo.runtime.api.*;
import org.nuxeo.ecm.platform.usermanager.*;

@WebAdapter(name="engine", targetType="Admin")
@Produces(["text/html", "*/*"])
public class EngineService extends DefaultAdapter {

  @GET
  public Object getIndex() {
    return getView("index.ftl");
  }

  @GET
  @Path("reload")
  public Response doReload() {
    ctx.getEngine().reload();
    return redirect(path);
  }

}

