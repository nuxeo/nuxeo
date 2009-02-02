package org.nuxeo.ecm.webengine.admin;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.nuxeo.ecm.webengine.model.*;
import org.nuxeo.ecm.webengine.model.impl.*;

@WebObject(type = "Engine")
@Produces("text/html; charset=UTF-8")
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
