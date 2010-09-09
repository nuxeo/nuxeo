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
