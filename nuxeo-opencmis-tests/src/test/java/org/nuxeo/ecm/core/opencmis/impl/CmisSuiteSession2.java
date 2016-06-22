/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.impl.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManagerComponent;
import org.nuxeo.ecm.core.blob.BlobProviderDescriptor;
import org.nuxeo.ecm.core.opencmis.tests.Helper;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * Suite of CMIS tests with minimal setup, checking HTTP headers.
 */
@RunWith(FeaturesRunner.class)
@Features(CmisFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class CmisSuiteSession2 {

    protected static final String USERNAME = "Administrator";

    protected static final String PASSWORD = "test";

    protected static final String BASIC_AUTH = "Basic " + Base64.encodeBytes((USERNAME + ":" + PASSWORD).getBytes());

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected TransactionalFeature txFeature;


    @Inject
    protected CmisFeatureSession cmisFeatureSession;

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected BlobManager blobManager;

    @Inject
    protected Session session;

    protected boolean isAtomPub;

    protected boolean isBrowser;

    protected static class NeverRedirectStrategy extends DefaultRedirectStrategy {
        public static final NeverRedirectStrategy INSTANCE = new NeverRedirectStrategy();

        @Override
        public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context)
                throws ProtocolException {
            return false;
        }
    }

    @Before
    public void setUp() throws Exception {
        useDummyCmisBlobProvider();
        setUpData();
        session.clear(); // clear cache

        isAtomPub = cmisFeatureSession.isAtomPub;
        isBrowser = cmisFeatureSession.isBrowser;
    }

    /**
     * Use a BlobProvider that can redirect to a different URI for download.
     */
    protected void useDummyCmisBlobProvider() {
        BlobManagerComponent blobManagerComponent = (BlobManagerComponent) blobManager;
        BlobProviderDescriptor descr = new BlobProviderDescriptor();
        descr.name = coreSession.getRepositoryName();
        descr.klass = DummyCmisBlobProvider.class;
        blobManagerComponent.registerBlobProvider(descr);
    }

    protected void setUpData() throws Exception {
        Helper.makeNuxeoRepository(coreSession);
        txFeature.nextTransaction();
        coreFeature.getStorageConfiguration().sleepForFulltext();
    }

    protected String getURI() {
        Document file = (Document) session.getObjectByPath("/testfolder1/testfile1");
        RepositoryInfo ri = session.getRepositoryInfo();
        String uri = ri.getThinClientUri() + ri.getId() + "/";
        uri += isAtomPub ? "content?id=" : "root?objectId=";
        uri += file.getId();
        return uri;
    }

    @Test
    public void testContentStreamRedirect() throws Exception {
        assumeTrue(isAtomPub || isBrowser);

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        httpClientBuilder.setRedirectStrategy(NeverRedirectStrategy.INSTANCE); // to check Location header manually
        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            String uri = getURI() + "&testredirect=true"; // to provoke a redirect in our dummy blob provider
            HttpGet request = new HttpGet(uri);
            request.setHeader("Authorization", BASIC_AUTH);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatusLine().getStatusCode());
                Header locationHeader = response.getFirstHeader("Location");
                assertNotNull(locationHeader);
                assertEquals("http://example.com/dummyedirect", locationHeader.getValue());
            }
        }
    }

    @Test
    public void testContentStreamUsingGetMethod() throws Exception {
        doTestContentStream(new HttpGet(getURI()));
    }

    @Test
    public void testContentStreamUsingHeadMethod() throws Exception {
        doTestContentStream(new HttpHead(getURI()));
    }

    private void doTestContentStream(HttpUriRequest request) throws Exception {
        assumeTrue(isAtomPub || isBrowser);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            request.setHeader("Authorization", BASIC_AUTH);
            harness.deployContrib("org.nuxeo.ecm.core.opencmis.tests.tests", "OSGI-INF/download-listener-contrib.xml");
            DownloadListener.clearMessages();
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Header lengthHeader = response.getFirstHeader("Content-Length");
                assertNotNull(lengthHeader);
                byte[] expectedBytes = Helper.FILE1_CONTENT.getBytes("UTF-8");
                int expectedLength = expectedBytes.length;
                assertEquals(String.valueOf(expectedLength), lengthHeader.getValue());
                List<String> downloadMessages = DownloadListener.getMessages();
                if (request instanceof HttpGet) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try (InputStream in = response.getEntity().getContent()) {
                        IOUtils.copy(in, out);
                    }
                    assertEquals(expectedLength, out.size());
                    assertTrue(Arrays.equals(expectedBytes, out.toByteArray()));
                    assertEquals(Arrays.asList("download:comment=testfile.txt,downloadReason=cmis"), downloadMessages);
                } else { // request instanceof HttpHead
                    assertNull(response.getEntity());
                    assertEquals(0, downloadMessages.size());
                }
            }
        } finally {
            harness.undeployContrib("org.nuxeo.ecm.core.opencmis.tests.tests", "OSGI-INF/download-listener-contrib.xml");
        }
    }

}
