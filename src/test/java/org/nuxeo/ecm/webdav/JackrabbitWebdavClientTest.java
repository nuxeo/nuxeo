package org.nuxeo.ecm.webdav;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;

public class JackrabbitWebdavClientTest extends AbstractServerTest {

    private static HttpClient client;

    @BeforeClass
    public static void setUp() {
        // Setup code
        HostConfiguration hostConfig = new HostConfiguration();
        hostConfig.setHost("localhost", FunctionalTest.port);

        HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        int maxHostConnections = 20;
        params.setMaxConnectionsPerHost(hostConfig, maxHostConnections);
        connectionManager.setParams(params);

        client = new HttpClient(connectionManager);
        client.setHostConfiguration(hostConfig);

        Credentials creds = new UsernamePasswordCredentials("userId", "pw");
        client.getState().setCredentials(AuthScope.ANY, creds);
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testPropFindOnFolderDepthInfinity() throws Exception {
        DavMethod pFind = new PropFindMethod(ROOT_URI, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_INFINITY);
        client.executeMethod(pFind);

        MultiStatus multiStatus = pFind.getResponseBodyAsMultiStatus();

        //Not quite nice, but for a example ok
        DavPropertySet props = multiStatus.getResponses()[0].getProperties(200);

        Collection<DefaultDavProperty> propertyColl = props.getContent();
        for (DefaultDavProperty prop : propertyColl) {
            System.out.println(prop.getName() + "  " + prop.getValue());
        }
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void testPropFindOnFolderDepthZero() throws Exception {
        DavMethod pFind = new PropFindMethod(ROOT_URI, DavConstants.PROPFIND_ALL_PROP, DavConstants.DEPTH_0);
        client.executeMethod(pFind);

        MultiStatus multiStatus = pFind.getResponseBodyAsMultiStatus();

        //Not quite nice, but for a example ok
        DavPropertySet props = multiStatus.getResponses()[0].getProperties(200);

        Collection<DefaultDavProperty> propertyColl = props.getContent();
        for (DefaultDavProperty prop : propertyColl) {
            System.out.println(prop.getName() + "  " + prop.getValue());
        }
    }

}