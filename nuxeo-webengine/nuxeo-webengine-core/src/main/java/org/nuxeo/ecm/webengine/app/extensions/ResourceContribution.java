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

import org.nuxeo.ecm.webengine.app.annotations.ResourceExtension;
import org.nuxeo.ecm.webengine.model.Resource;

/**
 * A resource contribution is a resource provider for a specific extension point (i.e. path segment)
 * in a target resource of type {@link ExtensibleResource}.
 * The resource contribution is instantiated in a lazy way when the modules are registered.
 * (i.e. the first time the module registry is accessed by client code - usually at first request on a web module root resource)
 * <p>
 * The resource contribution is responsible of checking if the contribution can be done - depending on current context
 * and to instantiate new resources to be used when request is matching the contribution key.   
 * <p>
 * Classes implementing this interface must be annotated using {@link ResourceExtension} to define the target resource, 
 * the path segment used for matching and optional hints for computing enabling state of this contribution 
 * depending on the runtiem context.      
 *  
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ResourceContribution {

    protected Class<? extends ExtensibleResource> target;
    protected String key;
    protected String label;
    protected String[] categories;
    protected String[] targetFacets;
    protected boolean translate;
    
    public ResourceContribution() {
        ResourceExtension anno = getClass().getAnnotation(ResourceExtension.class);
        if (anno == null) {
            throw new IllegalStateException("Resource contributions must be annotated with "+ResourceExtension.class+". Faulty contribution: "+getClass());
        }
        target = anno.target();
        categories = anno.categories();
        key = anno.key();
        targetFacets = anno.targetFacets();
        label = anno.label();
        if (label.length() == 0) {
            translate = true;
            label = getClass().getName()+".label";
        }
    }
    
    public String getKey() {
        return key;
    }
    
    /**
     * Override this to specify a link target page
     * @return
     */
    public String getLinkTarget() {
        return null;
    }
    
    public String getLabel() {
        // TODO - use i18n messages if translate = true
        return label;
    }
    
    public String[] getCategories() {
        return categories;
    }
    
    public boolean hasCategory(String category) {
        if (categories == null || categories.length == 0) {
            return false;
        }
        for (int i=0; i<categories.length; i++) {
            if (category.equals(categories[i])) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if the given resource is accepted.
     * Override this method if you need to filter your contributions using custom logic. 
     * The default implementation is using the target facets declared in the {@link ResourceExtension} 
     * annotation to check contribution enablement.   
     * @param target
     * @return true if the target resource is accepted and contribution can be done, false otherwise
     */
    public boolean accept(Resource target) {
        if (targetFacets == null || targetFacets.length == 0) {
            return true;
        }
        for (int i=0; i<targetFacets.length; i++) {
            if (target.hasFacet(targetFacets[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a resource instance to handle the request 
     * @param target
     * @return
     */
    public abstract Object newInstance(Resource target);

    @Override
    public String toString() {
        return "Contribution "+getClass().getName()+" on "+target+" at "+key;
    }
}
