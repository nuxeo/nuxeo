/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.nuxeo.ecm.webengine.jaxrs.views.TemplateViewMessageBodyWriter;
import org.nuxeo.ecm.webengine.jaxrs.views.ViewMessageBodyWriter;
import org.osgi.framework.Bundle;

/**
 * A composite JAX-RS application that can receive fragments from outside.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ApplicationHost extends Application {

    protected final String name;

    protected final List<ApplicationFragment> apps;

    protected List<Reloadable> listeners;


    /**
     * Root resource classes to owner bundles.
     * This is a fall-back for FrameworkUtils.getBundle(class)
     * since is not supported in all OSGi like frameworks
     */
    protected HashMap<Class<?>, Bundle> class2Bundles;


    public ApplicationHost(String name) {
        this.name = name;
        apps = new ArrayList<ApplicationFragment>();
        class2Bundles = new HashMap<Class<?>, Bundle>();
        listeners = new ArrayList<Reloadable>();
    }

    public String getName() {
        return name;
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

    public synchronized void reload() throws Exception {
        for (ApplicationFragment fragment : apps) {
            fragment.reload();
        }
        class2Bundles = new HashMap<Class<?>, Bundle>();
        for (Reloadable listener : listeners) {
            listener.reload();
        }
    }

    /**
     * Get the bundle declaring the given root class.
     * This method is not synchronized since it is assumed to be called
     * after the application was created and before it was destroyed.
     * <br>
     * When a bundle is refreshing this method may throw
     * exceptions but it is not usual to refresh bundles at runtime
     * and making requests in same time.
     *
     * @param clazz
     * @return
     */
    public Bundle getBundle(Class<?> clazz) {
        return class2Bundles.get(clazz);
    }

    @Override
    public synchronized Set<Class<?>> getClasses() {
        HashSet<Class<?>> result = new HashSet<Class<?>>();
        for (ApplicationFragment app : getApplications()) {
            for (Class<?> clazz : app.getClasses()) {
                if (clazz.isAnnotationPresent(Path.class)) {
                    class2Bundles.put(clazz, app.getBundle());
                }
                result.add(clazz);
            }
        }
        return result;
    }

    @Override
    public synchronized Set<Object> getSingletons() {
        HashSet<Object> result = new HashSet<Object>();
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
