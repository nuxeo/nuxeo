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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.model.MainResource;
import org.nuxeo.ecm.webengine.rest.model.Resource;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *  
 *  TODO: be able to use other repositories than the default
 */
public class DocumentApplication extends MainResource {

    public DocumentObject newRoot(String path) throws WebException {
        return newRoot(new PathRef(path));
    }

    public DocumentObject newRoot(DocumentRef doc) throws WebException {
        try {
            return newRoot(ctx.getCoreSession().getDocument(doc));
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }
    
    public DocumentObject newRoot(DocumentModel doc) throws WebException {
        return (DocumentObject)(ctx.newObject(doc.getType(), doc));
    }
        
    @Path(value="{path}")
    public Resource traverse(@PathParam("path") String path) throws WebException {
        return newRoot("/").newObject(path);
    }
    
}
