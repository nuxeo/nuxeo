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

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;

import javax.inject.Inject;

import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestDownloadFileRestlet extends AbstractRestletTest {

    protected static final String ENDPOINT = "/downloadFile";

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    protected String repositoryName;

    protected DocumentModel doc;

    @Before
    public void before() {
        repositoryName = session.getRepositoryName();
        doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:title", "titleimage.png");
        Blob blob = Blobs.createBlob("somebincontent", "image/png", null, "myimage.png");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);
        session.save();
        txFeature.nextTransaction();
    }

    @Test
    public void testDownload() throws Exception {
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT;
        String content = executeRequest(path, HttpGet::new, SC_OK, "image/png",
                "attachment; filename*=UTF-8''myimage.png");
        assertEquals("somebincontent", content);
    }

    @Test
    public void testDownloadExplicitProperties() throws Exception {
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT //
                + "?blobPropertyName=file:content&filenamePropertyName=dc:title";
        String content = executeRequest(path, HttpGet::new, SC_OK, "image/png",
                "attachment; filename*=UTF-8''titleimage.png");
        assertEquals("somebincontent", content);
    }

    @Test
    public void testDownloadExplicitSchema() throws Exception {
        String path = "/" + repositoryName + "/" + doc.getId() + ENDPOINT //
                + "?schema=file&blobField=content";
        String content = executeRequest(path, HttpGet::new, SC_OK, "image/png",
                "attachment; filename*=UTF-8''myimage.png");
        assertEquals("somebincontent", content);
    }

}
