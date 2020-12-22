/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Martins <tmartins@nuxeo.com>
 */

package org.nuxeo.ecm.platform.preview.tests.adapter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.convert")
@Deploy("org.nuxeo.ecm.platform.preview")
@Deploy("org.nuxeo.ecm.platform.htmlsanitizer")
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.preview:doctype-contrib-test.xml")
public class TestPreviewAdapter {

    @Inject
    CoreSession session;

    @Inject
    protected ConversionService conversionService;

    @Inject
    protected MimetypeRegistry mimetypeRegistry;

    protected static final String BAD = "<html>\n" //
            + "<body>\n" //
            + "<script>alert(1)</script>\n" //
            + "<b>test</b>" //
            + "</body>\n" //
            + "</html>";

    @Test
    public void testNoBlob() throws Exception {
        DocumentModel document = session.createDocumentModel("File");
        HtmlPreviewAdapter adapter = document.getAdapter(HtmlPreviewAdapter.class);
        assertEquals(false, adapter.hasBlobToPreview());
        assertFalse(PreviewHelper.blobSupportsPreview(document, "file:content"));
        List<Blob> blobs = adapter.getFilePreviewBlobs();
        assertEquals(Collections.emptyList(), blobs);
    }

    @Test
    public void testFileDocument() throws Exception {
        Blob blob = new StringBlob("test", "text/plain");
        doTestFileDocument(blob, "<pre>test</pre>");
    }

    @Test
    public void testHTMLDocument() throws Exception {
        Blob blob = new StringBlob(BAD, "text/html");
        doTestFileDocument(blob, "\n\n<b>test</b>");
    }

    @Test
    public void testXMLDocument() throws Exception {
        Blob blob = new StringBlob(BAD, "text/xml");
        doTestFileDocument(blob, "\n\n<b>test</b>");
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.preview:test-zip-preview-contrib.xml")
    public void testZIPDocument() throws Exception {
        File file = Framework.createTempFile("testZIPDocument", ".zip");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
            writeZipEntry(out, "payload.html");
            writeZipEntry(out, "payload.xml");
            writeZipEntry(out, "payload.txt");
        }
        Blob blob = Blobs.createBlob(file, "application/zip", null, "testZIPDocument.zip");
        List<Blob> blobs = doGetPreviewBlobs(blob);
        assertEquals(4, blobs.size());
        String index = blobs.get(0).getString();
        assertEquals("<!doctype html><html><body><h1>testZIPDocument.zip</h1><ul>"
                + "<li><a href=\"payload.html\">payload.html</a></li><li><a href=\"payload.xml\">payload.xml</a></li>"
                + "<li><a href=\"payload.txt\">payload.txt</a></li></ul></body></html>", index);
        String payloadHtml = blobs.get(1).getString();
        assertEquals("<!doctype html><html><body>\n\n<b>test</b></body></html>", payloadHtml);
        String payloadXml = blobs.get(2).getString();
        assertEquals("<!doctype html><html><body>\n\n<b>test</b></body></html>", payloadXml);
        String payloadTxt = blobs.get(3).getString();
        assertEquals("<!doctype html><html><body>\n\n<b>test</b></body></html>", payloadTxt);
    }

    protected void writeZipEntry(ZipOutputStream out, String filename) throws IOException {
        ZipEntry e = new ZipEntry(filename);
        out.putNextEntry(e);

        byte[] data = BAD.getBytes(UTF_8);
        out.write(data, 0, data.length);
        out.closeEntry();
    }

    protected void doTestFileDocument(Blob blob, String expectedBody) throws Exception {
        List<Blob> blobs = doGetPreviewBlobs(blob);
        assertEquals(1, blobs.size());
        String preview = blobs.get(0).getString();
        assertEquals("<!doctype html><html><body>" + expectedBody + "</body></html>", preview);
    }

    protected List<Blob> doGetPreviewBlobs(Blob blob) {
        DocumentModel document = session.createDocumentModel("File");
        HtmlPreviewAdapter adapter = document.getAdapter(HtmlPreviewAdapter.class);
        Map<String, Serializable> file = new HashMap<>();
        // Attach one file to the list
        file.put("file", (Serializable) blob);
        document.setPropertyValue("file:content", (Serializable) blob);
        assertEquals(true, adapter.hasBlobToPreview());
        assertTrue(PreviewHelper.blobSupportsPreview(document, "file:content"));
        return adapter.getFilePreviewBlobs();
    }

    @Test
    public void testCustomDocumentWithFilesSchema() throws Exception {
        DocumentModel document = session.createDocumentModel("CustomDoc");

        // no preview adapter should be found in this case (empty files property)
        assertEquals(null, document.getAdapter(HtmlPreviewAdapter.class));
        assertFalse(PreviewHelper.blobSupportsPreview(document, "files:files/0/file"));

        Blob blob = new StringBlob("test");
        Map<String, Serializable> file = new HashMap<>();
        // Attach one file to the list
        file.put("file", (Serializable) blob);
        ArrayList<Map<String, Serializable>> files = new ArrayList<>();
        files.add(file);
        document.setPropertyValue("files:files", files);
        HtmlPreviewAdapter adapter = document.getAdapter(HtmlPreviewAdapter.class);
        assertEquals(true, adapter.hasBlobToPreview());
        assertTrue(PreviewHelper.blobSupportsPreview(document, "files:files/0/file"));
        List<Blob> blobs = adapter.getFilePreviewBlobs();
        assertEquals(1, blobs.size());
        String preview = blobs.get(0).getString();
        assertTrue(preview, preview.contains("<pre>test</pre>"));
    }

    @Test
    public void testFolderishDocument() throws Exception {
        DocumentModel document = session.createDocumentModel("File");
        document.addFacet("Folderish");
        HtmlPreviewAdapter adapter = document.getAdapter(HtmlPreviewAdapter.class);
        assertNotNull(adapter);
    }

    @Test
    public void testConvertTextToHtml() throws IOException {
        Blob blob = new StringBlob("thisismytest", "text/plain");
        // convert using SimpleBlobHolder (like ConvertAdapter)
        BlobHolder blobHolder = new SimpleBlobHolder(blob);
        BlobHolder res = conversionService.convertToMimeType("text/html", blobHolder , null);
        List<Blob> blobs = res.getBlobs();
        assertEquals(1, blobs.size());
        String preview = blobs.get(0).getString();
        assertTrue(preview, preview.contains("thisismytest"));
    }

}
