/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.utils.StringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ApplicationManager implements BundleTrackerCustomizer {

    public final static String HOST_ATTR = "host";

    public final static String DEFAULT_HOST = "default";

    private final static ApplicationManager instance = new ApplicationManager();

    public static ApplicationManager getInstance() {
        return instance;
    }

    protected BundleTracker tracker;

    protected final Map<String, ApplicationHost> apps = new HashMap<>();

    public ApplicationManager() {
    }

    public synchronized void start(BundleContext context) {
        tracker = new BundleTracker(context, Bundle.ACTIVE | Bundle.STARTING | Bundle.RESOLVED, this);
        tracker.open();
    }

    public synchronized void stop(BundleContext context) {
        tracker.close();
        tracker = null;
        apps.clear();
    }

    public synchronized ApplicationHost getOrCreateApplication(String name) {
        ApplicationHost host = apps.get(name);
        if (host == null) {
            host = new ApplicationHost(name);
            apps.put(name, host);
        }
        return host;
    }

    public synchronized ApplicationHost[] getApplications() {
        return apps.values().toArray(new ApplicationHost[apps.size()]);
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
        String v = (String) bundle.getHeaders().get("Nuxeo-WebModule");
        if (v != null) {
            String classRef;
            Map<String, String> vars = new HashMap<>();
            String varsStr = null;
            int i = v.indexOf(';');
            if (i > -1) {
                classRef = v.substring(0, i).trim();
                varsStr = v.substring(i + 1).trim();
            } else {
                classRef = v.trim();
            }
            if (varsStr != null) {
                vars = parseAttrs(varsStr);
            }
            ApplicationFragment fragment = new ApplicationFragment(bundle, classRef, vars);
            ApplicationHost app = getOrCreateApplication(fragment.getHostName());
            app.add(fragment);
            app.reload();
            return fragment;
        }
        return null;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
        // TODO not yet impl.
        if (event.getType() == BundleEvent.UPDATED) {
            ApplicationFragment fragment = (ApplicationFragment) object;
            if (fragment != null) {
                ApplicationHost app = getApplication(fragment);
                if (app != null) {
                    app.reload();
                }
            }
        }
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        ApplicationFragment fragment = (ApplicationFragment) object;
        if (fragment != null) {
            ApplicationHost app = getApplication(fragment);
            if (app != null) {
                app.remove(fragment);
                app.reload();
            }
        }
    }

    protected Map<String, String> parseAttrs(String expr) {
        Map<String, String> map = new HashMap<>();
        String[] ar = StringUtils.split(expr, ';', true);
        for (String a : ar) {
            int i = a.indexOf('=');
            if (i == -1) {
                map.put(a, null);
            } else {
                String key = a.substring(0, i).trim();
                String val = a.substring(i + 1).trim();
                if (key.endsWith(":")) {
                    key = key.substring(0, key.length() - 1).trim();
                }
                map.put(key, val);
            }
        }
        return map;
    }

}
