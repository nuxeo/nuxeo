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
package org.nuxeo.ecm.webengine.app.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.webengine.app.annotations.ResourceExtension;

/**
 * Describe the external contributions on a parent resource
 * 
 * Class names are used to store references to involved resources instead of class objects since
 * WebEngine is using a specific class loader (not the one used by the framework) to load resources - and doing so we avoid class cast exceptions. 
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ResourceContributions {

    /**
     * The target resource class name
     */
    protected String target;
    
    /**
     * A map of key : values where key is the segment name that should match a contribution resource and value is the contribution resource class name
     */
    protected Map<String, String> contribs;
    
    
    public ResourceContributions(String target) {
        this.target = target;
        contribs = new ConcurrentHashMap<String, String>();
    }
    
    public String getTarget() {
        return target;
    }
    
    public String getContribution(String key) {
        return contribs.get(key);
    }
    
    public void addContribution(String key, String resourceType) {
        contribs.put(key, resourceType);
    }
    
    public void removeContribution(String key) {
        contribs.remove(key);
    }
    
    public void addContribution(Class<?> contrib) {
        ResourceExtension xt = contrib.getAnnotation(ResourceExtension.class);
        if (xt == null) {
            throw new Error("Tried to contribute an extension resource "+contrib+" which is not annotated using "+ResourceExtension.class);
        }
        addContribution(xt.key(), xt.target().getName());
    }
    
    public void removeContribution(Class<?> contrib) {
        ResourceExtension xt = contrib.getAnnotation(ResourceExtension.class);
        if (xt != null) {
            removeContribution(xt.key());    
        }        
    }
    
}
