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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */
package org.nuxeo.ecm.core.convert.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;

/**
 *
 * Task for GC dedicated Thread
 *
 * @author tiry
 *
 */
public class GCTask implements Runnable {

    public static boolean GCEnabled=false;

    private static long GCInterval=0;

    private static final Log log = LogFactory.getLog(GCTask.class);

    public static long getGCIntervalInMinutes()
    {
        if (GCInterval==0)
        {
            GCInterval = ConversionServiceImpl.getGCIntervalInMinutes();
            log.debug("GC interval set to " + GCInterval);
        }
        return GCInterval;
    }


    public static void setGCIntervalInMinutes(long interval)
    {
        GCInterval=interval;
    }

    public void run() {
        log.debug("starting GC thread");
        while (GCEnabled)
        {
            ConversionCacheGCManager.gcIfNeeded();
            try {
                long gcInterval = getGCIntervalInMinutes();

                if (gcInterval<0)
                {
                    // for tests
                    log.debug("GC sleeps for " + (-gcInterval));
                    Thread.sleep(-gcInterval);
                }
                else
                {
                       log.debug("GC sleeps for " + (gcInterval*60*1000));
                    // GC Interval is stored in minutes
                    Thread.sleep(gcInterval*60*1000);
                }
            } catch (InterruptedException e) {
                GCEnabled=false;
                log.info("GCThread bruttaly interupted");
            }
        }
    }

}
