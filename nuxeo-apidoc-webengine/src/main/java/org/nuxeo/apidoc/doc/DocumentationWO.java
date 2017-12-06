/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.doc;

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
    public Object viewAll() {
        DocumentationService ds = Framework.getService(DocumentationService.class);
        Map<String, List<DocumentationItem>> docs = ds.listDocumentationItems(getContext().getCoreSession(), null, null);
        return getView("index").arg("distId", ctx.getProperty("distId")).arg("docsByCat", docs);
    }

    @GET
    @Produces("text/html")
    @Path("filter")
    public Object filterAll() {
        String fulltext = getContext().getForm().getFormProperty("fulltext");
        DocumentationService ds = Framework.getService(DocumentationService.class);

        ArtifactSearcher searcher = Framework.getService(ArtifactSearcher.class);
        List<DocumentationItem> items = searcher.searchDocumentation(getContext().getCoreSession(),
                (String) ctx.getProperty("distId"), fulltext, null);
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
        return getView("index").arg("distId", ctx.getProperty("distId")).arg("docsByCat", docs).arg("searchFilter",
                fulltext);
    }

    @GET
    @Produces("text/html")
    @Path("view/{docUUID}")
    public Object viewDoc(@PathParam("docUUID") String docUUID) {
        DocumentRef docRef = new IdRef(docUUID);
        DocumentModel docModel = getContext().getCoreSession().getDocument(docRef);
        DocumentationItem doc = docModel.getAdapter(DocumentationItem.class);

        return getView("viewSingleDoc").arg("distId", ctx.getProperty("distId")).arg("doc", doc);
    }

}
