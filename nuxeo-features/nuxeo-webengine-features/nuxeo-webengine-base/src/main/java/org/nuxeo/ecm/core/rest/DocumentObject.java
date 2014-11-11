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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebObject(type = "Document")
@Produces({"text/html; charset=UTF-8"})
public class DocumentObject extends DefaultObject {

    protected DocumentModel doc;

    @Override
    public <A> A getAdapter(Class<A> adapter) {
        if (adapter == DocumentModel.class) {
            return adapter.cast(doc);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void initialize(Object... args) {
        assert args != null && args.length == 1;
        doc = (DocumentModel) args[0];
    }

    @GET
    public Object doGet() {
        return getView("index");
    }

    // simulate a DELETE using GET
    @GET
    @Path("@delete")
    public Response getDelete() {
        return doDelete();
    }

    @GET
    @Path("@search")
    public Object search() {
        final HttpServletRequest request = ctx.getRequest();
        String query = request.getParameter("query");
        if (query == null) {
            String fullText = request.getParameter("fullText");
            if (fullText == null) {
                throw new IllegalParameterException("Expecting a query or a fullText parameter");
            }
            String orderBy = request.getParameter("orderBy");
            String orderClause = "";
            if (orderBy != null) {
                orderClause = " ORDER BY " + orderBy;
            }
            String path;
            if (doc.isFolder()) {
                path = doc.getPathAsString();
            } else {
                path = doc.getPath().removeLastSegments(1).toString();
            }
            query = "SELECT * FROM Document WHERE (ecm:fulltext = \"" + fullText
                    + "\") AND (ecm:isCheckedInVersion = 0) AND (ecm:path STARTSWITH \"" + path + "\")" + orderClause;
        }
        try {
            DocumentModelList docs = ctx.getCoreSession().query(query);
            return getView("search").arg("query", query).arg("result", docs);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    @DELETE
    public Response doDelete() {
        try {
            CoreSession session = ctx.getCoreSession();
            session.removeDocument(doc.getRef());
            session.save();
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete document " + doc.getPathAsString(), e);
        }
        if (prev != null) { // show parent ? TODO: add getView(method) to be able to change the view method
            return redirect(prev.getPath());
        }
        return redirect(ctx.getBasePath());
    }

    @POST
    public Response doPost() {
        String name = ctx.getForm().getString("name");
        DocumentModel newDoc = DocumentHelper.createDocument(ctx, doc, name);
        String pathSegment = URIUtils.quoteURIPathComponent(newDoc.getName(),
                true);
        return redirect(getPath() + '/' + pathSegment);
    }

    @PUT
    public Response doPut() {
        doc = DocumentHelper.updateDocument(ctx, doc);
        return redirect(getPath());
    }

    @POST
    @Path("@put")
    public Response getPut() {
        return doPut();
    }

    //TODO implement HEAD
    public Object doHead() {
        return null; //TODO
    }

    @Path(value = "{path}")
    public Resource traverse(@PathParam("path") String path) {
        return newDocument(path);
    }

    public DocumentObject newDocument(String path) {
        try {
            PathRef pathRef = new PathRef(doc.getPath().append(path).toString());
            DocumentModel doc = ctx.getCoreSession().getDocument(pathRef);
            return (DocumentObject) ctx.newObject(doc.getType(), doc);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    public DocumentObject newDocument(DocumentRef ref) {
        try {
            DocumentModel doc = ctx.getCoreSession().getDocument(ref);
            return (DocumentObject) ctx.newObject(doc.getType(), doc);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    public DocumentObject newDocument(DocumentModel doc) {
        try {
            return (DocumentObject) ctx.newObject(doc.getType(), doc);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    public CoreSession getCoreSession() {
        return ctx.getCoreSession();
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public String getTitle() {
        try {
            return doc.getTitle();
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

}
