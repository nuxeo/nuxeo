/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

    public boolean GCEnabled = true;

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
                    Thread.sleep(TimeUnit.MILLISECONDS.convert(gcInterval, TimeUnit.MINUTES));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                GCEnabled = false;
            }
        }
    }

}
