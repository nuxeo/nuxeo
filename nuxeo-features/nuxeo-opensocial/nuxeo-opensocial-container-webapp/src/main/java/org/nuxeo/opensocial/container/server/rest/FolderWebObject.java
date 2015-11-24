package org.nuxeo.opensocial.container.server.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.webengine.model.WebObject;

import com.google.gson.Gson;

/**
 * @author Stéphane Fourrier
 */
@WebObject(type = "folderWebObject")
public class FolderWebObject extends DocumentObject {

    private static final Log log = LogFactory.getLog(FolderWebObject.class);

    @GET
    @Produces("text/json")
    @Path("gadgetChildren")
    public Object doGetGadgetChildrenInParentWs(@QueryParam("type") String type)
            throws ClientException {
        final String logPrefix = "<doGetGadgetChildrenInParentWs>";
        CoreSession session = ctx.getCoreSession();

        DocumentModel parentWorkspace = getParentWorkspace(doc);
        log.debug(logPrefix + doc);
        DocumentModelList list = session.getChildren(parentWorkspace.getRef(),
                type);
        Gson gson = new Gson();
        String response = gson.toJson(new FoldersListGson(list, session));
        return response;
    }

    private DocumentModel getParentWorkspace(DocumentModel doc)
            throws ClientException {
        DocumentModel parentDocument = ctx.getCoreSession().getParentDocument(
                doc.getRef());
        if ("Workspace".equals(parentDocument.getType())) {
            return parentDocument;
        } else {
            return getParentWorkspace(parentDocument);
        }
    }

}
