/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

/**
 * Version Service - manage document versions TODO not yet implemented
 * <p>
 * Accepts the following methods:
 * <ul>
 * <li>GET - get the last document version
 * <li>DELETE - delete a version
 * <li>POST - create a new version
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebAdapter(name = "versions", type = "VersionService", targetType = "Document", targetFacets = { "Versionable" })
public class VersionService extends DefaultAdapter {

    @GET
    public Object doGet() {
        return getTarget().getView("versions");
    }

    @Path("last")
    public DocumentObject getLastVersion() {
        DocumentObject dobj = (DocumentObject) getTarget();
        DocumentModel doc = dobj.getDocument();
        DocumentModel v = dobj.getCoreSession().getLastDocumentVersion(doc.getRef());
        if (v != null) {
            return dobj.newDocument(v);
        }

        throw new WebResourceNotFoundException(
                "No version found for " + ((DocumentObject) getTarget()).getDocument().getPath());
    }

    @Path("{label}")
    public DocumentObject getVersion(@PathParam("label") String label) {
        DocumentObject dobj = (DocumentObject) getTarget();
        DocumentModel doc = dobj.getDocument();
        List<VersionModel> versions = dobj.getCoreSession().getVersionsForDocument(doc.getRef());
        for (VersionModel v : versions) {
            if (label.equals(v.getLabel())) {
                return dobj.newDocument(dobj.getCoreSession().getDocumentWithVersion(doc.getRef(), v));
            }
        }

        throw new WebResourceNotFoundException("No such version " + label + " for document" + getTarget().getPath());
    }

    @POST
    public Object doPost() {
        return null;
    }

}
