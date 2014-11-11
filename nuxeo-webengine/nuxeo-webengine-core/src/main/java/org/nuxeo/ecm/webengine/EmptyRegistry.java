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
package org.nuxeo.ecm.webengine;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;


/**
 * Empoty impl for deprecated {@link ResourceRegistry}. This will be removed in future
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EmptyRegistry implements ResourceRegistry {

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.webengine.ResourceRegistry#addBinding(org.nuxeo.ecm.webengine.ResourceBinding)
     */
    public void addBinding(ResourceBinding binding) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.webengine.ResourceRegistry#addMessageBodyReader(javax.ws.rs.ext.MessageBodyReader)
     */
    public void addMessageBodyReader(MessageBodyReader<?> reader) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.webengine.ResourceRegistry#addMessageBodyWriter(javax.ws.rs.ext.MessageBodyWriter)
     */
    public void addMessageBodyWriter(MessageBodyWriter<?> writer) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.webengine.ResourceRegistry#clear()
     */
    public void clear() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.webengine.ResourceRegistry#getBindings()
     */
    public ResourceBinding[] getBindings() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.webengine.ResourceRegistry#reload()
     */
    public void reload() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.webengine.ResourceRegistry#removeBinding(org.nuxeo.ecm.webengine.ResourceBinding)
     */
    public void removeBinding(ResourceBinding binding) {
        // TODO Auto-generated method stub
        
    }

    
    
}
