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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@WebObject(name="Document", superType="*")
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
    public void initialize(Object... args) throws WebException {
        assert args != null && args.length == 1;
        doc = (DocumentModel)args[0];
    }

    @GET
    public Object doGet() {
        return getView("index.ftl");
    }

    @DELETE
    public Object doDelete() {
        try {
            CoreSession session = ctx.getCoreSession();
            session.removeDocument(doc.getRef());
            session.save();
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete document "+doc.getPathAsString(), e);
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
        return redirect(getPath()+'/'+newDoc.getName());
    }

    @PUT
    public Response doPut() {
        doc = DocumentHelper.updateDocument(ctx, doc);
        return redirect(getPath());
    }

    //TODO implement HEAD
    public Object doHead() {
        return null; //TODO
    }

    @Path(value="{path}")
    public Resource traverse(@PathParam("path") String path) throws WebException {
        return newObject(path);
    }

    public DocumentObject newObject(String path) {
        try {
            PathRef pathRef = new PathRef(doc.getPath().append(path).toString());
            DocumentModel doc = ctx.getCoreSession().getDocument(pathRef);
            return (DocumentObject)(ctx.newObject(doc.getType(), doc));
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    public DocumentObject newObject(DocumentRef ref) {
        try {
            DocumentModel doc = ctx.getCoreSession().getDocument(ref);
            return (DocumentObject)(ctx.newObject(doc.getType(), doc));
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    public DocumentObject newObject(DocumentModel doc) {
        try {
            return (DocumentObject)(ctx.newObject(doc.getType(), doc));
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
        return doc.getTitle();
    }

}
