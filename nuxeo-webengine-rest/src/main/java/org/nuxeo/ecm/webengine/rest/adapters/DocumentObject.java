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

package org.nuxeo.ecm.webengine.rest.adapters;

import javax.ws.rs.GET;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.methods.LOCK;
import org.nuxeo.ecm.webengine.rest.types.WebType;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentObject extends WebObject {

    protected DocumentModel doc;


    public DocumentObject(WebType type) {
        super (type);
    }

    public void initialize(WebContext2 ctx, DocumentModel doc) {
        super.initialize(ctx, null); //TODO
        this.ctx = ctx;
        this.doc = doc;
    }


    public CoreSession getCoreSession() {
        return ctx.getCoreSession();
    }

    public DocumentModel getDocument() {
        return doc;
    }

    @GET
    public DocumentModel get() throws Exception {
        return doc;
    }

    @LOCK
    public String lock() throws Exception {
        return "LockId";
    }

}
