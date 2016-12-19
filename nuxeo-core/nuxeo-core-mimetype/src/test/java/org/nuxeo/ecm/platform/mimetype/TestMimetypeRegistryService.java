/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.impl.blob.URLBlob;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.mimetype.service.ExtensionDescriptor;
import org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/*
 * Test the Nuxeo component. No deployment here (true unit test).
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.mimetype")
public class TestMimetypeRegistryService {

    @Inject
    private MimetypeRegistry mimetypeRegistry;

    protected MimetypeRegistryService mimetypeRegistryService;

    private static MimetypeEntryImpl getMimetypeSample() {
        String normalizedMimetype = "application/msword";

        List<String> mimetypes = new ArrayList<String>();
        mimetypes.add("application/msword");
        // fake
        mimetypes.add("app/whatever-word");

        List<String> extensions = new ArrayList<String>();
        extensions.add("doc");
        extensions.add("xml");

        String iconPath = "icons/doc.png";

        boolean binary = true;
        boolean onlineEditable = true;
        boolean oleSupported = true;

        return new MimetypeEntryImpl(normalizedMimetype, mimetypes, extensions, iconPath, binary, onlineEditable,
                oleSupported);
    }

    @Before
    public void before() {
        mimetypeRegistryService = ((MimetypeRegistryService) mimetypeRegistry);
    }

    @Test
    public void testMimetypeRegistration() {
        MimetypeEntry mimetype = getMimetypeSample();
        mimetypeRegistryService.registerMimetype(mimetype);
        assertEquals(mimetypeRegistry.getMimetypeEntryByName(mimetype.getNormalized()), mimetype);

        // Second registration
        mimetypeRegistryService.registerMimetype(mimetype);
        assertEquals(mimetypeRegistry.getMimetypeEntryByName(mimetype.getNormalized()), mimetype);

        mimetypeRegistryService.unregisterMimetype(mimetype.getNormalized());
        assertNull(mimetypeRegistry.getMimetypeEntryByName(mimetype.getNormalized()));
    }

    @Test
    public void testGetExtensionsFromMimetype() {
        MimetypeEntry mimetype = getMimetypeSample();
        mimetypeRegistryService.registerMimetype(mimetype);

        assertEquals(mimetypeRegistry.getExtensionsFromMimetypeName(mimetype.getNormalized()),
                mimetype.getExtensions());

        mimetypeRegistryService.unregisterMimetype(mimetype.getNormalized());
    }

    @Test
    public void testGetMimetypeFromFile() throws Exception {
        MimetypeEntry mimetypeEntry = getMimetypeSample();
        mimetypeRegistryService.registerMimetype(mimetypeEntry);

        File file = FileUtils.getResourceFileFromContext("test-data/hello.doc");

        String mimetype = mimetypeRegistry.getMimetypeFromFile(file);
        assertEquals("application/msword", mimetype);

        List<String> extensions = mimetypeRegistry.getExtensionsFromMimetypeName(mimetype);
        assertTrue(extensions.contains("doc"));
    }

    @Test
    public void testGetMimetypeFromBlob() throws Exception {
        MimetypeEntry mimetypeEntry = getMimetypeSample();
        mimetypeRegistryService.registerMimetype(mimetypeEntry);
        String mimetype = mimetypeRegistry.getMimetypeFromBlob(getWordBlob());
        assertEquals("application/msword", mimetype);

        List<String> extensions = mimetypeRegistry.getExtensionsFromMimetypeName(mimetype);
        assertTrue(extensions.contains("doc"));
    }

    protected Blob getWordBlob() throws FileNotFoundException {
        return new URLBlob(getClass().getResource("/test-data/hello.doc"));
    }

    protected Blob getWordMLBlob() throws FileNotFoundException {
        return new URLBlob(getClass().getResource("/test-data/wordml-sample.xml"));
    }

    @Test
    public void testGetMimetypeFromFilnameAndBlobWithDefault() throws Exception {

        MimetypeEntry mimetypeEntry = getMimetypeSample();
        mimetypeRegistryService.registerMimetype(mimetypeEntry);

        MimetypeEntry docbook = new MimetypeEntryImpl("application/docbook+xml",
                Arrays.asList("application/docbook+xml"), Arrays.asList("xml", "doc.xml", "docb"), "", false, false,
                false);
        mimetypeRegistryService.registerMimetype(docbook);

        ExtensionDescriptor ed = new ExtensionDescriptor("xml");
        ed.setAmbiguous(true);
        ed.setMimetype("text/xml");
        mimetypeRegistryService.registerFileExtension(ed);

        // doc filename + empty file gives word mimetype
        String mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("hello.doc", Blobs.createBlob(""),
                "default/mimetype");
        assertEquals("application/msword", mimetype);
        List<String> extensions = mimetypeRegistry.getExtensionsFromMimetypeName(mimetype);
        assertTrue(extensions.contains("doc"));

        // bad filename extension + word file gives word mimetype
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("bad_file_name.ext", getWordBlob(),
                "default/mimetype");
        assertEquals("application/msword", mimetype);

        // bad filename (without extension) + word file gives word mimetype
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("bad_file_name", getWordBlob(),
                "default/mimetype");
        assertEquals("application/msword", mimetype);

        // bad name and empty file: fallback to default mimetype
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("bad_file_name", Blobs.createBlob(""),
                "default/mimetype");
        assertEquals("default/mimetype", mimetype);

        // test ambiguous file extension with wordml sniffing
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("sample-wordml.xml", getWordMLBlob(),
                "default/mimetype");
        assertEquals("application/msword", mimetype);

        // test ambiguous file extension with empty file
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("sample-wordml.xml", Blobs.createBlob(""),
                "default/mimetype");
        assertEquals("default/mimetype", mimetype);
    }

    @Test
    public void testGetMimetypeFromFilenameWithBlobMimetypeFallback() throws Exception {

        MimetypeEntry mimetypeEntry = getMimetypeSample();
        mimetypeRegistryService.registerMimetype(mimetypeEntry);

        // bad filename + word mimetype : fallback to excel mimetype
        Blob blob = Blobs.createBlob("");
        blob.setMimeType("application/msword");
        String mimetype = mimetypeRegistry.getMimetypeFromFilenameWithBlobMimetypeFallback("bad_file_name", blob,
                "default/mimetype");
        assertEquals("application/msword", mimetype);

        // bad filename + bad mimetype : fallback to sniffing blob
        blob = getWordBlob();
        blob.setMimeType("bad/mimetype");
        mimetype = mimetypeRegistry.getMimetypeFromFilenameWithBlobMimetypeFallback("bad_file_name", blob,
                "default/mimetype");
        assertEquals("application/msword", mimetype);

        // bad filename + bad mimetype : fallback to default mimetype
        blob = Blobs.createBlob("");
        blob.setMimeType("bad/mimetype");
        mimetype = mimetypeRegistry.getMimetypeFromFilenameWithBlobMimetypeFallback("bad_file_name", blob,
                "default/mimetype");
        assertEquals("default/mimetype", mimetype);
    }

    @Test
    public void testGetMimetypeEntryByMimetype() {

        MimetypeEntry mimetypeEntry = getMimetypeSample();
        mimetypeRegistryService.registerMimetype(mimetypeEntry);

        // Using normalized name.
        MimetypeEntry entry = mimetypeRegistry.getMimetypeEntryByMimeType("application/msword");
        assertNotNull(entry);
        assertEquals("application/msword", entry.getNormalized());

        // Using other mimetype
        entry = mimetypeRegistry.getMimetypeEntryByMimeType("app/whatever-word");
        assertNotNull(entry);
        assertEquals("application/msword", entry.getNormalized());
    }

}
