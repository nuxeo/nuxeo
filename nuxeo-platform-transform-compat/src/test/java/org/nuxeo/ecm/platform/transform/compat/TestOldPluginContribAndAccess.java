/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.transform.compat;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test Contribution of old style plugins.
 *
 * @author tiry
 */
public class TestOldPluginContribAndAccess extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.platform.transform.api");
        deployBundle("org.nuxeo.ecm.platform.transform");
        deployContrib("org.nuxeo.ecm.platform.transform.compat.tests", "OSGI-INF/old-transform-contrib-test.xml");
    }

    public void testAccessViaNewAPI() throws Exception {
        ConversionService cs = Framework.getLocalService(ConversionService.class);
        assertNotNull(cs);

        String converterName = cs.getConverterName("foo/bar", "text/html");
        assertNotNull(converterName);
        assertEquals("dummyOldPlugin", converterName);

        Blob blob = new StringBlob("Yo");
        BlobHolder bh = new SimpleBlobHolder(blob);
        BlobHolder result = cs.convert(converterName, bh, null);
        assertNotNull(result);
        assertTrue(result.getBlob().getString().startsWith("<html> DummyTest:Yo"));

        String converterName2 = cs.getConverterName("dummy/bar", "foo/bar");
        assertNotNull(converterName2);
        assertEquals("dummyOldPlugin2", converterName2);

        String converterName3 = cs.getConverterName("dummy/bar", "text/html");
        assertNotNull(converterName3);
        assertEquals("dummy2html", converterName3);

        blob = new StringBlob("Yo2");
        blob.setMimeType("dummy/bar");
        bh = new SimpleBlobHolder(blob);

        result = cs.convert(converterName3, bh, null);
        assertTrue(result.getBlob().getString().startsWith("<html> DummyTest:DummyTest2:Yo2"));
    }

}
