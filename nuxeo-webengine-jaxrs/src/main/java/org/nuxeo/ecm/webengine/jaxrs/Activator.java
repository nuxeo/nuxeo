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
package org.nuxeo.ecm.webengine.jaxrs;


import java.util.List;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Activator implements BundleActivator, BundleTrackerCustomizer {

    private static Activator instance;

    public static Activator getInstance() {
        return instance;
    }

    protected BundleContext context;
    protected BundleTracker tracker;

    protected CompositeApplication app;

    protected List<Reloadable> toReload;

    @Override
    public void start(BundleContext context) throws Exception {
        instance = this;
        this.context = context;
        toReload = new Vector<Reloadable>();
        app = new CompositeApplication();
        tracker = new BundleTracker(context, Bundle.ACTIVE | Bundle.STARTING | Bundle.RESOLVED, this);
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        instance = null;
        tracker.close();
        tracker = null;
        toReload = null;
        app = null;
        context = null;
    }

    public BundleContext getContext() {
        return context;
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
    }


    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        String v = (String)bundle.getHeaders().get("Nuxeo-WebModule");
        if (v != null) {
            String className = null;
            int i = v.indexOf(';');
            if (i > -1) {
                className = v.substring(0, i);
            } else {
                className = v;
            }
            try {
                BundledApplication ba = new BundledApplication(bundle, className);
                app.add(ba);
                reload();
                return ba;
            } catch (Exception e) {
                e.printStackTrace(); //TODO log
            }
        }
        return null;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
        //TODO not yet impl.
        reload();
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        app.remove((BundledApplication)object);
        reload();
    }

}
