/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
                try {
                    InetAddress addr = InetAddress.getLocalHost();
                    serverInstanceName = addr.getHostName();
                } catch (UnknownHostException e) {
                    serverInstanceName = "localhost";
                }
            }
        }

        return serverInstanceName;
    }
}
