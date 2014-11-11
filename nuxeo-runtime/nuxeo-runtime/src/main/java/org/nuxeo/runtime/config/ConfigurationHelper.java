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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.config;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ConfigurationHelper {

    // Utility class.
    private ConfigurationHelper() {
    }

    /**
     * Returns the normalized host if not already normalized otherwise
     * return null.
     * <p>
     * The normalization replaces special hosts like 0.0.0.0, 127.0.0.1, localhost
     * with the given newHost.
     */
    public static String getNormalizedURI(String uri, String newHost) {
        int p;
        if ((p = uri.indexOf("://0.0.0.0")) > -1) {
            return uri.substring(0, p+3)+newHost+uri.substring(p+10);
        } else if ((p = uri.indexOf("://127.0.0.1")) > -1) {
            return uri.substring(0, p+3)+newHost+uri.substring(p+12);
        } else if ((p = uri.indexOf("://localhost")) > -1) {
            return uri.substring(0, p+3)+newHost+uri.substring(p+12);
        } else {
            return null;
        }
    }

    public static String getNormalizedHost(String host, String newHost) {
        if (host.equals("0.0.0.0") || host.equals("127.0.0.1") || host.equals("localhost")) {
            return newHost;
        }
        return null;
    }

}
