/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.webengine.jaxrs.servlet.config.ResourceExtension;
import org.nuxeo.ecm.webengine.jaxrs.views.BundleResource;
import org.nuxeo.ecm.webengine.jaxrs.views.TemplateViewMessageBodyWriter;
import org.nuxeo.ecm.webengine.jaxrs.views.ViewMessageBodyWriter;
import org.osgi.framework.Bundle;

/**
 * A composite JAX-RS application that can receive fragments from outside.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ApplicationHost extends Application {

    private static final Log log = LogFactory.getLog(ApplicationHost.class);

    protected final String name;

    protected final Map<String, Boolean> features = new HashMap<>();

    protected final List<ApplicationFragment> apps;

    protected List<Reloadable> listeners;

    protected RenderingEngine rendering;

    /**
     * Sub-Resources extensions
     */
    protected Map<String, ResourceExtension> extensions;

    /**
     * Root resource classes to owner bundles. This is a fall-back for FrameworkUtils.getBundle(class) since is not
     * supported in all OSGi like frameworks
     */
    protected HashMap<Class<?>, Bundle> class2Bundles;

    public ApplicationHost(String name) {
        this.name = name;
        apps = new ArrayList<>();
        class2Bundles = new HashMap<>();
        listeners = new ArrayList<>();
        extensions = new HashMap<>();
    }

    public BundleResource getExtension(BundleResource target, String segment) {
        ResourceExtension xt = getExtension(target.getClass().getName() + "#" + segment);
        if (xt != null) {
            BundleResource res = target.getResource(xt.getResourceClass());
            if (res != null && res.accept(target)) {
                res.getContext().pushBundle(xt.getBundle());
                return res;
            }
        }
        return null;
    }

    public RenderingEngine getRendering() {
        return rendering;
    }

    public void setRendering(RenderingEngine rendering) {
        this.rendering = rendering;
    }

    public synchronized void addExtension(ResourceExtension xt) {
        extensions.put(xt.getId(), xt);
        class2Bundles.put(xt.getResourceClass(), xt.getBundle());
        if (rendering != null) {
            rendering.flushCache();
        }
    }

    public synchronized void removeExtension(ResourceExtension xt) {
        extensions.remove(xt.getId());
        class2Bundles.remove(xt.getResourceClass());
        if (rendering != null) {
            rendering.flushCache();
        }
    }

    public synchronized ResourceExtension getExtension(String id) {
        return extensions.get(id);
    }

    public synchronized ResourceExtension[] getExtensions(ResourceExtension xt) {
        return extensions.values().toArray(new ResourceExtension[extensions.size()]);
    }

    public String getName() {
        return name;
    }

    public Map<String, Boolean> getFeatures() {
        return features;
    }

    public synchronized void add(ApplicationFragment app) {
        apps.add(app);
    }

    public synchronized void remove(ApplicationFragment app) {
        apps.remove(app);
    }

    public synchronized ApplicationFragment[] getApplications() {
        return apps.toArray(new ApplicationFragment[apps.size()]);
    }

    public synchronized void addReloadListener(Reloadable listener) {
        listeners.add(listener);
    }

    public synchronized void removeReloadListener(Reloadable listener) {
        listeners.remove(listener);
    }

    public synchronized void reload() {
        for (ApplicationFragment fragment : apps) {
            fragment.reload();
        }
        // TODO this will not work with extension subresources - find a fix
        class2Bundles = new HashMap<>();
        for (Reloadable listener : listeners) {
            listener.reload();
        }
        if (rendering != null) {
            rendering.flushCache();
        }
    }

    /**
     * Get the bundle declaring the given root class. This method is not synchronized since it is assumed to be called
     * after the application was created and before it was destroyed. <br>
     * When a bundle is refreshing this method may throw exceptions but it is not usual to refresh bundles at runtime
     * and making requests in same time.
     */
    public Bundle getBundle(Class<?> clazz) {
        return class2Bundles.get(clazz);
    }

    @Override
    public synchronized Set<Class<?>> getClasses() {
        HashSet<Class<?>> result = new HashSet<>();
        for (ApplicationFragment app : getApplications()) {
            try {
                for (Class<?> clazz : app.getClasses()) {
                    if (clazz.isAnnotationPresent(Path.class)) {
                        class2Bundles.put(clazz, app.getBundle());
                    }
                    result.add(clazz);
                }
            } catch (java.lang.LinkageError e) {
                log.error(e);
            }
        }
        return result;
    }

    @Override
    public synchronized Set<Object> getSingletons() {
        HashSet<Object> result = new HashSet<>();
        result.add(new TemplateViewMessageBodyWriter());
        result.add(new ViewMessageBodyWriter());
        for (ApplicationFragment app : getApplications()) {
            for (Object obj : app.getSingletons()) {
                if (obj.getClass().isAnnotationPresent(Path.class)) {
                    class2Bundles.put(obj.getClass(), app.getBundle());
                }
                result.add(obj);
            }
        }
        return result;
    }

}
