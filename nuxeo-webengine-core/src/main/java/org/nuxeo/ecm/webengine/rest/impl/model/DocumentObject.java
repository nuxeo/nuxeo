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

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.annotations.WebObject;
import org.nuxeo.ecm.webengine.rest.impl.DefaultObject;
import org.nuxeo.ecm.webengine.rest.model.Resource;
import org.nuxeo.ecm.webengine.rest.model.ResourceType;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@WebObject(value="Document", superType="*")
public class DocumentObject extends DefaultObject {

    protected DocumentModel doc;


    @Override
    public Resource initialize(WebContext2 ctx, ResourceType<?> type,
            Object... args) throws WebException {
        assert args != null && args.length == 1;
        doc = (DocumentModel)args[0];
        return super.initialize(ctx, type, args);
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
    
    public CoreSession getCoreSession() {
        return ctx.getCoreSession();
    }

    public DocumentModel getDocument() {
        return doc;
    }

}
