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
package org.nuxeo.ecm.webengine.app;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.nuxeo.ecm.webengine.app.annotations.ResourceExtension;
import org.nuxeo.ecm.webengine.app.annotations.ResourceExtensions;
import org.nuxeo.ecm.webengine.app.annotations.WebModule;
import org.nuxeo.ecm.webengine.app.extensions.ResourceContribution;

/**
 * Base application class for WebEngine modules.
 * Enable application configuration through annotations - like declaring root resources and other WebEngine related 
 * configuration.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class WebEngineModule extends Application {

    protected String name;
    protected Class<?>[] roots;
    protected Class<? extends WebEngineModule> base;
    protected Class<? extends ResourceContribution>[] contributions;
    
    public WebEngineModule() {
        Class<?> type = getClass();
        WebModule wm = type.getAnnotation(WebModule.class);
        if (wm == null) {
            throw new Error("Invalid web engine application class: "+type+". Must be annotated with @WebModule");
        }
        name = wm.name();
        if (name.length() == 0) {
            name = type.getSimpleName();
        };
        base = wm.base();
        if (Modifier.isAbstract(base.getModifiers())) {
            base = null;
        }
        roots = wm.roots();
        ResourceExtensions rs = type.getAnnotation(ResourceExtensions.class);
        if (rs != null) {
            contributions = rs.value();
            for (int i=0; i<contributions.length; i++) {
                if (!contributions[i].isAnnotationPresent(ResourceExtension.class)) {
                    throw new Error("Invalid resource extension class: "+contributions[i]+" in application: "+type+". Must be annotated with @ResourceExtension.");        
                }
            }
        }
    }
    
    /**
     * When overriding this class make sure to call <code>super.getClasses()</code> to add your custom classes.
     * <p>
     * Example:
     * <pre>
     * Set<Class<?>> result = super.getClasses();
     * result.add(MyResource.class);
     * return result;
     * </pre>
     */
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = new HashSet<Class<?>>();
        Class<?>[] rc = getRootClasses();
        if (rc != null && rc.length > 0) {
            for (Class<?> c : rc) {
                result.add(c);
            }
        }
        return result;
    }

    /**
     * The module name to be displayed in UI
     * @return
     */
    public String getName() {
        WebModule wm = getClass().getAnnotation(WebModule.class);
        return wm != null ? wm.name() : null;
    }
    
    public Class<?>[] getRootClasses() {
        return roots;
    }

    public Class<? extends ResourceContribution>[] getContributions() {
        return contributions;
    }
    
    public Class<? extends WebEngineModule> getBaseModule() {
        return base;
    }
    
}
