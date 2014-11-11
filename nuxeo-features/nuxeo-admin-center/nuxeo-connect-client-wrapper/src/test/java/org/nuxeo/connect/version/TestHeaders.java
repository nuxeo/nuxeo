package org.nuxeo.connect.version;

import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestHeaders extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.connect.client");
        deployBundle("org.nuxeo.connect.client.wrapper");
    }


    public void testVersion() {
        String buildVersion = NuxeoConnectClient.getBuildVersion();
        System.out.println("Build version=" + buildVersion);
        String version = NuxeoConnectClient.getVersion();
        System.out.println("Version=" + version);

    }
}
