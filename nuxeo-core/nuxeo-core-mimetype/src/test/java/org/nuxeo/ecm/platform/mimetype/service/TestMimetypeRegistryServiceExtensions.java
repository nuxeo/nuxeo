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
 * $Id: TestMimetypeRegistryServiceExtensions.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.mimetype.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/*
 * Test the Nuxeo component and mimetype extension regisration. (int)
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.mimetype")
public class TestMimetypeRegistryServiceExtensions {

    @Inject
    private MimetypeRegistry mimetypeRegistry;

    private MimetypeEntry wordMimeType;

    private MimetypeEntry pdfMimeType;

    private ExtensionDescriptor xmlExtension;

    @Before
    public void setUp() throws Exception {
        wordMimeType = mimetypeRegistry.getMimetypeEntryByName("application/msword");
        pdfMimeType = mimetypeRegistry.getMimetypeEntryByName("application/pdf");
        xmlExtension = ((MimetypeRegistryService) mimetypeRegistry).extensionRegistry.get("xml");
    }

    @Test
    public void testComponentRegistration() {
        assertNotNull(mimetypeRegistry);
    }

    @Test
    public void testPluginsRegistration() {
        assertEquals("application/msword", wordMimeType.getNormalized());
        assertEquals("application/pdf", pdfMimeType.getNormalized());

        assertTrue(xmlExtension.isAmbiguous());
        assertEquals("text/xml", xmlExtension.getMimetype());
    }

    @Test
    public void testPdfMimetypes() {
        assertEquals("application/pdf", pdfMimeType.getNormalized());
        assertEquals("application", pdfMimeType.getMajor());
        assertEquals("pdf", pdfMimeType.getMinor());
        List<String> mimetypes = pdfMimeType.getMimetypes();
        assertTrue(mimetypes.contains("application/pdf"));
        assertFalse(mimetypes.contains("app-whatever-pdf"));
    }

    @Test
    public void testPdfExtensions() {
        List<String> extensions = pdfMimeType.getExtensions();
        assertTrue(extensions.contains("pdf"));
        assertTrue(!extensions.contains("fake"));
    }

    @Test
    public void testPdfBinary() {
        assertTrue(pdfMimeType.isBinary());
    }

    @Test
    public void testPdfOnlineEditable() {
        assertFalse(pdfMimeType.isOnlineEditable());
    }

    public void xtestPdfIcon() {
        String iconPath = pdfMimeType.getIconPath();
        assertNotNull(iconPath);
    }

    @Test
    public void testWordMimetypes() {
        assertEquals("application/msword", wordMimeType.getNormalized());
        assertEquals("application", wordMimeType.getMajor());
        assertEquals("msword", wordMimeType.getMinor());
        List<String> mimetypes = wordMimeType.getMimetypes();
        assertEquals(1, mimetypes.size());
        assertTrue(mimetypes.contains("application/msword"));
    }

    @Test
    public void testWordExtensions() {
        List<String> extensions = wordMimeType.getExtensions();
        assertEquals(2, extensions.size());
        assertTrue(extensions.contains("doc"));
        assertTrue(extensions.contains("dot"));
    }

    @Test
    public void testWordBinary() {
        assertTrue(wordMimeType.isBinary());
    }

    @Test
    public void testWordOnlineEditable() {
        assertTrue(wordMimeType.isOnlineEditable());
    }

    public void xtestWordIcon() {
        String iconPath = wordMimeType.getIconPath();
        assertNotNull(iconPath);
    }

    public void xtestRemote() throws Exception {
        wordMimeType = mimetypeRegistry.getMimetypeEntryByName("application/msword");
        assertEquals("application/msword", wordMimeType.getNormalized());

        pdfMimeType = mimetypeRegistry.getMimetypeEntryByName("application/pdf");
        List<String> extensions = pdfMimeType.getExtensions();
        assertTrue(extensions.contains("pdf"));
    }

}
