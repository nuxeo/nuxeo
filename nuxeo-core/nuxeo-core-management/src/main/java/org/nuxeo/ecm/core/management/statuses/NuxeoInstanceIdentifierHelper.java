/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.core.management.statuses;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.runtime.api.Framework;

public class NuxeoInstanceIdentifierHelper {

    protected static String serverInstanceName;

    public static String getServerInstanceName() {
        if (serverInstanceName == null) {
            serverInstanceName = Framework.getProperties().getProperty(
                    AdministrativeStatusManager.ADMINISTRATIVE_INSTANCE_ID);
            if (StringUtils.isEmpty(serverInstanceName)) {
                InetAddress addr;
                try {
                    addr = InetAddress.getLocalHost();
                    serverInstanceName = addr.getHostName();
                } catch (UnknownHostException e) {
                    serverInstanceName = "localhost";
                }
            }
        }

        return serverInstanceName;
    }
}
