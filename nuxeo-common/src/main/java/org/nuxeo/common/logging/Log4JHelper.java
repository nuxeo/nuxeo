/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.common.logging;

import java.net.URL;

/**
 * Helper for log4j.
 *
 * @since 9.2
 */
public class Log4JHelper {

    // utility class
    private Log4JHelper() {
    }

    /**
     * Calls log4j's DOMConfigurator.configureAndWatch method, if available, to automatically watch and reload the
     * configuration file if needed.
     *
     * @param delay
     *            the delay (in milliseconds)
     * @return {@code true} if log4j is available and the call succeeded
     */
    public static Log4jWatchdogHandle configureAndWatch(long delay) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        URL url = contextClassLoader.getResource("log4j.xml");
        if (url == null) {
            return null;
        }
        try {
            return Log4jWatchdog.watch(url.getFile(), delay);
        } catch (LinkageError cause) {
            return null;
        }
    }

}
