/*
 * (C) Copyright 2006-2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.mimetype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry.DEFAULT_MIMETYPE;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.impl.blob.URLBlob;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * Allows the tests of {@link MimetypeRegistryService} behaviour.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.mimetype")
public class TestMimetypeRegistryService {

    @Inject
    private MimetypeRegistry mimetypeRegistry;

    @Inject
    protected HotDeployer hotDeployer;

    protected static final String SAMPLE_MIMETYPE_NORMALIZED = "application/msword";

    protected static MimetypeEntryImpl getMimetypeDefault() {
        return new MimetypeEntryImpl(SAMPLE_MIMETYPE_NORMALIZED, //
                List.of(SAMPLE_MIMETYPE_NORMALIZED), //
                List.of("doc", "dot"), //
                "word.png", //
                true, //
                true, //
                true);
    }

    protected static MimetypeEntryImpl getMimetypeSample() {
        return new MimetypeEntryImpl(SAMPLE_MIMETYPE_NORMALIZED, //
                List.of(SAMPLE_MIMETYPE_NORMALIZED, "app/whatever-word"), //
                List.of("doc", "xml"), //
                "icons/doc.png", //
                true, //
                true, //
                true);
    }

    protected static MimetypeEntryImpl getMimetypeSampleMerged() {
        return new MimetypeEntryImpl(SAMPLE_MIMETYPE_NORMALIZED, //
                List.of(SAMPLE_MIMETYPE_NORMALIZED, "app/whatever-word"), //
                List.of("doc", "dot", "xml"), //
                "icons/doc.png", //
                true, //
                true, //
                true);
    }

    protected void check(MimetypeEntry expected, MimetypeEntry actual) {
        assertNotNull(expected);
        assertNotNull(actual);
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected.getExtensions(), actual.getExtensions());
        assertEquals(expected.getIconPath(), actual.getIconPath());
        assertEquals(expected.getMajor(), actual.getMajor());
        assertEquals(expected.getMimetypes(), actual.getMimetypes());
        assertEquals(expected.getMinor(), actual.getMinor());
        assertEquals(expected.getNormalized(), actual.getNormalized());
        assertEquals(expected.isBinary(), actual.isBinary());
        assertEquals(expected.isOleSupported(), actual.isOleSupported());
        assertEquals(expected.isOnlineEditable(), actual.isOnlineEditable());
    }

    @Test
    public void testMimetypeRegistration() throws Exception {
        check(getMimetypeDefault(), mimetypeRegistry.getMimetypeEntryByName(SAMPLE_MIMETYPE_NORMALIZED));

        hotDeployer.deploy("org.nuxeo.ecm.core.mimetype:test-mimetype-contrib.xml");

        // check override
        check(getMimetypeSampleMerged(), mimetypeRegistry.getMimetypeEntryByName(SAMPLE_MIMETYPE_NORMALIZED));

        // Second registration (?)
        hotDeployer.deploy("org.nuxeo.ecm.core.mimetype:test-mimetype-contrib.xml");
        check(getMimetypeSampleMerged(), mimetypeRegistry.getMimetypeEntryByName(SAMPLE_MIMETYPE_NORMALIZED));

        // check hot undeploy
        hotDeployer.undeploy("org.nuxeo.ecm.core.mimetype:test-mimetype-contrib.xml");
        check(getMimetypeDefault(), mimetypeRegistry.getMimetypeEntryByName(SAMPLE_MIMETYPE_NORMALIZED));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.mimetype:test-mimetype-contrib.xml")
    public void testGetExtensionsFromMimetype() {
        MimetypeEntry mimetype = getMimetypeSampleMerged();
        assertEquals(mimetypeRegistry.getExtensionsFromMimetypeName(mimetype.getNormalized()),
                mimetype.getExtensions());
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.mimetype:test-mimetype-contrib.xml")
    public void testGetMimetypeFromFile() throws Exception {
        File file = FileUtils.getResourceFileFromContext("test-data/hello.doc");

        String mimetype = mimetypeRegistry.getMimetypeFromFile(file);
        assertEquals(SAMPLE_MIMETYPE_NORMALIZED, mimetype);

        List<String> extensions = mimetypeRegistry.getExtensionsFromMimetypeName(mimetype);
        assertTrue(extensions.contains("doc"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.mimetype:test-mimetype-contrib.xml")
    public void testGetMimetypeFromBlob() throws Exception {
        String mimetype = mimetypeRegistry.getMimetypeFromBlob(getWordBlob());
        assertEquals(SAMPLE_MIMETYPE_NORMALIZED, mimetype);

        List<String> extensions = mimetypeRegistry.getExtensionsFromMimetypeName(mimetype);
        assertTrue(extensions.contains("doc"));
    }

    protected Blob getWordBlob() {
        return new URLBlob(getClass().getResource("/test-data/hello.doc"));
    }

    protected Blob getWordMLBlob() {
        return new URLBlob(getClass().getResource("/test-data/wordml-sample.xml"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.mimetype:test-mimetype-contrib.xml")
    @Deploy("org.nuxeo.ecm.core.mimetype:test-mimetype-contrib2.xml")
    @Deploy("org.nuxeo.ecm.core.mimetype:test-extension-contrib.xml")
    public void testGetMimetypeFromFilnameAndBlobWithDefault() {
        // doc filename + empty file gives word mimetype
        String mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("hello.doc", Blobs.createBlob(""),
                "default/mimetype");
        assertEquals(SAMPLE_MIMETYPE_NORMALIZED, mimetype);
        List<String> extensions = mimetypeRegistry.getExtensionsFromMimetypeName(mimetype);
        assertTrue(extensions.contains("doc"));

        // bad filename extension + word file gives word mimetype
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("bad_file_name.ext", getWordBlob(),
                "default/mimetype");
        assertEquals(SAMPLE_MIMETYPE_NORMALIZED, mimetype);

        // bad filename (without extension) + word file gives word mimetype
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("bad_file_name", getWordBlob(),
                "default/mimetype");
        assertEquals(SAMPLE_MIMETYPE_NORMALIZED, mimetype);

        // bad name and empty file: fallback to default mimetype
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("bad_file_name", Blobs.createBlob(""),
                "default/mimetype");
        assertEquals("default/mimetype", mimetype);

        // test ambiguous file extension with wordml sniffing
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("sample-wordml.xml", getWordMLBlob(),
                "default/mimetype");
        assertEquals(SAMPLE_MIMETYPE_NORMALIZED, mimetype);

        // test ambiguous file extension with empty file
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("sample-wordml.xml", Blobs.createBlob(""),
                "default/mimetype");
        assertEquals("default/mimetype", mimetype);

        // test temporary file, extension .tmp
        Blob blob = Blobs.createBlob("", "foo/bar");
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("myfile.TMP", blob, "default/mimetype");
        assertEquals(DEFAULT_MIMETYPE, mimetype);

        // test MS Office temporary file, starting with tilde-dollar (~$)
        blob = Blobs.createBlob("", "foo/bar");
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("~$sample-docx.docx", blob,
                "default/mimetype");
        assertEquals(DEFAULT_MIMETYPE, mimetype);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.mimetype:test-mimetype-contrib.xml")
    public void testGetMimetypeFromFilenameWithBlobMimetypeFallback() throws Exception {
        // bad filename + word mimetype : fallback to excel mimetype
        Blob blob = Blobs.createBlob("");
        blob.setMimeType(SAMPLE_MIMETYPE_NORMALIZED);
        String mimetype = mimetypeRegistry.getMimetypeFromFilenameWithBlobMimetypeFallback("bad_file_name", blob,
                "default/mimetype");
        assertEquals(SAMPLE_MIMETYPE_NORMALIZED, mimetype);

        // bad filename + bad mimetype : fallback to sniffing blob
        blob = getWordBlob();
        blob.setMimeType("bad/mimetype");
        mimetype = mimetypeRegistry.getMimetypeFromFilenameWithBlobMimetypeFallback("bad_file_name", blob,
                "default/mimetype");
        assertEquals(SAMPLE_MIMETYPE_NORMALIZED, mimetype);

        // bad filename + bad mimetype : fallback to default mimetype
        blob = Blobs.createBlob("");
        blob.setMimeType("bad/mimetype");
        mimetype = mimetypeRegistry.getMimetypeFromFilenameWithBlobMimetypeFallback("bad_file_name", blob,
                "default/mimetype");
        assertEquals("default/mimetype", mimetype);

        // test temporary file, extension .tmp
        blob = Blobs.createBlob("", "foo/bar");
        mimetype = mimetypeRegistry.getMimetypeFromFilenameWithBlobMimetypeFallback("myfile.tmp", blob,
                "default/mimetype");
        assertEquals(DEFAULT_MIMETYPE, mimetype);

        // test MS Office temporary file, starting with tilde-dollar (~$)
        blob = Blobs.createBlob("", "foo/bar");
        mimetype = mimetypeRegistry.getMimetypeFromFilenameWithBlobMimetypeFallback("~$sample-docx.docx", blob,
                "default/mimetype");
        assertEquals(DEFAULT_MIMETYPE, mimetype);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.mimetype:test-mimetype-contrib.xml")
    public void testGetMimetypeEntryByMimetype() {

        // Using normalized name.
        MimetypeEntry entry = mimetypeRegistry.getMimetypeEntryByMimeType(SAMPLE_MIMETYPE_NORMALIZED);
        assertNotNull(entry);
        assertEquals(SAMPLE_MIMETYPE_NORMALIZED, entry.getNormalized());

        // Using other mimetype
        entry = mimetypeRegistry.getMimetypeEntryByMimeType("app/whatever-word");
        assertNotNull(entry);
        assertEquals(SAMPLE_MIMETYPE_NORMALIZED, entry.getNormalized());
    }

    @Test
    public void iCanRetrieveNormalizedMimetype() {
        verifyNormalizedMimetype("application/zip", "application/zip");
        verifyNormalizedMimetype("application/zip", "application/x-zip-compressed");

        verifyNormalizedMimetype("text/plain", "text/plain");
        verifyNormalizedMimetype("text/x-rst", "text/x-rst");
        verifyNormalizedMimetype("text/x-rst", "text/restructured");

        verifyNormalizedMimetype("text/python-source", "text/python-source");
        verifyNormalizedMimetype("text/python-source", "text/x-python");

        verifyNormalizedMimetype("application/vnd.ms-excel", "application/vnd.ms-excel");

        verifyNormalizedMimetype("application/pdf", "application/pdf");

        verifyNormalizedMimetype("application/vnd.sun.xml.writer.template", "application/vnd.sun.xml.writer.template");

        verifyNormalizedMimetype("application/photoshop", "application/photoshop");
        verifyNormalizedMimetype("application/photoshop", "image/photoshop");
        verifyNormalizedMimetype("application/photoshop", "image/vnd.adobe.photoshop");
        verifyNormalizedMimetype("application/photoshop", "application/psd");
        verifyNormalizedMimetype("application/photoshop", "image/x-psd");

    }

    @Test
    public void iCannotRetrieveNormalizedMimeType() {
        assertFalse(mimetypeRegistry.getNormalizedMimeType("application/unExisting").isPresent());
        assertFalse(mimetypeRegistry.getNormalizedMimeType("application/photoshob").isPresent());
    }

    @Test
    public void iCanCheckIfMimeTypeIsNormalized() {
        assertTrue(mimetypeRegistry.isMimeTypeNormalized("application/zip"));
        assertTrue(mimetypeRegistry.isMimeTypeNormalized("application/photoshop"));
        assertTrue(mimetypeRegistry.isMimeTypeNormalized("text/plain"));
        assertFalse(mimetypeRegistry.isMimeTypeNormalized("application/x-photoshop"));
        assertFalse(mimetypeRegistry.isMimeTypeNormalized("image/photoshop"));
    }

    protected void verifyNormalizedMimetype(String expectedNormalizedMimetype, String mimetype) {
        mimetypeRegistry.getNormalizedMimeType(mimetype)
                        .ifPresentOrElse(r -> assertEquals(expectedNormalizedMimetype, r),
                                () -> fail(String.format("'%s' should have a normalized mime type", mimetype)));
    }
}
