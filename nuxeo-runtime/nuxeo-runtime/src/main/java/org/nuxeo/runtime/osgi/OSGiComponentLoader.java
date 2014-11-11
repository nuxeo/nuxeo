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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * @author Bogdan Stefanescu
 * @author Ian Smith
 * @author Florent Guillaume
 */
public class OSGiComponentLoader implements BundleTrackerCustomizer {

    private static final Log log = LogFactory.getLog(OSGiComponentLoader.class);

    protected final OSGiRuntimeService runtime;

    protected BundleTracker tracker;

    public OSGiComponentLoader(OSGiRuntimeService runtime) {
        this.runtime = runtime;
    }

    public void start() {
        tracker = new BundleTracker(runtime.getBundleContext(),  Bundle.RESOLVED
                | Bundle.STARTING | Bundle.ACTIVE, this);
        tracker.open();
    }

    public void stop() {
        try {
            tracker.close();
        } finally {
            tracker = null;
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

    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        try {
            log.info("building runtime context " + bundle);
            runtime.installBundle(bundle);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to resolve components for bundle: " + bundle, e);
        }
        bundleDebug("Registered bundle: %s component list: "
                + OSGiRuntimeService.getComponentsList(bundle), bundle.getSymbolicName());
        return bundle;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
        if (event.getType() == BundleEvent.STARTED) {
            try {
                runtime.activateContext(bundle);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot activate " + bundle, e);
            }
        }
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        runtime.destroyContext(bundle);
    }

}
