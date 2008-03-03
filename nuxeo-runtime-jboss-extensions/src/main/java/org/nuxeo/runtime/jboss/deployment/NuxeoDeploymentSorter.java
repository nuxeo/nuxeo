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

package org.nuxeo.runtime.jboss.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployment.DeploymentSorter;
import org.jboss.system.server.ServerConfig;
import org.jboss.system.server.ServerConfigLocator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// FIXME: not used? Otherwise, document.
public class NuxeoDeploymentSorter extends DeploymentSorter implements Serializable {

    private static final long serialVersionUID = 4635486588780702177L;
    private static final Log log  = LogFactory.getLog(NuxeoDeploymentSorter.class);

    private final Map<String, Integer> bundlesOrder;

    public NuxeoDeploymentSorter() {
        bundlesOrder = new HashMap<String, Integer>();
        ServerConfig config = ServerConfigLocator.locate();
        try {
            URL configFile = new URL(config.getServerConfigURL(),
                    "deployment-order");
            loadConfigFile(configFile);
        } catch (Exception e) {
            log.warn("Failed to load deployment-order file");
        }
    }

    private void loadConfigFile(URL configFile) throws IOException {
        InputStream in = configFile.openStream();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = reader.readLine().trim();
            int i = 0;
            while (line != null) {
                if (line.length() > 0 && line.charAt(0) != '#') {
                    bundlesOrder.put(line, i++);
                }
                line = reader.readLine();
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private static String getFileName(URL url) {
        String path = url.getPath();
        int len = path.length();
        if (len == 0) {
            return "";
        }
        int offset = path.charAt(len - 1) == '/' ? len - 2 : len - 1;
        int p = path.lastIndexOf('/', offset);
        if (p > -1) {
            return path.substring(p + 1);
        }
        return path;
    }

    /**
     * Returns a negative number if o1 appears lower in the suffix order than
     * o2.
     * <p>
     * If the suffixes are indentical, then sorts based on nuxeo dependencies and
     * then based on name.
     * This is so that deployment order of components is always identical.
     */
    @Override
    public int compare(Object o1, Object o2) {
        URL u1 = (URL) o1;
        URL u2 = (URL) o2;

        // check if these are nuxeo bundles and have deployment orders
        String name1 = getFileName(u1);
        Integer i1 = bundlesOrder.get(name1);
        if (i1 != null) {
            String name2 = getFileName(u2);
            Integer i2 = bundlesOrder.get(name2);
            if (i2 != null) {
                return i1 - i2;
            } else {
                return 1; // nuxeo bundles are deployed at end
            }
        }

        String name2 = getFileName(u2);
        Integer i2 = bundlesOrder.get(name2);
        if (i2 != null) {
            return -1; // nuxeo bundles are deployed at end
        }

        // orig. deployment sorter algo
        int order = getExtensionIndex(u1) - getExtensionIndex(u2);
        if (order != 0) {
            return order;
        }

        // sort by name
        return u1.getFile().compareTo(u2.getFile());
    }

}
