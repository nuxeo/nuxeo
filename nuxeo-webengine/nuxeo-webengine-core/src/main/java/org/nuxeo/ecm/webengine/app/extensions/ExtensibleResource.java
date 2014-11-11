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
package org.nuxeo.ecm.webengine.app.extensions;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;

import com.sun.jersey.spi.container.servlet.WebComponent;


/**
 * A resource that can be extended with sub resources contributed via an {@link ResourceContribution}. 
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ExtensibleResource implements Resource {

    protected WebContext ctx;
    
    public ExtensibleResource() {
        this (WebEngine.getActiveContext());
    }
    
    public ExtensibleResource(WebContext ctx) {
        this.ctx = ctx;
    }
    
    @Path("{key}")
    public Object dispatch(@PathParam("key") String key) {
        try {
            ResourceContribution res = (ResourceContribution)getContext().getEngine().getApplicationManager().getContribution(this, key);
            if (res.accept(this)) {
                return res;
            } else {
                return resolveUnmatchedSegment(key);
            }
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }
    
    protected Object resolveUnmatchedSegment(String key) {
        throw new WebResourceNotFoundException("No resource found at "+key+" in context "+this);
    }

    
    
}
