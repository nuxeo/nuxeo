package org.nuxeo.ecm.webengine.admin;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

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
