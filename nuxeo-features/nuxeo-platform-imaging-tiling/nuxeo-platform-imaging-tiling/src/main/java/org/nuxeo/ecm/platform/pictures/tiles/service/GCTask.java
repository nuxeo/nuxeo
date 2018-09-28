/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */
package org.nuxeo.ecm.platform.pictures.tiles.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Task for GC dedicated Thread
 *
 * @author tiry
 */
public class GCTask implements Runnable {

    protected static volatile boolean GCEnabled = false;

    private static final String GCINTERVAL_KEY = "GCInterval";

    private static long GC_INTERVAL = 0;

    private static final Log log = LogFactory.getLog(GCTask.class);

    public static long getGCIntervalInMinutes() {
        if (GC_INTERVAL == 0) {
            GC_INTERVAL = Long.parseLong(PictureTilingComponent.getEnvValue(GCINTERVAL_KEY, Long.toString(10)));
            log.debug("GC interval set to " + GC_INTERVAL);
        }
        return GC_INTERVAL;
    }

    public static void setGCIntervalInMinutes(long interval) {
        GC_INTERVAL = interval;
    }

    public void run() {
        log.debug("starting GC thread");
        while (GCEnabled) {
            PictureTilingCacheGCManager.gcIfNeeded();
            try {
                long gcInterval = getGCIntervalInMinutes();

                if (gcInterval < 0) {
                    // for tests
                    log.debug("GC sleeps for " + (-gcInterval));
                    Thread.sleep(-gcInterval);
                } else {
                    log.debug("GC sleeps for " + (gcInterval * 60 * 1000));
                    // GC Interval is stored in minutes
                    Thread.sleep(gcInterval * 60 * 1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                GCEnabled = false;
            }
        }
    }

}
