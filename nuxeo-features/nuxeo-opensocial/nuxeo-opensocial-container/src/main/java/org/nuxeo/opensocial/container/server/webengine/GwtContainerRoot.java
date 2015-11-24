package org.nuxeo.opensocial.container.server.webengine;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.webengine.gwt.GwtResource;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @author Stéphane Fourrier
 */
@Path("/gwt-container")
@WebObject(type = "GwtContainerRoot")
public class GwtContainerRoot extends GwtResource {
    @GET
    @Produces("text/html")
    public Object getIndex() {
        return Response.status(404).build();
    }
}
