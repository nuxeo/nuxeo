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

package org.nuxeo.connect.services;

import org.nuxeo.connect.connector.ConnectConnector;
import org.nuxeo.connect.downloads.ConnectDownloadManager;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.connect.registration.ConnectRegistrationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestServiceBindings extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.connect.client");
        deployBundle("org.nuxeo.connect.client.wrapper");
    }

    public void testServicesLookup() {
        ConnectRegistrationService crs = Framework.getLocalService(ConnectRegistrationService.class);
        assertNotNull(crs);

        ConnectConnector connector = Framework.getLocalService(ConnectConnector.class);
        assertNotNull(connector);

        ConnectDownloadManager cdm = Framework.getLocalService(ConnectDownloadManager.class);
        assertNotNull(cdm);

        PackageManager pm = Framework.getLocalService(PackageManager.class);
        assertNotNull(pm);
    }

}
