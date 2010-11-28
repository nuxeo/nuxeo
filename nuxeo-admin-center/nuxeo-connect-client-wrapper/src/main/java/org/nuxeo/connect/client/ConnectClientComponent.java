/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.connect.client;

import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.connect.connector.ConnectConnector;
import org.nuxeo.connect.downloads.ConnectDownloadManager;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.registration.ConnectRegistrationService;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Nuxeo Runtime Component used to wrap nuxeo-connect-client services as Nuxeo Services.
 * <p>
 * This is required because nuxeo-connect-client can not depend on Nuxeo Runtime,
 * so this wrapper manages the integration and the callbacks needed.
 *
 * @author tiry
 */
public class ConnectClientComponent extends DefaultComponent {

    @Override
    public void activate(ComponentContext context) throws Exception {
        NuxeoConnectClient.setCallBackHolder(new NuxeoCallbackHolder());
    }

    // Wrap connect client services as Nuxeo Services
    public <T> T getAdapter(Class<T> adapter) {

        if (adapter.getCanonicalName().equals(
                ConnectConnector.class.getCanonicalName())) {
            return adapter.cast(NuxeoConnectClient.getConnectConnector());
        }

        if (adapter.getCanonicalName().equals(
                ConnectRegistrationService.class.getCanonicalName())) {
            return adapter.cast(NuxeoConnectClient
                    .getConnectRegistrationService());
        }

        if (adapter.getCanonicalName().equals(
                ConnectDownloadManager.class.getCanonicalName())) {
            return adapter.cast(NuxeoConnectClient.getDownloadManager());
        }

        if (adapter.getCanonicalName().equals(
                PackageManager.class.getCanonicalName())) {
            return adapter.cast(NuxeoConnectClient.getPackageManager());
        }

        if (adapter.getCanonicalName().equals(
                PackageUpdateService.class.getCanonicalName())) {
            return adapter.cast(NuxeoConnectClient.getPackageUpdateService());
        }

        return adapter.cast(this);
    }

}
