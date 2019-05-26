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
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestCreateDocumentRestlet extends AbstractRestletTest {

    protected static final String ENDPOINT = "/createDocument";

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
    public void testUpload() throws Exception {
        String path = "/" + repositoryName + "/" + folder.getId() + ENDPOINT;
        String uri = getUri(path);
        URIBuilder uriBuilder = new URIBuilder(uri);
        uriBuilder.addParameter("docType", "Note");
        uriBuilder.addParameter("dublincore:title", "mytitle");
        uriBuilder.addParameter("dc:description", "mydescr");
        HttpPost request = new HttpPost(uriBuilder.build());
        setAuthorization(request);
        EntityBuilder entityBuilder = EntityBuilder.create();
        entityBuilder.setContentType(ContentType.IMAGE_PNG);
        entityBuilder.setBinary("bin1".getBytes());
        request.setEntity(entityBuilder.build());
        String content;
        try (CloseableHttpClient httpClient = httpClientBuilder.build();
                CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(SC_OK, response.getStatusLine().getStatusCode());
            assertEquals("application/xml;charset=UTF-8", response.getFirstHeader("Content-Type").getValue());
            try (InputStream is = response.getEntity().getContent()) {
                content = IOUtils.toString(is, UTF_8);
            }
        }

        // check doc has been created
        txFeature.nextTransaction();
        DocumentRef docRef = new PathRef("/folder/mytitle");
        assertTrue(session.exists(docRef));
        DocumentModel doc = session.getDocument(docRef);
        assertEquals("Note", doc.getType());
        assertEquals("mytitle", doc.getTitle());
        assertEquals("mydescr", doc.getPropertyValue("dc:description"));

        // check body returned
        String expectedContent = XML //
                + "<document>" //
                + "<repository>" + repositoryName + "</repository>" //
                + "<docRef>" + doc.getId() + "</docRef>" //
                + "<docTitle>mytitle</docTitle>" //
                + "<docPath>/folder/mytitle</docPath>" //
                + "</document>";
        assertEquals(expectedContent, content);
    }

}
