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

import java.lang.reflect.Method;
import java.net.URL;

/**
 * Helper for log4j.
 *
 * @since 9.2
 */
public class Log4JHelper {

    public static final String DOM_CONFIGURATOR_CLASS = "org.apache.log4j.xml.DOMConfigurator";

    public static final String CONFIGURE_AND_WATCH_METHOD = "configureAndWatch";

    public static final String LOG4J_XML = "log4j.xml";

    // utility class
    private Log4JHelper() {
    }

    /**
     * Calls log4j's DOMConfigurator.configureAndWatch method, if available, to automatically watch and reload the
     * configuration file if needed.
     *
     * @param delay the delay (in milliseconds)
     * @return {@code true} if log4j is available and the call succeeded
     */
    public static boolean configureAndWatch(long delay) {
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(LOG4J_XML);
            if (url == null) {
                return false;
            }
            String filename = url.getFile();
            Class<?> klass = Class.forName(DOM_CONFIGURATOR_CLASS);
            Method method = klass.getMethod(CONFIGURE_AND_WATCH_METHOD, String.class, long.class);
            method.invoke(null, filename, Long.valueOf(delay));
        } catch (ReflectiveOperationException | SecurityException e) {
            return false;
        }
        return true;
    }

}
