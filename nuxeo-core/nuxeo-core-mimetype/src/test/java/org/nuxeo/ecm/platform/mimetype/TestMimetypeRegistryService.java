/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
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

    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

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

        assertEquals(mimetypeRegistry.getExtensionsFromMimetypeName(mimetype.getNormalized()), mimetype.getExtensions());

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
    public void testGetMimetypeFromStream() throws Exception {
        MimetypeEntry mimetypeEntry = getMimetypeSample();
        mimetypeRegistryService.registerMimetype(mimetypeEntry);

        InputStream istream = new FileInputStream(FileUtils.getResourceFileFromContext("test-data/hello.doc"));

        String mimetype = mimetypeRegistry.getMimetypeFromStream(istream);
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
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("sample-wordml.xml",
                Blobs.createBlob(""), "default/mimetype");
        assertEquals("default/mimetype", mimetype);

        // test temporary file, extension .tmp
        Blob blob = Blobs.createBlob("", "foo/bar");
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("myfile.TMP", blob, "default/mimetype");
        assertEquals(APPLICATION_OCTET_STREAM, mimetype);

        // test MS Office temporary file, starting with tilde-dollar (~$)
        blob = Blobs.createBlob("", "foo/bar");
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault("~$sample-docx.docx", blob,
                "default/mimetype");
        assertEquals(APPLICATION_OCTET_STREAM, mimetype);
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
