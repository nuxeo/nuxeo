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
 *     Thomas Roger <troger@nuxeo.com>
 */
package org.nuxeo.opensocial.helper;

import static org.nuxeo.launcher.config.Environment.NUXEO_LOOPBACK_URL;
import static org.nuxeo.launcher.config.Environment.OPENSOCIAL_GADGETS_EMBEDDED_SERVER;
import static org.nuxeo.launcher.config.Environment.OPENSOCIAL_GADGETS_HOST;
import static org.nuxeo.launcher.config.Environment.OPENSOCIAL_GADGETS_PORT;

import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.2
 */
public class OpenSocialGadgetHelper {

    public static final String HTTP = "http://";

    public static final String HTTP_SEPARATOR = ":";

    private OpenSocialGadgetHelper() {
        // Helper class
    }

    public static String getGadgetsBaseUrl(boolean relativeUrl) {
        return getGadgetsBaseUrl(relativeUrl, true);
    }

    public static String getGadgetsBaseUrl(boolean relativeUrl, boolean addContextPath) {
        boolean gadgetsEmbeddedServer = Boolean.valueOf(Framework.getProperty(
                OPENSOCIAL_GADGETS_EMBEDDED_SERVER, "true"));
        StringBuilder sb = new StringBuilder();
        if (gadgetsEmbeddedServer) {
            if (!relativeUrl) {
                sb.append(Framework.getProperty(NUXEO_LOOPBACK_URL));
            } else {
                sb.append(VirtualHostHelper.getContextPathProperty());
            }
        } else {
            sb.append(HTTP);
            sb.append(Framework.getProperty(OPENSOCIAL_GADGETS_HOST));
            sb.append(HTTP_SEPARATOR);
            sb.append(Framework.getProperty(OPENSOCIAL_GADGETS_PORT));
            if (addContextPath) {
                sb.append(VirtualHostHelper.getContextPathProperty());
            }
        }
        return sb.toString();
    }

    public static String computeGadgetDefUrlBeforeSave(String gadgetDef) {
        String urlToCheck = getGadgetsBaseUrl(false);
        if (gadgetDef.contains(urlToCheck)) {
            gadgetDef = gadgetDef.split(urlToCheck)[1];
        }
        return gadgetDef;
    }

    public static String computeGadgetDefUrlAfterLoad(String gadgetDefUrl) {
        if (!gadgetDefUrl.contains("://")) {
            gadgetDefUrl = getGadgetsBaseUrl(false) + gadgetDefUrl;
        }
        return gadgetDefUrl;
    }

}
