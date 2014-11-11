/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

/**
 * Version Service - manage document versions
 * TODO not yet implemented
 * <p>
 * Accepts the following methods:
 * <ul>
 * <li> GET - get the last document version
 * <li> DELETE - delete a version
 * <li> POST - create a new version
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebAdapter(name = "versions", type = "VersionService", targetType = "Document", targetFacets = {"Versionable"})
public class VersionService extends DefaultAdapter {

    @GET
    public Object doGet() {
        return getTarget().getView("versions");
    }

    @Path("last")
    public DocumentObject getLastVersion() {
        try {
            DocumentObject dobj = (DocumentObject) getTarget();
            DocumentModel doc = dobj.getDocument();
            DocumentModel v = dobj.getCoreSession().getLastDocumentVersion(doc.getRef());
            if (v != null) {
                return dobj.newDocument(v);
            }
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
        throw new WebResourceNotFoundException(
                "No version found for " + ((DocumentObject) getTarget()).getDocument().getPath());
    }

    @Path("{label}")
    public DocumentObject getVersion(@PathParam("label") String label) {
        try {
            DocumentObject dobj = (DocumentObject) getTarget();
            DocumentModel doc = dobj.getDocument();
            List<VersionModel> versions = dobj.getCoreSession().getVersionsForDocument(doc.getRef());
            for (VersionModel v : versions) {
                if (label.equals(v.getLabel())) {
                    return dobj.newDocument(dobj.getCoreSession().getDocumentWithVersion(doc.getRef(), v));
                }
            }
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
        throw new WebResourceNotFoundException(
                "No such version " + label + " for document" + getTarget().getPath());
    }

    @POST
    public Object doPost() {
        return null;
    }

}
