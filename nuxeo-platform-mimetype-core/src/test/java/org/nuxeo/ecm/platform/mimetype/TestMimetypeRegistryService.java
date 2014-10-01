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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.service.ExtensionDescriptor;
import org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService;

/*
 * Test the Nuxeo component. No deployment here (true unit test).
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
//disabled for now, see NXP-15315
@Ignore
public class TestMimetypeRegistryService {

    private MimetypeRegistryService mimetypeRegistry;

    @Before
    public void setUp() {
        mimetypeRegistry = new MimetypeRegistryService();
    }

    @After
    public void tearDown() {
        mimetypeRegistry = null;
    }

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

        return new MimetypeEntryImpl(normalizedMimetype, mimetypes, extensions,
                iconPath, binary, onlineEditable, oleSupported);
    }

    @Test
    public void testMimetypeRegistration() {
        MimetypeEntry mimetype = getMimetypeSample();
        mimetypeRegistry.registerMimetype(mimetype);
        assertEquals(
                mimetypeRegistry.getMimetypeEntryByName(mimetype.getNormalized()),
                mimetype);

        // Second registration
        mimetypeRegistry.registerMimetype(mimetype);
        assertEquals(
                mimetypeRegistry.getMimetypeEntryByName(mimetype.getNormalized()),
                mimetype);

        mimetypeRegistry.unregisterMimetype(mimetype.getNormalized());
        assertNull(mimetypeRegistry.getMimetypeEntryByName(mimetype.getNormalized()));
    }

    @Test
    public void testGetExtensionsFromMimetype() {
        MimetypeEntry mimetype = getMimetypeSample();
        mimetypeRegistry.registerMimetype(mimetype);

        assertEquals(
                mimetypeRegistry.getExtensionsFromMimetypeName(mimetype.getNormalized()),
                mimetype.getExtensions());

        mimetypeRegistry.unregisterMimetype(mimetype.getNormalized());
    }

    @Test
    public void testGetMimetypeFromFile() throws Exception {
        MimetypeEntry mimetypeEntry = getMimetypeSample();
        mimetypeRegistry.registerMimetype(mimetypeEntry);

        File file = FileUtils.getResourceFileFromContext("test-data/hello.doc");

        String mimetype = mimetypeRegistry.getMimetypeFromFile(file);
        assertEquals("application/msword", mimetype);

        List<String> extensions = mimetypeRegistry.getExtensionsFromMimetypeName(mimetype);
        assertTrue(extensions.contains("doc"));
    }

    @Test
    public void testGetMimetypeFromStream() throws Exception {
        MimetypeEntry mimetypeEntry = getMimetypeSample();
        mimetypeRegistry.registerMimetype(mimetypeEntry);

        InputStream istream = new FileInputStream(
                FileUtils.getResourceFileFromContext("test-data/hello.doc"));

        String mimetype = mimetypeRegistry.getMimetypeFromStream(istream);
        assertEquals("application/msword", mimetype);

        List<String> extensions = mimetypeRegistry.getExtensionsFromMimetypeName(mimetype);
        assertTrue(extensions.contains("doc"));
    }

    @Test
    public void testGetMimetypeFromBlob() throws Exception {
        MimetypeEntry mimetypeEntry = getMimetypeSample();
        mimetypeRegistry.registerMimetype(mimetypeEntry);

        InputStream istream = new FileInputStream(
                FileUtils.getResourceFileFromContext("test-data/hello.doc"));

        String mimetype = mimetypeRegistry.getMimetypeFromBlob(StreamingBlob.createFromStream(istream));
        assertEquals("application/msword", mimetype);

        List<String> extensions = mimetypeRegistry.getExtensionsFromMimetypeName(mimetype);
        assertTrue(extensions.contains("doc"));
    }

    protected static Blob getBlob(String filename) throws FileNotFoundException {
        InputStream istream = new FileInputStream(
                FileUtils.getResourceFileFromContext(filename));
        return StreamingBlob.createFromStream(istream);
    }

    protected static Blob getWordBlob() throws FileNotFoundException {
        return getBlob("test-data/hello.doc");
    }

    protected static Blob getWordMLBlob() throws FileNotFoundException {
        return getBlob("test-data/wordml-sample.xml");
    }

    @Test
    public void testGetMimetypeFromFilnameAndBlobWithDefault() throws Exception {

        MimetypeEntry mimetypeEntry = getMimetypeSample();
        mimetypeRegistry.registerMimetype(mimetypeEntry);

        MimetypeEntry docbook = new MimetypeEntryImpl(
                "application/docbook+xml",
                Arrays.asList("application/docbook+xml"),
                Arrays.asList("xml", "doc.xml", "docb"),
                "", false, false, false);
        mimetypeRegistry.registerMimetype(docbook);

        ExtensionDescriptor ed = new ExtensionDescriptor("xml");
        ed.setAmbiguous(true);
        ed.setMimetype("text/xml");
        mimetypeRegistry.registerFileExtension(ed);

        // doc filename + empty file gives word mimetype
        String mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault(
                "hello.doc", StreamingBlob.createFromByteArray(new byte[0]),
                "default/mimetype");
        assertEquals("application/msword", mimetype);
        List<String> extensions = mimetypeRegistry.getExtensionsFromMimetypeName(mimetype);
        assertTrue(extensions.contains("doc"));

        // bad filename extension + word file gives word mimetype
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault(
                "bad_file_name.ext", getWordBlob(), "default/mimetype");
        assertEquals("application/msword", mimetype);

        // bad filename (without extension) + word file gives word mimetype
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault(
                "bad_file_name", getWordBlob(), "default/mimetype");
        assertEquals("application/msword", mimetype);

        // bad name and empty file: fallback to default mimetype
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault(
                "bad_file_name",
                StreamingBlob.createFromByteArray(new byte[0]),
                "default/mimetype");
        assertEquals("default/mimetype", mimetype);

        // test ambiguous file extension with wordml sniffing
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault(
                "sample-wordml.xml", getWordMLBlob(), "default/mimetype");
        assertEquals("application/msword", mimetype);

        // test ambiguous file extension with empty file
        mimetype = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault(
                "sample-wordml.xml",
                StreamingBlob.createFromByteArray(new byte[0]),
                "default/mimetype");
        assertEquals("default/mimetype", mimetype);
    }

    @Test
    public void testGetMimetypeEntryByMimetype() {

        MimetypeEntry mimetypeEntry = getMimetypeSample();
        mimetypeRegistry.registerMimetype(mimetypeEntry);

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
