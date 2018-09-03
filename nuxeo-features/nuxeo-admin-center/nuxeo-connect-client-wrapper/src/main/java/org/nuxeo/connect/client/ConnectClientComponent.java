/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.connect.client;

import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.connect.connector.ConnectConnector;
import org.nuxeo.connect.downloads.ConnectDownloadManager;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.registration.ConnectRegistrationService;
import org.nuxeo.connect.update.PackageUpdateService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Nuxeo Runtime Component used to wrap nuxeo-connect-client services as Nuxeo Services.
 * <p>
 * This is required because nuxeo-connect-client can not depend on Nuxeo Runtime, so this wrapper manages the
 * integration and the callbacks needed.
 *
 * @author tiry
 */
public class ConnectClientComponent extends DefaultComponent {

    /**
     * Name of the {@link ConfigurationService} property used to disable Studio snapshot package validation.
     *
     * @since 10.3
     */
    public static final String STUDIO_SNAPSHOT_DISABLE_VALIDATION_PROPERTY = "studio.snapshot.disablePkgValidation";

    @Override
    public void activate(ComponentContext context) {
        NuxeoConnectClient.setCallBackHolder(new NuxeoCallbackHolder());
    }

    // Wrap connect client services as Nuxeo Services
    public <T> T getAdapter(Class<T> adapter) {

        if (adapter.getCanonicalName().equals(ConnectConnector.class.getCanonicalName())) {
            return adapter.cast(NuxeoConnectClient.getConnectConnector());
        }

        if (adapter.getCanonicalName().equals(ConnectRegistrationService.class.getCanonicalName())) {
            return adapter.cast(NuxeoConnectClient.getConnectRegistrationService());
        }

        if (adapter.getCanonicalName().equals(ConnectDownloadManager.class.getCanonicalName())) {
            return adapter.cast(NuxeoConnectClient.getDownloadManager());
        }

        if (adapter.getCanonicalName().equals(PackageManager.class.getCanonicalName())) {
            return adapter.cast(NuxeoConnectClient.getPackageManager());
        }

        if (adapter.getCanonicalName().equals(PackageUpdateService.class.getCanonicalName())) {
            return adapter.cast(NuxeoConnectClient.getPackageUpdateService());
        }

        return adapter.cast(this);
    }

}
