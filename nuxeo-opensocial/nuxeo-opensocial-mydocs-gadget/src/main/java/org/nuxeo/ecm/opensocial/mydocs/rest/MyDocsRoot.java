package org.nuxeo.ecm.opensocial.mydocs.rest;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

@WebObject(type = "MyDocsRoot")
@Produces( { "text/html; charset=UTF-8" })
public class MyDocsRoot extends ModuleRoot {

    @GET
    public Object noRootRessource() {
        return Response.serverError();
    }

    @Path("/{gadgetid}")
    public Object getParentWorkspace(@PathParam("gadgetid") String gadgetId) {
        CoreSession session = ctx.getCoreSession();
        try {
            IdRef spaceRef = new IdRef(gadgetId);
            if (!session.exists(spaceRef)) {
                return Response.status(404);
            }

            List<DocumentModel> parents = session.getParentDocuments(spaceRef);
            Collections.reverse(parents);
            for (DocumentModel parent : parents) {
                if (parent.getType().equals("Workspace")) {
                    return newObject("JSONDocument", parent);
                }
            }
            return Response.serverError();

        } catch (ClientException e) {
            return Response.serverError();
        }

    }



}
