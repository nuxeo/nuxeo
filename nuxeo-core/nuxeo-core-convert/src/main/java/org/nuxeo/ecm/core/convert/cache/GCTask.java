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
package org.nuxeo.ecm.core.convert.cache;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;

/**
 * Task for GC dedicated Thread.
 *
 * @author tiry
 */
public class GCTask implements Runnable {

    public static boolean GCEnabled = false;

    private static final Log log = LogFactory.getLog(GCTask.class);

    @Override
    public void run() {
        log.debug("starting GC thread");
        while (GCEnabled) {
            ConversionCacheGCManager.gcIfNeeded();
            try {
                long gcInterval = ConversionServiceImpl.getGCIntervalInMinutes();

                if (gcInterval < 0) {
                    // for tests
                    log.debug("GC sleeps for " + -gcInterval);
                    Thread.sleep(-gcInterval);
                } else {
                    log.debug("GC sleeps for " + gcInterval * 60 * 1000);
                    // GC Interval is stored in minutes
                    Thread.sleep(TimeUnit.MILLISECONDS.convert(gcInterval,
                            TimeUnit.MINUTES));
                }
            } catch (InterruptedException e) {
                GCEnabled = false;
                log.info("GCThread bruttaly interupted");
            }
        }
    }

}
