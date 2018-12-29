/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *
 */

package org.nuxeo.wopi;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.wopi.Constants.WOPI_SERVLET_PATH;
import static org.nuxeo.wopi.TestConstants.FILE_CONTENT_PROPERTY;

import java.io.IOException;
import java.io.Serializable;
import java.util.Base64;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features({ WOPIFeature.class, WOPIServletContainerFeature.class })
@Deploy("org.nuxeo.ecm.jwt")
@Deploy("org.nuxeo.wopi:OSGI-INF/test-jwt-contrib.xml")
@Deploy("org.nuxeo.wopi:OSGI-INF/test-authentication-contrib.xml")
@Deploy("org.nuxeo.wopi:OSGI-INF/test-servletcontainer-contrib.xml")
public class TestWOPIServlet {

    protected static final String BASIC_AUTH = "Basic "
            + Base64.getEncoder().encodeToString(("Administrator:Administrator").getBytes());

    @Inject
    protected CoreSession session;

    @Inject
    protected WOPIService wopiService;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected ServletContainerFeature servletContainerFeature;

    protected DocumentModel docxDoc;

    protected DocumentModel binDoc;

    @Before
    public void before() throws IOException {
        docxDoc = session.createDocumentModel("/", "docxDoc", "File");
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-file.txt"));
        blob.setFilename("foo.docx");
        docxDoc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) blob);
        docxDoc = session.createDocument(docxDoc);

        binDoc = session.createDocumentModel("/", "binDoc", "File");
        blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-file.txt"));
        blob.setFilename("foo.bin");
        binDoc.setPropertyValue(FILE_CONTENT_PROPERTY, (Serializable) blob);
        binDoc = session.createDocument(binDoc);

        // make sure everything is committed
        transactionalFeature.nextTransaction();
    }

    protected String getBaseURL() {
        int port = servletContainerFeature.getPort();
        return "http://localhost:" + port + "/";
    }

    @Test
    public void testDocumentNotFound() throws IOException {
        String url = getBaseURL() + "wopi/view/test/unknownid/" + FILE_CONTENT_PROPERTY;
        doGet(url, response -> assertEquals(404, response.getStatusLine().getStatusCode()));
    }

    @Test
    public void testBlobNotFound() throws IOException {
        String url = Helpers.getWOPIURL(getBaseURL(), "view", docxDoc, "files:files/0/file");
        doGet(url, response -> assertEquals(404, response.getStatusLine().getStatusCode()));
    }

    @Test
    public void testUnsupportedBlob() throws IOException {
        String url = Helpers.getWOPIURL(getBaseURL(), "view", binDoc, FILE_CONTENT_PROPERTY);
        doGet(url, response -> assertEquals(404, response.getStatusLine().getStatusCode()));
    }

    @Test
    public void testSupportedBlob() throws IOException {
        String url = Helpers.getWOPIURL(getBaseURL(), "view", docxDoc, FILE_CONTENT_PROPERTY);
        doGet(url, response -> assertEquals(200, response.getStatusLine().getStatusCode()));
    }

    @Test
    public void testNullPath() throws IOException {
        String url = getBaseURL() + WOPI_SERVLET_PATH;
        doGet(url, response -> assertEquals(400, response.getStatusLine().getStatusCode()));
    }

    @Test
    public void testInvalidPath() throws IOException {
        // invalid path: no doc id, no xpath
        String url = getBaseURL() + WOPI_SERVLET_PATH + "/view/default/";
        doGet(url, response -> assertEquals(400, response.getStatusLine().getStatusCode()));
    }

    @Test
    public void testWOPINotEnabled() throws IOException {
        // force wopi to be disabled
        ((WOPIServiceImpl) wopiService).extensionActionURLs.clear();
        ((WOPIServiceImpl) wopiService).extensionAppNames.clear();

        // ask for a supported blob
        String url = Helpers.getWOPIURL(getBaseURL(), "view", docxDoc, FILE_CONTENT_PROPERTY);
        doGet(url, response -> assertEquals(404, response.getStatusLine().getStatusCode()));
    }

    protected void doGet(String url, Consumer<CloseableHttpResponse> consumer) throws IOException {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("Authorization", BASIC_AUTH);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                consumer.accept(response);
            }
        }
    }

}
