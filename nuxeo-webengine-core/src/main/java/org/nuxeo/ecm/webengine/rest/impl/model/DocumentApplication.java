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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.model.MainResource;
import org.nuxeo.ecm.webengine.rest.model.ObjectResource;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *  
 *  TODO: use defined repository
 */
public class DocumentApplication extends MainResource {

    @Path(value="{path}")
    protected ObjectResource resolveObject(String segment) throws WebException {
        DocumentModel root = getRootDocument(ctx);       
        // push the root first
        //TODO we need the actual path not the path template
        ctx.push(getDocumentObject(ctx, root)); 
        DocumentModel doc = resolveDocument(ctx, segment);
        return (ObjectResource)ctx.push(getDocumentObject(ctx, doc));
    }
    
    public DocumentObject getDocumentObject(WebContext2 ctx, DocumentModel doc) throws WebException {
        DocumentObject obj = (DocumentObject)(app.getType(doc.getType()).newInstance());
        obj.setDocument(doc);
        return obj;
    }
    
    public DocumentModel resolveDocument(WebContext2 ctx, String path) throws WebException {
        try {
            String p = getContentRoot();
            if (path.startsWith("/") || p.endsWith("/")) {
                p = new StringBuilder().append(p).append(path).toString();
            } else {
                p = new StringBuilder().append(p).append('/').append(path).toString();
            }
            return ctx.getCoreSession().getDocument(new PathRef(p));
        } catch(Exception e) {
            throw WebException.wrap("Failed to get document: "+path, e);
        }
    }
    
    public DocumentModel getRootDocument(WebContext2 ctx) throws WebException {
        try {
            return ctx.getCoreSession().getDocument(new PathRef(getContentRoot()));
        } catch(Exception e) {
            throw WebException.wrap("Failed to get document: "+getContentRoot(), e);
        }
    }
    

    public String getRepository() {
        return (String)app.getProperty("repository", "default");
    }
    
    public String getContentRoot() {
        return (String)app.getProperty("content-root", "/");
    }
    
}
