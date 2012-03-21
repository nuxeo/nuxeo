/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */
package org.nuxeo.ecm.core.convert.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.convert.cache.GCTask;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/**
 * Run the GC processing to clean up disk cache.
 *
 * @author mcedica
 */
public class CacheCGTaskActivator implements BundleActivator, FrameworkListener {

    private static final Log log = LogFactory.getLog(CacheCGTaskActivator.class);

    private static final int defaultGCIntervalInMins = 10;

    protected static Thread gcThread;

    @Override
    public void start(BundleContext context) throws Exception {
        context.addFrameworkListener(this);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        endGC();
        context.removeFrameworkListener(this);
    }

    @Override
    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() == FrameworkEvent.STARTED) {
            long interval = ConversionServiceImpl.getGCIntervalInMinutes();
            if (interval <= 0) {
                interval = defaultGCIntervalInMins;
            }
            GCTask.setGCIntervalInMinutes(interval);
            startGC();
        }
    }

    public void startGC() {
        if (!GCTask.GCEnabled) {
            GCTask.GCEnabled = true;
            log.debug("CasheCGTaskActivator activated starting GC thread");
            gcThread = new Thread(new GCTask(), "Nuxeo-Convert-GC");
            gcThread.setDaemon(true);
            gcThread.start();
            log.debug("GC Thread started");
        } else {
            log.debug("GC Thread is already started");
        }

    }

    public void endGC() {
        if (GCTask.GCEnabled) {
            GCTask.GCEnabled = false;
            log.debug("Stoping GC Thread");
            gcThread.interrupt();
        } else {
            log.debug("GC Thread is already stoped");
        }
    }

}
