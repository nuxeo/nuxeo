/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.core.opencmis.tests.Helper.FILE1_CONTENT;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.impl.Base64;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamHashImpl;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManagerComponent;
import org.nuxeo.ecm.core.blob.BlobProviderDescriptor;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoPropertyData;
import org.nuxeo.ecm.core.opencmis.tests.Helper;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Suite of CMIS tests with minimal setup, checking HTTP headers.
 */
@RunWith(FeaturesRunner.class)
@Features(CmisFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/download-listener-contrib.xml")
public class CmisSuiteSession2 {

    protected static final String USERNAME = "Administrator";

    protected static final String PASSWORD = "test";

    protected static final String BASIC_AUTH = "Basic " + Base64.encodeBytes((USERNAME + ":" + PASSWORD).getBytes());

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

        DocumentModel file7 = coreSession.createDocumentModel("/testfolder1", "testfile7", "File");
        file7.setPropertyValue("dc:title", "title7");
        String content = FILE1_CONTENT;
        String filename = "testfile.txt";
        Blob blob7 = Blobs.createBlob(content);
        blob7.setDigest(DigestUtils.md5Hex(content));
        blob7.setFilename(filename);
        file7.setPropertyValue("content", (Serializable) blob7);
        file7 = Helper.createDocument(coreSession, file7);
        Helper.sleepForAuditGranularity();
        file7.putContextData("disableDublinCoreListener", Boolean.TRUE);
        DocumentRef file7verref = file7.checkIn(VersioningOption.MINOR, null);

        txFeature.nextTransaction();
        coreFeature.getStorageConfiguration().sleepForFulltext();
    }

    protected String getURI(String path) {
        CmisObject file = session.getObjectByPath(path);
        RepositoryInfo ri = session.getRepositoryInfo();
        String uri = ri.getThinClientUri() + ri.getId() + "/";
        uri += isAtomPub ? "content?id=" : "root?objectId=";
        uri += file.getId();
        return uri;
    }

    protected HttpEntity getCreateDocumentHttpEntity(File file) {
        FormBodyPart cmisactionPart = FormBodyPartBuilder.create("cmisaction",
                new StringBody("createDocument", ContentType.TEXT_PLAIN)).build();
        FormBodyPart contentPart = FormBodyPartBuilder.create("content",
                new FileBody(file, ContentType.TEXT_PLAIN, "testfile.txt")).build();
        HttpEntity entity = MultipartEntityBuilder.create()
                .addPart(cmisactionPart)
                .addTextBody("propertyId[0]", "cmis:name")
                .addTextBody("propertyValue[0]", "testfile01")
                .addTextBody("propertyId[1]", "cmis:objectTypeId")
                .addTextBody("propertyValue[1]", "File")
                .addPart(contentPart).build();
        return entity;
    }

    protected HttpEntity getCheckInHttpEntity(File file) {
        FormBodyPart cmisactionPart = FormBodyPartBuilder.create("cmisaction",
                new StringBody("checkIn", ContentType.TEXT_PLAIN)).build();
        FormBodyPart contentPart = FormBodyPartBuilder.create("content",
                new FileBody(file, ContentType.TEXT_PLAIN, "testfile.txt")).build();
        HttpEntity entity = MultipartEntityBuilder.create().addPart(cmisactionPart).addPart(contentPart).build();
        return entity;
    }

    protected HttpEntity getSetContentStreamHttpEntity(File file, String changeToken) {
        FormBodyPart cmisactionPart = FormBodyPartBuilder.create("cmisaction",
                new StringBody("setContent", ContentType.TEXT_PLAIN)).build();
        FormBodyPart contentPart = FormBodyPartBuilder.create("content",
                new FileBody(file, ContentType.TEXT_PLAIN, "testfile.txt")).build();
        HttpEntity entity = MultipartEntityBuilder.create()
                .addPart(cmisactionPart)
                .addTextBody("changeToken", changeToken)
                .addPart(contentPart).build();
        return entity;
    }

    @Test
    public void testCreateDocumentWithContentStreamAndDigestHeader() throws Exception {
        setUpData();
        session.clear(); // clear cache

        assumeTrue(isBrowser);

        String content = FILE1_CONTENT;
        String contentMD5Hex = DigestUtils.md5Hex(content);
        String contentMD5Base64 = NuxeoPropertyData.transcodeHexToBase64(contentMD5Hex);

        File[] files = createFiles(content);

        ObjectMapper mapper = new ObjectMapper();
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            String uri = getURI("/testfolder1") + "&succinct=true";
            HttpPost request = new HttpPost(uri);
            request.setHeader("Authorization", BASIC_AUTH);

            for (int i = 0; i < 2; i++) {
                boolean okRequest = i == 0;

                request.setHeader("Digest", "md5=" + (String) (okRequest ? contentMD5Base64 : "bogusMD5Sum"));
                HttpEntity reqEntity = getCreateDocumentHttpEntity(files[i]);
                request.setEntity(reqEntity);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    if (okRequest) {
                        JsonNode root = checkOkContentStreamResponse(contentMD5Hex, mapper, response);
                        String objectId = root.path("succinctProperties").path("cmis:objectId").textValue();
                        assertNotNull(objectId);
                        coreSession.removeDocument(new IdRef(objectId));
                        coreSession.save();
                    } else {
                        checkBadContentStreamResponse(mapper, response);
                    }
                }
            }
        }
        deleteFiles(files);
    }

    @Test
    public void testCheckInWithDigestHeader() throws Exception {
        setUpData();
        session.clear(); // clear cache

        assumeTrue(isBrowser);

        String content = FILE1_CONTENT + " Updated";
        String contentMD5Hex = DigestUtils.md5Hex(content);
        String contentMD5Base64 = NuxeoPropertyData.transcodeHexToBase64(contentMD5Hex);

        File[] files = createFiles(content);

        ObjectMapper mapper = new ObjectMapper();
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            String uri = getURI("/testfolder1/testfile7") + "&succinct=true&filter=cmis:contentStreamHash";
            HttpPost request = new HttpPost(uri);
            request.setHeader("Authorization", BASIC_AUTH);
            for (int i = 0; i < 2; i++) {
                boolean okRequest = i == 0;

                List<NameValuePair> paramList = Arrays.asList(new BasicNameValuePair("cmisaction", "checkOut"));
                HttpEntity reqEntity = new UrlEncodedFormEntity(paramList);
                request.setEntity(reqEntity);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    assertEquals(HttpServletResponse.SC_CREATED, response.getStatusLine().getStatusCode());
                    InputStream is = response.getEntity().getContent();
                    JsonNode root = mapper.readTree(is);
                    String objectId = root.path("succinctProperties").path("cmis:objectId").textValue();
                    assertNotNull(objectId);
                }

                request.setHeader("Digest", "md5=" + (String) (okRequest ? contentMD5Base64 : "bogusMD5Sum"));
                reqEntity = getCheckInHttpEntity(files[i]);
                request.setEntity(reqEntity);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    if (okRequest) {
                        checkOkContentStreamResponse(contentMD5Hex, mapper, response);
                    } else {
                        checkBadContentStreamResponse(mapper, response);
                    }
                }
            }
        }
        deleteFiles(files);
    }

    @Test
    public void testSetContentStreamWithDigestHeader() throws Exception {
        setUpData();
        session.clear(); // clear cache

        assumeTrue(isBrowser);

        String content = FILE1_CONTENT + " Updated";
        String contentMD5Hex = DigestUtils.md5Hex(content);
        String contentMD5Base64 = NuxeoPropertyData.transcodeHexToBase64(contentMD5Hex);

        File[] files = createFiles(content);

        ObjectMapper mapper = new ObjectMapper();
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            String uri = getURI("/testfolder1/testfile1") + "&succinct=true&filter=cmis:contentStreamHash";
            HttpPost request = new HttpPost(uri);
            request.setHeader("Authorization", BASIC_AUTH);

            for (int i = 0; i < 2; i++) {
                boolean okRequest = i == 0;

                request.setHeader("Digest", "md5=" + (String) (okRequest ? contentMD5Base64 : "bogusMD5Sum"));
                session.clear();
                String changeToken = session.getObjectByPath("/testfolder1/testfile1").getChangeToken();
                HttpEntity reqEntity = getSetContentStreamHttpEntity(files[i], changeToken);
                request.setEntity(reqEntity);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    if (okRequest) {
                        checkOkContentStreamResponse(contentMD5Hex, mapper, response);
                    } else {
                        checkBadContentStreamResponse(mapper, response);
                    }
                }
            }
        }
        deleteFiles(files);
    }

    protected JsonNode checkOkContentStreamResponse(String contentMD5Hex, ObjectMapper mapper,
            CloseableHttpResponse response) throws IOException {
        String content;
        try (InputStream is = response.getEntity().getContent()) {
            content = IOUtils.toString(is, StandardCharsets.UTF_8);
        }
        assertEquals(content, HttpServletResponse.SC_CREATED, response.getStatusLine().getStatusCode());
        JsonNode root = mapper.readTree(content);
        String expectedContentStreamHash = new ContentStreamHashImpl(
                ContentStreamHashImpl.ALGORITHM_MD5, contentMD5Hex).toString();
        Iterator iter = root.path("succinctProperties").path("cmis:contentStreamHash").elements();
        boolean found = false;
        while (iter.hasNext()) {
            String hash = ((JsonNode) iter.next()).textValue();
            if (expectedContentStreamHash.equals(hash)) {
                found = true;
                break;
            }
        }
        assertTrue("cmis:contentStreamHash does not contain " + expectedContentStreamHash, found);
        return root;
    }

    protected JsonNode checkBadContentStreamResponse(ObjectMapper mapper, CloseableHttpResponse response)
            throws IOException {
        String content;
        try (InputStream is = response.getEntity().getContent()) {
            content = IOUtils.toString(is, StandardCharsets.UTF_8);
        }
        assertEquals(content, HttpServletResponse.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
        JsonNode root = mapper.readTree(content);
        String exception = root.path("exception").textValue();
        assertEquals("invalidArgument", exception);
        return root;
    }

    protected File[] createFiles(String content) throws IOException {
        File[] files = new File[2];
        for (int i = 0; i < 2; i++) {
            File file = files[i] = Framework.createTempFile("NuxeoCMIS-", null);
            try (Writer writer = new FileWriter(file); Reader reader = new StringReader(content)) {
                IOUtils.copy(reader, writer);
            }
        }
        return files;
    }

    protected void deleteFiles(File[] files) throws IOException {
        for (File file : files) {
            file.delete();
        }
    }

    @Test
    public void testContentStreamRedirect() throws Exception {
        useDummyCmisBlobProvider();
        setUpData();
        session.clear(); // clear cache

        assumeTrue(isAtomPub || isBrowser);

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        httpClientBuilder.setRedirectStrategy(NeverRedirectStrategy.INSTANCE); // to check Location header manually
        try (CloseableHttpClient httpClient = httpClientBuilder.build()) {
            String uri = getURI("/testfolder1/testfile1") + "&testredirect=true"; // to provoke a redirect in our dummy blob provider
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
        setUpData();
        session.clear(); // clear cache

        doTestContentStream(new HttpGet(getURI("/testfolder1/testfile1")));
    }

    @Test
    public void testContentStreamUsingHeadMethod() throws Exception {
        setUpData();
        session.clear(); // clear cache

        doTestContentStream(new HttpHead(getURI("/testfolder1/testfile1")));
    }

    private void doTestContentStream(HttpUriRequest request) throws Exception {
        assumeTrue(isAtomPub || isBrowser);

        String contentMD5Hex = DigestUtils.md5Hex(FILE1_CONTENT);
        String contentMD5Base64 = NuxeoPropertyData.transcodeHexToBase64(contentMD5Hex);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            request.setHeader("Authorization", BASIC_AUTH);
            boolean isHeadRequest = request instanceof HttpHead;
            request.setHeader("Want-Digest", isHeadRequest ? "contentMD5" : "md5");
            DownloadListener.clearMessages();
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatusLine().getStatusCode());
                Header lengthHeader = response.getFirstHeader("Content-Length");
                assertNotNull(lengthHeader);
                byte[] expectedBytes = FILE1_CONTENT.getBytes("UTF-8");
                int expectedLength = expectedBytes.length;
                assertEquals(String.valueOf(expectedLength), lengthHeader.getValue());
                List<String> downloadMessages = DownloadListener.getMessages();
                if (isHeadRequest) {
                    Header contentMD5Header = response.getFirstHeader("Content-MD5");
                    assertEquals(contentMD5Base64, contentMD5Header.getValue());
                    assertNull(response.getEntity());
                    assertEquals(0, downloadMessages.size());
                } else {
                    Header digestHeader = response.getFirstHeader("Digest");
                    assertEquals("MD5=" + contentMD5Base64, digestHeader.getValue());
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try (InputStream in = response.getEntity().getContent()) {
                        IOUtils.copy(in, out);
                    }
                    assertEquals(expectedLength, out.size());
                    assertTrue(Arrays.equals(expectedBytes, out.toByteArray()));
                    assertEquals(Arrays.asList("download:comment=testfile.txt,downloadReason=cmis"), downloadMessages);
                }
            }
        }
    }

}
