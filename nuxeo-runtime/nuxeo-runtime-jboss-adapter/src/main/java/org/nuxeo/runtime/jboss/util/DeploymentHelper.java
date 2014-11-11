/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.runtime.jboss.util;

import java.net.URL;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployment.DeploymentInfo;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public final class DeploymentHelper {

    public static final Log log = LogFactory.getLog(DeploymentHelper.class);

    private static ObjectName DEPLOYER_NAME;

    private static MBeanServer jboss;

    // Utility class
    private DeploymentHelper() {
    }

    static {
        try {
            DEPLOYER_NAME = new ObjectName("jboss.system:service=MainDeployer");
            jboss = findMBeanServer();
        } catch (MalformedObjectNameException e) {
            throw new Error("Failed to initialize jboss deployement helper");
        }
    }

    public static MBeanServer findMBeanServer() {
        return MBeanServerLocator.locateJBoss();
    }

    /**
     * Gets the JBoss MBeanServer.
     *
     * @return the MBean JBoss server
     */
    public static MBeanServer getMBeanServer() {
        return jboss;
    }

    public static void deploy(URL url, DeploymentInfo parent) throws Exception {
        log.debug("Deploying " + url);
        try {

            DeploymentInfo di = new DeploymentInfo(url, parent, jboss);
            jboss.invoke(DEPLOYER_NAME, "deploy",
                    new Object[] { di },
                    new String[] { "org.jboss.deployment.DeploymentInfo" });

        } catch (Exception e) {
            throw new Exception("mbean server invocation failed", e);
        }
    }

    public static void deploy(URL url) throws Exception {
        log.debug("Deploying " + url);
        try {
            jboss.invoke(DEPLOYER_NAME,
               "deploy",
               new Object[]{url},
               new String[]{"java.net.URL"});
        } catch (Exception e) {
            throw new Exception("mbean server invocation failed", e);
        }
    }

    public static void undeploy(URL url) throws Exception {
        log.debug("Undeploying " + url);
        try {
            Object[] args = { url };
            String[] sig = { "java.net.URL" };
            jboss.invoke(DEPLOYER_NAME, "undeploy", args, sig);
        } catch (Exception e) {
            throw new Exception("mbean server invocation failed", e);
        }
    }

    public static void redeploy(URL url) throws Exception {
        log.debug("Redeploying " + url);
        try {
            jboss.invoke(DEPLOYER_NAME,
                    "redeploy",
                    new Object[] { url },
                    new String[] { "java.net.URL" });
        } catch (Exception e) {
            throw new Exception("mbean server invocation failed", e);
        }
    }

}
