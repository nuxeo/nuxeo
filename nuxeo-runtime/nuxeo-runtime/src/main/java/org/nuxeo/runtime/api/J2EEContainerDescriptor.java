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
 */

package org.nuxeo.runtime.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;

/**
 * Helper class to get container related properties.
 *
 * @author Stephane Lacoin
 * @author bstefanescu
 * @author Florent Guillaume
 */
public enum J2EEContainerDescriptor {

    JBOSS, TOMCAT, JETTY, GF3;

    public static final Log log = LogFactory.getLog(J2EEContainerDescriptor.class);

    static J2EEContainerDescriptor autodetect() {
        String hostName = Environment.getDefault().getHostApplicationName();
        if (hostName == null) {
            return null;
        }

        if (Environment.JBOSS_HOST.equals(hostName)) {
            log.info("Detected JBoss host");
            return JBOSS;
        } else if (Environment.TOMCAT_HOST.equals(hostName)) {
            log.info("Detected Tomcat host");
            return TOMCAT;
        } else if (Environment.NXSERVER_HOST.equals(hostName)) {
            // can be jetty or gf3 embedded
            try {
                Class.forName("com.sun.enterprise.glassfish.bootstrap.AbstractMain");
                log.info("Detected GlassFish host");
                return GF3;
            } catch (ClassNotFoundException e) {
                log.debug("Autodetect : not a glassfish host");
            }
            try {
                Class.forName("org.mortbay.jetty.webapp.WebAppContext");
                log.info("Detected Jetty host");
                return JETTY;
            } catch (ClassNotFoundException e) {
                log.debug("Autodetect : not a jetty host");
            }
            return null; // unknown host
        } else {
            return null; // unknown host
        }
    }

    static J2EEContainerDescriptor selected;

    public static J2EEContainerDescriptor getSelected() {
        if (selected == null) {
            selected = autodetect();
        }
        return selected;
    }

}
