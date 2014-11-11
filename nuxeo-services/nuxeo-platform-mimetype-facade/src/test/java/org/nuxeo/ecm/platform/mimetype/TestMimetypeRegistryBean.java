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
 * $Id: TestMimetypeRegistryBean.java 20592 2007-06-16 16:33:37Z sfermigier $
 */

package org.nuxeo.ecm.platform.mimetype;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.mimetype.ejb.MimetypeRegistryBean;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/*
 * Test the EJB facade of the mimetype registry NX service.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestMimetypeRegistryBean extends NXRuntimeTestCase {

    private MimetypeRegistryBean mimetypeRegistry;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.mimetype.facade.tests",
                "nxmimetype-service.xml");
        mimetypeRegistry = new MimetypeRegistryBean();
    }

    @Override
    public void tearDown() throws Exception {
        mimetypeRegistry = null;
        super.tearDown();
    }

    private static MimetypeEntryImpl getMimetypeSample() {
        String normalized = "application/msword";

        List<String> mimetypes = new ArrayList<String>();
        mimetypes.add("application/msword");
        // fake
        mimetypes.add("app/whatever-word");

        List<String> extensions = new ArrayList<String>();
        extensions.add("doc");
        extensions.add("fake");

        String iconPath = "icons/pdf.png";

        boolean binary = true;

        boolean onlineEditable = true;
        boolean oleSupported = true;

        return new MimetypeEntryImpl(normalized, mimetypes, extensions,
                iconPath, binary, onlineEditable, oleSupported);
    }

    public void testGetExtensionsFromMimetype() {
        org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry mimetype = getMimetypeSample();
        mimetypeRegistry.registerMimetype(mimetype);

        assertEquals(mimetypeRegistry.getExtensionsFromMimetypeName(mimetype
                .getNormalized()), mimetype.getExtensions());

        mimetypeRegistry.unregisterMimetype(mimetype.getNormalized());
    }

    public void testMimetypeRegistration() {
        org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry mimetype = getMimetypeSample();
        mimetypeRegistry.registerMimetype(mimetype);
        assertNotNull(mimetypeRegistry.getMimetypeEntryByName(mimetype
                .getNormalized()));

        // Second registration
        mimetypeRegistry.registerMimetype(mimetype);
        assertNotNull(mimetypeRegistry.getMimetypeEntryByName(mimetype
                .getNormalized()));

        mimetypeRegistry.unregisterMimetype(mimetype.getNormalized());
        assertNull(mimetypeRegistry.getMimetypeEntryByName(mimetype.getNormalized()));
    }

    public void testSniffWordFromFile() throws Exception {
        org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry mimetypeEntry = getMimetypeSample();
        mimetypeRegistry.registerMimetype(mimetypeEntry);

        File file = FileUtils.getResourceFileFromContext("test-data/hello.doc");

        String mimetype = mimetypeRegistry.getMimetypeFromFile(file);
        assertEquals("application/msword", mimetype);

        List<String> extensions = mimetypeRegistry
                .getExtensionsFromMimetypeName(mimetype);
        assertTrue(extensions.contains("doc"));
    }

    public void testSniffWordFromStream() throws Exception {
        org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry mimetypeEntry = getMimetypeSample();
        mimetypeRegistry.registerMimetype(mimetypeEntry);

        InputStream istream = new FileInputStream(FileUtils.getResourceFileFromContext("test-data/hello.doc"));

        String mimetype = mimetypeRegistry.getMimetypeFromStream(istream);
        assertEquals("application/msword", mimetype);

        List<String> extensions = mimetypeRegistry
                .getExtensionsFromMimetypeName(mimetype);
        assertTrue(extensions.contains("doc"));
    }

    public void getMimetypeEntryByMimetype() {
        org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry mimetypeEntry = getMimetypeSample();
        mimetypeRegistry.registerMimetype(mimetypeEntry);

        // Using getNormalized name.
        MimetypeEntry entry = mimetypeRegistry
                .getMimetypeEntryByMimeType("application/msword");
        assertNotNull(entry);
        assertEquals("Microsoft word", entry.getNormalized());

        // Using other mimetype
        entry = mimetypeRegistry
                .getMimetypeEntryByMimeType("app/whatever-word");
        assertNotNull(entry);
        assertEquals("Microsoft word", entry.getNormalized());
    }

}
