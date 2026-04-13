/*
 * (C) Copyright 2013-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.restapi.server;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.restapi.server.adapters.EmptyDocumentAdapter;
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
     * @since 5.8
     */
    @Path("path/@{adapterName}")
    public Object getRootPathAdapter(@PathParam("adapterName") String adapterName) {
        DocumentModel rootDocument = getContext().getCoreSession().getRootDocument();

        return ctx.newAdapter(newObject(JSONDocumentObject.class, rootDocument), adapterName);
    }

    @Path("path{docPath:(/(?:(?!/@).)*)}")
    public Object getDocsByPath(@PathParam("docPath") String docPath) {
        CoreSession session = getContext().getCoreSession();
        DocumentModel doc = session.getDocument(new PathRef(docPath));
        return newObject(JSONDocumentObject.class, doc);
    }

    @Path("id/{id}")
    public Object getDocsById(@PathParam("id") String id) {
        CoreSession session = getContext().getCoreSession();
        DocumentModel doc = session.getDocument(new IdRef(id));
        return newObject(JSONDocumentObject.class, doc);
    }

    @Path("@" + EmptyDocumentAdapter.NAME)
    public Object getEmptyDocumentModel() {
        return newObject(EmptyDocumentAdapter.class);
    }

    /**
     * @since 7.2
     */
    @Path("{otherPath}")
    public Object route(@PathParam("otherPath") String otherPath) {
        return newObject(otherPath);
    }
}
