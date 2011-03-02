package org.nuxeo.opensocial.container.server.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

/**
 * @author Stéphane Fourrier
 */
@Path("/container")
@WebObject(type = "containerRoot")
public class ContainerRoot extends ModuleRoot {
    @GET
    public Object doGet() {
        return getView("index");
    }

    @Path("/browser/{id}")
    public Object doBrowse(@PathParam("id") String id) throws ClientException {
        IdRef ref = new IdRef(id);
        CoreSession session = ctx.getCoreSession();

        if (session.exists(ref)) {
            return newObject("folderWebObject", session.getDocument(ref));
        }

        return Response.status(Status.NOT_FOUND).build();
    }
}
