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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ApplicationManager implements BundleTrackerCustomizer {

    private static final Log log = LogFactory.getLog(ApplicationManager.class);

    public final static String HOST_ATTR = "host";
    public final static String DEFAULT_HOST = "default";

    private final static ApplicationManager instance = new ApplicationManager();

    public static ApplicationManager getInstance() {
        return instance;
    }

    protected BundleTracker tracker;

    protected Map<String, ApplicationHost> apps;


    public ApplicationManager() {
    }

    public synchronized void start(BundleContext context) {
        apps = new HashMap<String, ApplicationHost>();
        tracker = new BundleTracker(context, Bundle.ACTIVE | Bundle.STARTING | Bundle.RESOLVED, this);
        tracker.open();
    }

    public synchronized void stop(BundleContext context) {
        tracker.close();
        tracker = null;
        apps = null;
    }

    public synchronized ApplicationHost getOrCreateApplication(String name) {
        ApplicationHost host = apps.get(name);
        if (host == null) {
            host = new ApplicationHost(name);
            apps.put(name, host);
        }
        return host;
    }

    public synchronized ApplicationHost getApplication(String name) {
        return apps.get(name);
    }

    public synchronized ApplicationHost getApplication(ApplicationFragment fragment) {
        String host = fragment.getHostName();
        return apps.get(host);
    }


    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        String v = (String)bundle.getHeaders().get("Nuxeo-WebModule");
        if (v != null) {
            String classRef = null;
            Map<String, String> vars = new HashMap<String, String>();
            String varsStr = null;
            int i = v.indexOf(';');
            if (i > -1) {
                classRef = v.substring(0, i).trim();
                varsStr = v.substring(i+1).trim();
            } else {
                classRef = v.trim();
            }
            if (varsStr != null) {
                vars = parseAttrs(varsStr);
            }
            try {
                ApplicationFragment fragment = new ApplicationFragment(bundle, classRef, vars);
                ApplicationHost app = getOrCreateApplication(fragment.getHostName());
                app.add(fragment);
                app.reload();
                return fragment;
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
            ApplicationFragment fragment = (ApplicationFragment)object;
            if (fragment != null) {
                ApplicationHost app = getApplication(fragment);
                if (app != null) {
                    try {
                        app.reload();
                    } catch (Exception e) {
                        log.error(e);
                    }
                }
            }
        }
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        ApplicationFragment fragment = (ApplicationFragment)object;
        if (fragment != null) {
            ApplicationHost app = getApplication(fragment);
            if (app != null) {
                app.remove(fragment);
                try {
                    app.reload();
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
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

}
