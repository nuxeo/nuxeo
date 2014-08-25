/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Ian Smith
 *     Florent Guillaume
 */

package org.nuxeo.runtime.osgi;

import java.util.IllegalFormatException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
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

    private static final Log log = LogFactory.getLog(OSGiComponentLoader.class);

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
            bundleDebug("Install bundle: %s " + bundleStateAsString(state),
                    name);
            if ((state & mask) != 0) { // check only resolved bundles
                if (OSGiRuntimeService.getComponentsList(bundle) != null) {
                    bundleDebug("Install bundle: %s component list: " +
                            OSGiRuntimeService.getComponentsList(bundle), name);
                    // check only bundles containing nuxeo comp.
                    try {
                        runtime.createContext(bundle);
                    } catch (Throwable e) {
                        log.warn("Failed to load components for bundle: " +
                                name, e);
                    }
                } else {
                    bundleDebug("Install bundle: %s has no components", name);
                }
            } else {
                bundleDebug("Install bundle: %s is not STARTING "
                        + "or ACTIVE, so no context was created", name);
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

        bundleDebug("Bundle changed: %s " + bundleEventAsString(type), name);
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
                runtime.createContext(bundle);
                bundleDebug("Bundle changed: %s STARTING with components: "
                        + componentsList, name);
                break;
            case BundleEvent.STOPPED:
            case BundleEvent.UNRESOLVED:
                bundleDebug("Bundle changed: %s STOPPING with components: "
                        + componentsList, name);
                runtime.destroyContext(bundle);
                break;
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    /**
     * Used for generating good debug info. Convert bit vector into printable
     * string.
     *
     * @param state bitwise-or of UNINSTALLED, INSTALLED, RESOLVED, STARTING,
     *            STOPPING, and ACTIVE
     * @return printable version of bits that are on
     */
    public static String bundleStateAsString(int state) {
        List<String> list = new LinkedList<String>();
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
        return '[' + StringUtils.join(list, ',') + ']';
    }

    /**
     * Used for generating good debug info. Convert event type into printable
     * string.
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

    /**
     * Prints out a debug message for debugging bundles.
     *
     * @param msg the debug message with a %s in it which will be replaced by
     *            the component name
     * @param name the component name
     */
    public static void bundleDebug(String msg, String name) {
        if (log.isDebugEnabled()) {
            try {
                msg = String.format(msg, name);
            } catch (IllegalFormatException e) {
                // don't fail for this
            }
            log.debug(msg);
        }
    }

}
