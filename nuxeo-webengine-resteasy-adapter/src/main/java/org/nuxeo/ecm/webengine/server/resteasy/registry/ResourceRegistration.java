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

import javax.ws.rs.Path;

import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.nuxeo.ecm.webengine.loader.ClassProxy;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ResourceRegistration {

    protected String path;
    protected Object resource;
    

    public ResourceRegistration(Object resource) {
        this (resource, null);
    }
    
    
    public ResourceRegistration(Object resource, String path) {
        this.resource = resource;
        this.path = path;
    }

    
    public String getResourcePath() {
        return path != null ? path : getResourceClass().getAnnotation(Path.class).value();
    }
    
    public Class<?> getResourceClass() {
        if (resource.getClass() == Class.class) {
           return resource.getClass(); 
        } else if (resource instanceof ClassProxy) {
            return ((ClassProxy)resource).get();
        } else {
            return resource.getClass();
        }
    }

    public abstract void register(ResourceMethodRegistry registry);
    public abstract void unregister(ResourceMethodRegistry registry);

}
