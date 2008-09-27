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
import javax.ws.rs.core.Context;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.annotations.Application;
import org.nuxeo.ecm.webengine.rest.model.ManagedResource;
import org.nuxeo.ecm.webengine.rest.model.WebApplication;
import org.nuxeo.ecm.webengine.rest.model.WebObject;
import org.nuxeo.ecm.webengine.rest.model.WebType;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Application(name="document", path="/doc")
public class DocumentResource extends ManagedResource {

    protected String repository = "default"; // TODO

    public DocumentResource(WebApplication config) throws WebException {
        super (config);
    }
    
    @Override
    protected WebType getResourceType(WebContext2 ctx) throws WebException {
        CoreSession session = ctx.getCoreSession();
        // create the root document
        try {
            DocumentModel root = session.getDocument(new PathRef((String)app.getProperty("contentRoot")));
            return app.getType(root.getType());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }
    

    @Path(value="{path}", limited=true)
    public WebObject dispatch(@PathParam("path") String path, @Context WebContext2 ctx) throws Exception {
        return super.dispatch(path, ctx);
    }

}
