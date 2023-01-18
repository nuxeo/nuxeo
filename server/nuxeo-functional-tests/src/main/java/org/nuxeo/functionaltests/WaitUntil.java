/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 */
package org.nuxeo.functionaltests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class WaitUntil {

    private static final Logger log = LogManager.getLogger(WaitUntil.class);

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
