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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebObject(type = "Document")
@Produces("text/html; charset=UTF-8")
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
                    + "\") AND (ecm:isVersion = 0) AND (ecm:path STARTSWITH \"" + path + "\")" + orderClause;
        }
        DocumentModelList docs = ctx.getCoreSession().query(query);
        return getView("search").arg("query", query).arg("result", docs);
    }

    @DELETE
    public Response doDelete() {
        try {
            CoreSession session = ctx.getCoreSession();
            session.removeDocument(doc.getRef());
            session.save();
        } catch (NuxeoException e) {
            e.addInfo("Failed to delete document " + doc.getPathAsString());
            throw e;
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
        String pathSegment = URIUtils.quoteURIPathComponent(newDoc.getName(), true);
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

    // TODO implement HEAD
    public Object doHead() {
        return null; // TODO
    }

    @Path("{path}")
    public Resource traverse(@PathParam("path") String path) {
        return newDocument(path);
    }

    public DocumentObject newDocument(String path) {
        PathRef pathRef = new PathRef(doc.getPath().append(path).toString());
        DocumentModel doc = ctx.getCoreSession().getDocument(pathRef);
        return (DocumentObject) ctx.newObject(doc.getType(), doc);
    }

    public DocumentObject newDocument(DocumentRef ref) {
        DocumentModel doc = ctx.getCoreSession().getDocument(ref);
        return (DocumentObject) ctx.newObject(doc.getType(), doc);
    }

    public DocumentObject newDocument(DocumentModel doc) {
        return (DocumentObject) ctx.newObject(doc.getType(), doc);
    }

    public CoreSession getCoreSession() {
        return ctx.getCoreSession();
    }

    public DocumentModel getDocument() {
        return doc;
    }

    public String getTitle() {
        return doc.getTitle();
    }

}
