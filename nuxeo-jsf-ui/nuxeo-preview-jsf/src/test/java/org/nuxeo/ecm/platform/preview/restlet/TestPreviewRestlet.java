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
package org.nuxeo.ecm.platform.preview.restlet;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;

import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.ui.web.restAPI.AbstractRestletTest;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.platform.preview")
@Deploy("org.nuxeo.ecm.platform.preview.jsf")
public class TestPreviewRestlet extends AbstractRestletTest {

    protected static final String ENDPOINT = "/preview";

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    protected String repositoryName;

    protected DocumentModel doc;

    @Before
    public void before() throws Exception {
        repositoryName = session.getRepositoryName();
        doc = session.createDocumentModel("/", "doc", "Note");
        doc.setPropertyValue("note:note", "Hello <b>World!</b>");
        doc.setPropertyValue("note:mime_type", "text/html");
        doc = session.createDocument(doc);
        session.save();
        txFeature.nextTransaction();
    }

    @Test
    public void testPreview() throws Exception {
        String path = ENDPOINT + "/" + repositoryName + "/" + doc.getId() + "/default/";
        String content = executeRequest(path, HttpGet::new, SC_OK, "text/html;charset=UTF-8");
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"/>" //
                + "<html>" //
                + "<head>" //
                + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>" //
                + "</head>" //
                + "<body>" //
                + "Hello <b>World!</b>" //
                + "</body>" //
                + "</html>";
        assertEquals(expected, content);
    }

    /**
     * Dummy converter to return several blobs, with different names.
     *
     * @since 10.3
     */
    public static class DummyWordConverter implements Converter {

        @Override
        public void init(ConverterDescriptor descriptor) {
        }

        @Override
        public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters)
                throws ConversionException {
            Blob blob1 = Blobs.createBlob("<html>The Index</html>", "text/html", "UTF-8", "index.html");
            Blob blob2 = Blobs.createBlob("<html>Some Page</html>", "text/html", "UTF-8", "somepage.html");
            return new SimpleBlobHolder(Arrays.asList(blob1, blob2));
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.preview.jsf.tests:OSGI-INF/test-converter.xml")
    public void testPreviewSubpath() throws Exception {
        doc.setPropertyValue("note:mime_type", "application/x-dummy-word");
        doc = session.createDocument(doc);
        session.save();
        txFeature.nextTransaction();

        String path = ENDPOINT + "/" + repositoryName + "/" + doc.getId() + "/default/index.html";
        String content = executeRequest(path, HttpGet::new, SC_OK, "text/html;charset=UTF-8");
        String expected = "<html>The Index</html>";
        assertEquals(expected, content);

        path = ENDPOINT + "/" + repositoryName + "/" + doc.getId() + "/default/somepage.html";
        content = executeRequest(path, HttpGet::new, SC_OK, "text/html;charset=UTF-8");
        expected = "<html>Some Page</html>";
        assertEquals(expected, content);
    }

}
