/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package wiki;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.rest.DocumentFactory;
import org.nuxeo.ecm.core.rest.DocumentHelper;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

@WebObject(type = "wikis", facets = { "mainWiki" })
@Produces("text/html; charset=UTF-8")
public class Main extends ModuleRoot {

    DocumentModel doc;

    public Main() {
        // doc = getRootDocument();
    }

    public DocumentModel getDocument() {
        if (doc == null) {
            doc = getRootDocument();
        }
        return doc;
    }

    public static DocumentModel getRootDocument() {
        try {
            // testing if exist, and create it if doesn't exist
            DocumentRef wikisPath = new PathRef(
                    "/default-domain/workspaces/wikis");
            WebContext ctx = WebEngine.getActiveContext();
            CoreSession session = ctx.getCoreSession();
            if (session.exists(wikisPath)) {
                return session.getDocument(wikisPath);
            }
            DocumentModel newDoc = session.createDocumentModel(
                    "/default-domain/workspaces/", "wikis", "Workspace");
            if (newDoc.getTitle().length() == 0) {
                newDoc.getPart("dublincore").get("title").setValue(newDoc.getName());
            }
            newDoc = session.createDocument(newDoc);
            session.save();
            return newDoc;
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @POST
    public Response doPost() {
        String name = ctx.getForm().getString("name");
        DocumentModel newDoc = DocumentHelper.createDocument(ctx, getDocument(), name);
        return redirect(getPath() + '/' + newDoc.getName());
    }

    @GET
    public Object doGet() throws ClientException {
        List<DocumentModel> docs = ctx.getCoreSession().getChildren(getDocument().getRef(), "Wiki");
        return getView("index").arg("wikis", docs);
    }

    @Path("{segment}")
    public Object getWiki(@PathParam("segment") String segment) {
        return DocumentFactory.newDocument(ctx, getDocument().getPath().append(segment).toString());
    }

    @GET
    @Path("create/{segment}")
    public Response createPage(@PathParam("segment") String segment) {
        try {
            CoreSession session = ctx.getCoreSession();
            DocumentModel newDoc = session.createDocumentModel(
                    "/default-domain/workspaces/", segment, "Workspace");
            if (newDoc.getTitle().length() == 0) {
                newDoc.getPart("dublincore").get("title").setValue(newDoc.getName());
            }
            newDoc = session.createDocument(newDoc);
            session.save();
            return redirect(path + "/" + segment);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    // handle errors
    @Override
    public Object handleError(WebApplicationException e) {
        if (e instanceof WebSecurityException) {
            return Response.status(401).entity(getTemplate("error/error_401.ftl")).build();
        } else if (e instanceof WebResourceNotFoundException) {
            return Response.status(404).entity(getTemplate("error/error_404.ftl")).build();
        } else {
            return super.handleError(e);
        }
    }

    @Override
    public String getLink(DocumentModel doc) {
        getDocument(); // force doc loading
        String type = doc.getType();
        if ("Wiki".equals(type)) {
            return getPath() + "/" + doc.getName();
        } else if ("WikiPage".equals(type)) {
            // TODO: this will not work with multi level wiki pages
            org.nuxeo.common.utils.Path path = doc.getPath();
            int cnt = path.segmentCount();
            String s = getPath() + "/" + path.segment(cnt - 2) + "/"
                    + path.lastSegment();
            return s;
        }
        return super.getLink(doc);
    }

}
