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

package org.nuxeo.ecm.webengine.server.resteasy.registry;

import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.plugins.server.resourcefactory.POJOResourceFactory;
import org.nuxeo.ecm.webengine.loader.ClassProxy;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PerRequestRegistration extends ResourceRegistration {

    
    public PerRequestRegistration(Object resource) {
        super (resource);
    }
    
    
    public PerRequestRegistration(Object resource, String path) {
        super (resource, path);
    }
    
    public void register(ResourceMethodRegistry registry) {
        if (path == null) {
            if (resource instanceof ClassProxy) {
                registry.addResourceFactory(new ReloadablePerRequestFactory((ClassProxy)resource), null);
            } else {
                registry.addPerRequestResource((Class<?>)resource);
            }
        } else {
            if (resource instanceof ClassProxy) {
                registry.addResourceFactory(new ReloadablePerRequestFactory((ClassProxy)resource), path, ((ClassProxy)resource).get());                
            } else {
                registry.addResourceFactory(new POJOResourceFactory((Class<?>)resource), path, (Class<?>)resource);
            }            
        }
    }
    
    public void unregister(ResourceMethodRegistry registry) {
      // TODO
    }

}
