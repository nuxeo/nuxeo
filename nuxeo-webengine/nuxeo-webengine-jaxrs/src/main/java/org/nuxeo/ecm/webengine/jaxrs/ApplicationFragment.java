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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.osgi.framework.Bundle;

/**
 * A wrapper for a JAX-RS application fragment declared in manifest.
 * The fragment application will be added to the target host application.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ApplicationFragment extends Application {

    protected final String hostName;

    protected final Bundle bundle;

    protected final Map<String,String> attrs;

    protected String appClass;

    private volatile Application app;

    public static Map<String, String> createAttributes(String hostName) {
        HashMap<String, String> attrs = new HashMap<String, String>();
        if (hostName != null) {
            attrs.put("host", hostName);
        }
        return attrs;
    }


    public ApplicationFragment(Bundle bundle, String appClass) {
        this (bundle, appClass, (String)null);
    }

    public ApplicationFragment(Bundle bundle, String appClass, String host) {
        this (bundle, appClass, createAttributes(host));
    }

    public ApplicationFragment(Bundle bundle, String appClass, Map<String,String> attrs) {
        this.bundle = bundle;
        this.appClass = appClass;
        this.attrs = attrs;
        String host = attrs.get("host");
        this.hostName = host == null ? "default" : host;
    }

    protected synchronized void createApp() {
        try {
            Object obj = bundle.loadClass(appClass).newInstance();
            if (obj instanceof ApplicationFactory) {
                app = ((ApplicationFactory)obj).getApplication(bundle, attrs);
            } else if (obj instanceof Application) {
                app = (Application)obj;
            } else {
                throw new IllegalArgumentException("Expecting an Application or ApplicationFactory class: "+appClass);
            }
        } catch (Exception e) {
            String msg = "Cannot instantiate JAX-RS application "+appClass+" from bundle "+bundle.getSymbolicName();
            throw new RuntimeException(msg, e);
        }
    }

    public synchronized void reload() throws Exception {
        app = null;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public Map<String, String> getAttrs() {
        return attrs;
    }

    public String getHostName() {
        return hostName;
    }

    public Application get() {
        if (app == null) {
            createApp();
        }
        return app;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return get().getClasses();
    }

    @Override
    public Set<Object> getSingletons() {
        return get().getSingletons();
    }

}
