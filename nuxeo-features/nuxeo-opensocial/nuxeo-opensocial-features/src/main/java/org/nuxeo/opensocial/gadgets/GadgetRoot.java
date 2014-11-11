package org.nuxeo.opensocial.gadgets;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

@WebObject(type = "GadgetDocumentRoot")
public class GadgetRoot extends ModuleRoot {

    @GET
    public Object noRootRessource() {
        return Response.serverError();
    }

    @Path("{gadgetId}")
    public Object doGetGadget(@PathParam("gadgetId") String gadgetId)
            throws ClientException {

        CoreSession session = getContext().getCoreSession();
        IdRef ref = new IdRef(gadgetId);
        if (session.exists(ref)) {
            DocumentModel doc = session.getDocument(ref);
            return newObject("GadgetDocument", doc);
        } else {
            throw new WebResourceNotFoundException("Gadget not found");
        }
    }
}
