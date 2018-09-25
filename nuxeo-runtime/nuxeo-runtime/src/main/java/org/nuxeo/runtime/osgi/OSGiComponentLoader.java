/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Ian Smith
 *     Florent Guillaume
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.osgi;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

/**
 * @author Bogdan Stefanescu
 * @author Ian Smith
 * @author Florent Guillaume
 */
public class OSGiComponentLoader implements SynchronousBundleListener {

    private static final Logger log = LogManager.getLogger();

    private final OSGiRuntimeService runtime;

    public OSGiComponentLoader(OSGiRuntimeService runtime) {
        this.runtime = runtime;
        install();
    }

    public void install() {
        BundleContext ctx = runtime.getBundleContext();
        ctx.addBundleListener(this);
        Bundle[] bundles = ctx.getBundles();
        int mask = Bundle.STARTING | Bundle.ACTIVE;
        for (Bundle bundle : bundles) {
            String name = bundle.getSymbolicName();
            runtime.bundles.put(name, bundle);
            int state = bundle.getState();
            log.debug("Install bundle: {} {}", () -> name, () -> bundleStateAsString(state));
            if ((state & mask) != 0) { // check only resolved bundles
                if (OSGiRuntimeService.getComponentsList(bundle) != null) {
                    log.debug("Install bundle: {} component list: {}", name,
                            OSGiRuntimeService.getComponentsList(bundle));
                    // check only bundles containing nuxeo comp.
                    try {
                        runtime.createContext(bundle);
                    } catch (RuntimeException e) {
                        // don't raise this exception,
                        // we want to isolate bundle errors from other bundles
                        log.warn("Failed to load components for bundle: " + name, e);
                    }
                } else {
                    log.debug("Install bundle: {} has no components", name);
                }
            } else {
                log.debug("Install bundle: {} is not STARTING or ACTIVE, so no context was created", name);
            }
        }
    }

    public void uninstall() {
        runtime.getBundleContext().removeBundleListener(this);
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        String name = event.getBundle().getSymbolicName();
        int type = event.getType();

        log.trace("Bundle changed: {} {}", () -> name, () -> bundleEventAsString(type));
        try {
            Bundle bundle = event.getBundle();
            String componentsList = OSGiRuntimeService.getComponentsList(bundle);
            switch (type) {
            case BundleEvent.INSTALLED:
                runtime.bundles.put(bundle.getSymbolicName(), bundle);
                break;
            case BundleEvent.UNINSTALLED:
                runtime.bundles.remove(bundle.getSymbolicName());
                break;
            case BundleEvent.STARTING:
            case BundleEvent.LAZY_ACTIVATION:
                if (componentsList != null) {
                    log.trace("Bundle changed: {} STARTING with components: ", name, componentsList);
                    runtime.createContext(bundle);
                } else {
                    log.trace("Bundle changed: {} STARTING with no components", name);
                }
                break;
            case BundleEvent.STOPPED:
            case BundleEvent.UNRESOLVED:
                if (componentsList != null) {
                    log.trace("Bundle changed: {} STOPPING with components: ", name, componentsList);
                    runtime.destroyContext(bundle);
                } else {
                    log.trace("Bundle changed: {} STOPPING with no components", name);
                }
                break;
            }
        } catch (RuntimeException e) {
            log.error(e, e);
        }
    }

    /**
     * Used for generating good debug info. Convert bit vector into printable string.
     *
     * @param state bitwise-or of UNINSTALLED, INSTALLED, RESOLVED, STARTING, STOPPING, and ACTIVE
     * @return printable version of bits that are on
     */
    public static String bundleStateAsString(int state) {
        List<String> list = new LinkedList<>();
        if ((state & Bundle.UNINSTALLED) != 0) {
            list.add("UNINSTALLED");
        }
        if ((state & Bundle.INSTALLED) != 0) {
            list.add("INSTALLED");
        }
        if ((state & Bundle.RESOLVED) != 0) {
            list.add("RESOLVED");
        }
        if ((state & Bundle.STARTING) != 0) {
            list.add("STARTING");
        }
        if ((state & Bundle.STOPPING) != 0) {
            list.add("STOPPING");
        }
        if ((state & Bundle.ACTIVE) != 0) {
            list.add("ACTIVE");
        }
        return '[' + String.join(",", list) + ']';
    }

    /**
     * Used for generating good debug info. Convert event type into printable string.
     *
     * @param eventType INSTALLED, STARTED,STOPPED, UNINSTALLED,UPDATED
     * @return printable version of event type
     */
    public static String bundleEventAsString(int eventType) {
        switch (eventType) {
        case BundleEvent.INSTALLED:
            return "INSTALLED";
        case BundleEvent.STARTED:
            return "STARTED";
        case BundleEvent.STARTING:
            return "STARTING";
        case BundleEvent.STOPPED:
            return "STOPPED";
        case BundleEvent.UNINSTALLED:
            return "UNINSTALLED";
        case BundleEvent.UPDATED:
            return "UPDATED";
        case BundleEvent.LAZY_ACTIVATION:
            return "LAZY_ACTIVATION";
        case BundleEvent.RESOLVED:
            return "RESOLVED";
        case BundleEvent.UNRESOLVED:
            return "UNRESOLVED";
        case BundleEvent.STOPPING:
            return "STOPPING";
        default:
            return "UNKNOWN_OSGI_EVENT_TYPE_" + eventType;
        }
    }

}
