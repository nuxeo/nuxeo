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
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.webengine.jaxrs.servlet.JerseyServlet;
import org.nuxeo.ecm.webengine.jaxrs.servlet.config.ServletRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.sun.jersey.server.impl.provider.RuntimeDelegateImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Activator implements BundleActivator, BundleTrackerCustomizer, ServiceTrackerCustomizer {

    private static final Log log = LogFactory.getLog(Activator.class);

    private static Activator instance;

    public static Activator getInstance() {
        return instance;
    }

    protected ServiceTracker httpServiceTracker;

    protected BundleContext context;
    protected BundleTracker tracker;
    protected ServiceReference pkgAdm;

    protected CompositeApplication app;

    protected List<Reloadable> toReload;


    @Override
    public void start(BundleContext context) throws Exception {
        // we need to set by hand the runtime delegate to avoid letting ServiceFinder discover the implementation
        // which is not working in an OSGi environment
        RuntimeDelegate.setInstance(new RuntimeDelegateImpl());

        instance = this;
        this.context = context;
        toReload = new Vector<Reloadable>();
        app = new CompositeApplication();
        pkgAdm = context.getServiceReference(PackageAdmin.class.getName());
        // start bundle tracker
        tracker = new BundleTracker(context, Bundle.ACTIVE | Bundle.STARTING | Bundle.RESOLVED, this);
        tracker.open();
        //TODO hack to disable service tracker on regular Nuxeo distribs until finding a better solution
        if (!"Nuxeo".equals(context.getProperty(Constants.FRAMEWORK_VENDOR))) {
            httpServiceTracker = new ServiceTracker(context, HttpService.class.getName(), this);
            httpServiceTracker.open();
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (httpServiceTracker != null) {
            httpServiceTracker.close();
            httpServiceTracker = null;
        }
        ServletRegistry.dispose();
        instance = null;
        context.ungetService(pkgAdm);
        pkgAdm = null;
        tracker.close();
        tracker = null;
        toReload = null;
        app = null;
        this.context = null;
    }

    public BundleContext getContext() {
        return context;
    }

    public PackageAdmin getPackageAdmin() {
        return (PackageAdmin)context.getService(pkgAdm);
    }

    public CompositeApplication getApplication() {
        return app;
    }

    public void addReloadListener(Reloadable reloadable) {
        toReload.add(reloadable);
    }

    public void removeReloadListener(Reloadable reloadable) {
        toReload.remove(reloadable);
    }

    /**
     * Reload all jax-rs registries and other interested component that were registered using
     * {@link #addReloadListener(Reloadable)}
     */
    public void reload() {
        for (Reloadable reloadable : toReload.toArray(new Reloadable[toReload.size()])) {
            reloadable.reload();
        }
        app.reload();
        JerseyServlet.invalidate();
    }


    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        String v = (String)bundle.getHeaders().get("Nuxeo-WebModule");
        if (v != null) {
            String className = null;
            Map<String, String> vars = new HashMap<String, String>();
            String varsStr = null;
            int i = v.indexOf(';');
            if (i > -1) {
                className = v.substring(0, i).trim();
                varsStr = v.substring(i+1).trim();
            } else {
                className = v.trim();
            }
            if (varsStr != null) {
                vars = parseAttrs(varsStr);
            }
            try {
                ApplicationProxy ba = new ApplicationProxy(bundle, className, vars);
                app.add(ba);
                reload();
                return ba;
            } catch (Exception e) {
                log.error(e);
            }
        }
        return null;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
        //TODO not yet impl.
        if (event.getType() == BundleEvent.UPDATED) {
            reload();
        }
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        app.remove((ApplicationProxy)object);
        reload();
    }

    protected Map<String,String> parseAttrs(String expr) {
        Map<String, String> map = new HashMap<String, String>();
        String[] ar = StringUtils.split(expr, ';', true);
        for (String a : ar) {
            int i = a.indexOf('=');
            if (i == -1) {
                map.put(a, null);
            } else {
                String key = a.substring(0, i).trim();
                String val = a.substring(i+1).trim();
                if (key.endsWith(":")) {
                    key = key.substring(0, key.length()-1).trim();
                }
                map.put(key, val);
            }
        }
        return map;
    }

    @Override
    public Object addingService(ServiceReference reference) {
        Object service = context.getService(reference);
        try {
            if (service instanceof HttpService) {
                ServletRegistry.getInstance().initHttpService((HttpService)service);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize http service", e);
        }
        return service;
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        try {
            if (ServletRegistry.getInstance().getHttpService() == service) {
                ServletRegistry.getInstance().initHttpService(null);
            }
        } catch (Exception e) {
            log.error("Failed to remove http service", e);
        } finally {
            context.ungetService(reference);
        }
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        try {
            if (ServletRegistry.getInstance().getHttpService() == service) {
                ServletRegistry.getInstance().initHttpService(null);
                ServletRegistry.getInstance().initHttpService((HttpService)service);
            }
        } catch (Exception e) {
            log.error("Failed to update http service", e);
        }
    }
}
