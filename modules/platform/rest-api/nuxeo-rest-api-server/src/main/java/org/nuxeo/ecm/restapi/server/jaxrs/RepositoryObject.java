/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.EmptyDocumentAdapter;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * Holds all methods bound to a repository
 *
 * @since 5.7.3
 */
@WebObject(type = "repo")
public class RepositoryObject extends DefaultObject {

    /**
     * The regex of getDocsByPath doesn't catch the case of the root document.
     *
     * @param adapterName
     * @return
     * @since 5.8
     */
    @Path("path/@{adapterName}")
    public Object getRootPathAdapter(@PathParam("adapterName") String adapterName) {
        DocumentModel rootDocument = getContext().getCoreSession().getRootDocument();

        return ctx.newAdapter(newObject("Document", rootDocument), adapterName);
    }

    @Path("path{docPath:(/(?:(?!/@).)*)}")
    public Object getDocsByPath(@PathParam("docPath") String docPath) {
        CoreSession session = getContext().getCoreSession();
        DocumentModel doc = session.getDocument(new PathRef(docPath));
        return newObject("Document", doc);
    }

    @Path("id/{id}")
    public Object getDocsById(@PathParam("id") String id) {
        CoreSession session = getContext().getCoreSession();
        DocumentModel doc = session.getDocument(new IdRef(id));
        return newObject("Document", doc);
    }

    /**
     * @deprecated since 10.3, use {@link BulkActionFrameworkObject BAF} instead
     */
    @Path("bulk")
    @Deprecated
    public Object getBulkDocuments(@MatrixParam("id") List<String> ids) {
        return getBulkDocuments(this, ids);
    }

    /**
     * @deprecated since 10.3, use {@link BulkActionFrameworkObject BAF} instead
     */
    @Deprecated
    protected static Object getBulkDocuments(DefaultObject obj, List<String> ids) {
        CoreSession session = obj.getContext().getCoreSession();
        List<DocumentModel> docs = new ArrayList<>(ids.size());
        for (String loopid : ids) {
            docs.add(session.getDocument(new IdRef(loopid)));
        }

        return obj.newObject("bulk", new DocumentModelListImpl(docs));
    }

    @Path("@" + EmptyDocumentAdapter.NAME)
    public Object getEmptyDocumentModel() {
        return newObject("emptyDocumentAdapter");
    }

    /**
     * @since 7.2
     */
    @Path("{otherPath}")
    public Object route(@PathParam("otherPath") String otherPath) {
        return newObject(otherPath);
    }
}
