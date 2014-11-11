/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.webengine.app.document;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.app.extensions.ExtensibleResource;
import org.nuxeo.ecm.webengine.model.WebContext;

/**
 * A resource wrapping a Nuxeo document
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class DocumentResource extends ExtensibleResource {

    protected DocumentModel doc;
    
    public DocumentResource(DocumentModel doc) {
        this.doc = doc;
    }
    
    public DocumentResource(WebContext ctx, DocumentModel doc) {
        super (ctx);
        this.doc = doc;
    }
    
    protected DocumentModel getDocument() {
        return doc;
    }

    protected DocumentResource getDocumentResource(DocumentModel doc) {
    	return null;
        //return new DocumentResource(doc);
//      return getContext().getEngine().getApplicationManager()
//      .getDocumentResourceFactory().getDocumentResource(doc);
    }
    
    @Override
    protected Object resolveUnmatchedSegment(String key) {
        try {            
            DocumentModel child = getContext().getCoreSession().getDocument(
                    new PathRef(doc.getPath().append(key).toString()));
            return getDocumentResource(child);
        } catch (Exception e) {
            return super.resolveUnmatchedSegment(key);
        }
    }
    
}
