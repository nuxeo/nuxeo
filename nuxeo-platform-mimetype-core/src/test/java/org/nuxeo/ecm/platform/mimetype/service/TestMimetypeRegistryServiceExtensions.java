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
 * $Id: TestMimetypeRegistryServiceExtensions.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.mimetype.service;


import java.util.List;

import org.nuxeo.ecm.platform.mimetype.NXMimeType;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/*
 * Test the Nuxeo component and mimetype extension regisration. (int)
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestMimetypeRegistryServiceExtensions extends NXRuntimeTestCase {

    private MimetypeRegistryService mimetypeRegistryService;
    private MimetypeEntry wordMimeType;
    private MimetypeEntry pdfMimeType;
    private ExtensionDescriptor xmlExtension;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.mimetype.core.tests",
                "nxmimetype-service.xml");
        mimetypeRegistryService = NXMimeType.getMimetypeRegistryService();

        wordMimeType = mimetypeRegistryService.getMimetypeEntryByName("application/msword");
        pdfMimeType = mimetypeRegistryService.getMimetypeEntryByName("application/pdf");

        xmlExtension = mimetypeRegistryService.extensionRegistry.get("xml");
    }

    public void testComponentRegistration() {
        assertNotNull(mimetypeRegistryService);
    }

    public void testPluginsRegistration() {
        assertEquals("application/msword", wordMimeType.getNormalized());
        assertEquals("application/pdf", pdfMimeType.getNormalized());

        assertTrue(xmlExtension.isAmbiguous());
        assertEquals("text/xml", xmlExtension.getMimetype());
    }

    public void testPdfMimetypes() {
        assertEquals("application/pdf", pdfMimeType.getNormalized());
        assertEquals("application", pdfMimeType.getMajor());
        assertEquals("pdf", pdfMimeType.getMinor());
        List<String> mimetypes = pdfMimeType.getMimetypes();
        assertTrue(mimetypes.contains("application/pdf"));
        assertFalse(mimetypes.contains("app-whatever-pdf"));
    }

    public void testPdfExtensions() {
        List<String> extensions = pdfMimeType.getExtensions();
        assertTrue(extensions.contains("pdf"));
        assertTrue(!extensions.contains("fake"));
    }

    public void testPdfBinary() {
        assertTrue(pdfMimeType.isBinary());
    }

    public void testPdfOnlineEditable() {
        assertFalse(pdfMimeType.isOnlineEditable());
    }

    public void xtestPdfIcon() {
        String iconPath = pdfMimeType.getIconPath();
        assertNotNull(iconPath);
    }

    public void testWordMimetypes() {
        assertEquals("application/msword", wordMimeType.getNormalized());
        assertEquals("application", wordMimeType.getMajor());
        assertEquals("msword", wordMimeType.getMinor());
        List<String> mimetypes = wordMimeType.getMimetypes();
        assertEquals(1, mimetypes.size());
        assertTrue(mimetypes.contains("application/msword"));
    }

    public void testWordExtensions() {
        List<String> extensions = wordMimeType.getExtensions();
        assertEquals(1, extensions.size());
        assertTrue(extensions.contains("doc"));
    }

    public void testWordBinary() {
        assertTrue(wordMimeType.isBinary());
    }

    public void testWordOnlineEditable() {
        assertTrue(wordMimeType.isOnlineEditable());
    }

    public void xtestWordIcon() {
        String iconPath = wordMimeType.getIconPath();
        assertNotNull(iconPath);
    }

    public void xtestRemote() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.mimetype.core.tests",
                "nxmimetype-service.xml");
        MimetypeRegistry mimetypeRegistryService = Framework.getService(MimetypeRegistry.class);

        wordMimeType = mimetypeRegistryService.getMimetypeEntryByName("application/msword");
        assertEquals("application/msword", wordMimeType.getNormalized());

        pdfMimeType = mimetypeRegistryService.getMimetypeEntryByName("application/pdf");
        List<String> extensions = pdfMimeType.getExtensions();
        assertTrue(extensions.contains("pdf"));
    }

}
