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

package org.nuxeo.ecm.webengine.rest.impl.model;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.annotations.WebObject;
import org.nuxeo.ecm.webengine.rest.impl.DefaultObject;
import org.nuxeo.ecm.webengine.rest.methods.LOCK;
import org.nuxeo.ecm.webengine.rest.model.ObjectResource;
import org.nuxeo.ecm.webengine.rest.model.Resource;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@WebObject(value="Document", superType="*")
public class DocumentObject extends DefaultObject {

    protected DocumentModel doc;


    public void setDocument(DocumentModel doc) {
        this.doc = doc;
    }

    @Path(value="{path}")
    public Resource dispatch(@Context WebContext2 ctx, @PathParam("path") String path) throws WebException {
        try {
            DocumentModel doc = ctx.getCoreSession().getChild(((DocumentObject)ctx.tail()).getDocument().getRef(), path);
            DocumentObject obj = (DocumentObject)ctx.getApplication().getType(doc.getType()).newInstance();
            obj.setDocument(doc);
            return ctx.push(obj);
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

    @GET
    public ObjectResource get() throws Exception {
        return this;
    }

    @LOCK
    public String lock() throws Exception {
        return "LockId";
    }

}
