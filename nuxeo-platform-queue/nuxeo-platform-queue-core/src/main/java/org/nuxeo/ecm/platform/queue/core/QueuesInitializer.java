/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.platform.queue.core;

import org.nuxeo.ecm.platform.queue.api.QueueLocator;
import org.nuxeo.ecm.platform.queue.api.QueueManager;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/**
 * Initialize the contributed queues at startup
 *
 * @author matic
 *
 */
public class QueuesInitializer implements FrameworkListener {

    protected final QueueLocator locator;

    protected QueuesInitializer(QueueLocator locator) {
        this.locator = locator;
    }

    @Override
    public void frameworkEvent(FrameworkEvent event) {
        if (event.getType() != FrameworkEvent.STARTED) {
            return;
        }
        event.getBundle().getBundleContext().removeFrameworkListener(this);

        // Replace OSGI class loader by the standard java class loader used for
        // loading this class
        Thread currentThread = Thread.currentThread();
        ClassLoader previous = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(QueuesInitializer.class.getClassLoader());
        try {
            doInitialize();
        } finally {
            currentThread.setContextClassLoader(previous);
        }
    }

    void doInitialize() {
        for (QueueManager<?> mgr : locator.getManagers()) {
            mgr.initialize();
        }
    }
}
