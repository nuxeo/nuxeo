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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.restAPI;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.filemanager.api")
@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("org.nuxeo.ecm.platform.types.core")
public class TestUploadRestlet extends AbstractRestletTest {

    protected static final String ENDPOINT = "/upload";

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    protected String repositoryName;

    protected DocumentModel folder;

    @Before
    public void before() {
        repositoryName = session.getRepositoryName();
        folder = session.createDocumentModel("/", "folder", "Folder");
        folder = session.createDocument(folder);
        session.save();
        txFeature.nextTransaction();
    }

    @Test
    public void testUploadSimple() throws Exception {
        String path = "/" + repositoryName + "/" + folder.getId() + "/img.png" + ENDPOINT;
        String uri = getUri(path);
        HttpPost request = new HttpPost(uri);
        setAuthorization(request);
        EntityBuilder builder = EntityBuilder.create();
        builder.setContentType(ContentType.IMAGE_PNG);
        builder.setBinary("bin1".getBytes());
        request.setEntity(builder.build());
        try (CloseableHttpClient httpClient = httpClientBuilder.build();
                CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(SC_OK, response.getStatusLine().getStatusCode());
            assertEquals("application/xml;charset=UTF-8", response.getFirstHeader("Content-Type").getValue());
            try (InputStream is = response.getEntity().getContent()) {
                String content = IOUtils.toString(is, UTF_8);
                String expected = XML //
                        + "<upload>img.png</upload>";
                assertEquals(expected, content);
            }
        }

        // check file has been created
        txFeature.nextTransaction();
        checkFileAtPath("/folder/img.png", "bin1", "image/png");
    }

    @Test
    public void testUploadMultipart() throws Exception {
        String path = "/" + repositoryName + "/" + folder.getId() + "/filenamenotused" + ENDPOINT;
        String uri = getUri(path);
        HttpPost request = new HttpPost(uri);
        setAuthorization(request);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("image1", "bin1".getBytes(), ContentType.IMAGE_PNG, "img.png");
        builder.addBinaryBody("image2", "bin2".getBytes(), ContentType.IMAGE_GIF, "img.gif");
        request.setEntity(builder.build());
        try (CloseableHttpClient httpClient = httpClientBuilder.build();
                CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(SC_OK, response.getStatusLine().getStatusCode());
            assertEquals("application/xml;charset=UTF-8", response.getFirstHeader("Content-Type").getValue());
            try (InputStream is = response.getEntity().getContent()) {
                String content = IOUtils.toString(is, UTF_8);
                String expected = XML //
                        + "<uploads>" //
                        + "<upload>img.png</upload>" //
                        + "<upload>img.gif</upload>" //
                        + "</uploads>";
                assertEquals(expected, content);
            }
        }

        // check files have been created
        txFeature.nextTransaction();
        checkFileAtPath("/folder/img.png", "bin1", "image/png");
        checkFileAtPath("/folder/img.gif", "bin2", "image/gif");
    }

    protected void checkFileAtPath(String path, String content, String contentType) throws IOException {
        DocumentRef docRef = new PathRef(path);
        assertTrue(session.exists(docRef));
        DocumentModel doc = session.getDocument(docRef);
        assertEquals("File", doc.getType());
        Blob blob = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        assertEquals(content, blob.getString());
        assertEquals(contentType, blob.getMimeType());
    }
}
