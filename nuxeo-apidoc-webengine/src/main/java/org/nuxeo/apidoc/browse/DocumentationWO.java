package org.nuxeo.apidoc.browse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.documentation.DocumentationService;
import org.nuxeo.apidoc.search.ArtifactSearcher;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "documentation")
public class DocumentationWO extends DefaultObject {

    @GET
    @Produces("text/html")
    public Object viewAll() throws Exception {
        DocumentationService ds = Framework.getLocalService(DocumentationService.class);
        Map<String, List<DocumentationItem>> docs = ds.listDocumentationItems(getContext().getCoreSession(), null);
        return getView("index").arg("distId", ctx.getProperty("distId")).arg("docsByCat", docs);
    }


    @GET
    @Produces("text/html")
    @Path(value = "filter")
    public Object filterAll() throws Exception {
        String fulltext = getContext().getForm().getFormProperty("fulltext");
        DocumentationService ds = Framework.getLocalService(DocumentationService.class);

        ArtifactSearcher searcher = Framework.getLocalService(ArtifactSearcher.class);
        List<DocumentationItem> items = searcher.searchDocumentation(getContext().getCoreSession(), fulltext, null);
        Map<String, String> categories = ds.getCategories();

        Map<String, List<DocumentationItem>> docs = new HashMap<String, List<DocumentationItem>>();

        for (DocumentationItem item : items) {

            String catKey = item.getType();
            String catLabel = categories.get(catKey);
            if (docs.containsKey(catLabel)) {
                docs.get(catLabel).add(item);
            } else {
                List<DocumentationItem> itemList = new ArrayList<DocumentationItem>();
                itemList.add(item);
                docs.put(catLabel, itemList);
            }
        }
        return getView("index").arg("distId", ctx.getProperty("distId")).arg("docsByCat", docs).arg("searchFilter", fulltext);
    }


    @GET
    @Produces("text/html")
    @Path(value = "view/{docUUID}")
    public Object viewDoc(@PathParam("docUUID") String docUUID) throws Exception {

        DocumentRef docRef = new IdRef(docUUID);
        DocumentModel docModel = getContext().getCoreSession().getDocument(docRef);
        DocumentationItem doc=docModel.getAdapter(DocumentationItem.class);

        return getView("viewSingleDoc").arg("distId", ctx.getProperty("distId")).arg("doc", doc);
    }

}
