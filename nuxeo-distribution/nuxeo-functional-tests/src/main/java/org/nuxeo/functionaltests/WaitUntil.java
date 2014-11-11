/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.functionaltests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class WaitUntil {

    public static final Log log = LogFactory.getLog(WaitUntil.class);

    long timeout;

    public WaitUntil(long timeout) {
        this.timeout = timeout;
    }

    public WaitUntil() {
        // default value 2 secs
        timeout = 2000;
    }

    public void waitUntil() {
        long starttime = System.currentTimeMillis();
        Exception lastException = null;

        while (starttime > System.currentTimeMillis() - timeout) {
            try {
                if (condition()) {
                    return;
                }
                Thread.sleep(100);
                lastException = null;
            } catch (Exception e) {
                log.warn("An exception while testing condition", e);
                lastException = e;
            }

        }
        throw new RuntimeException("Couldn't find element", lastException);
    }

    public abstract boolean condition();
}
