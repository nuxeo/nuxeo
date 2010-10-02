/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.convert.tests;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.runtime.api.Framework;

public class TestWPD2TextConverter extends BaseConverterTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        tc.deployBundle("org.nuxeo.ecm.platform.commandline.executor");
    }

    @Test
    public void testWordPerfectToTextConverter() throws Exception {

        ConversionService cs = Framework.getLocalService(ConversionService.class);
        assertNotNull(cs);
        ConverterCheckResult check = cs.isConverterAvailable("wpd2text");
        assertNotNull(check);

        if (!check.isAvailable()) {
            System.out.print("Skipping Wordperfect conversion test since libpwd-tool is not installed");
            System.out.print(" converter check output : " + check.getInstallationMessage());
            System.out.print(" converter check output : " + check.getErrorMessage());
            return;
        }

        String converterName = cs.getConverterName("application/wordperfect", "text/plain");
        assertEquals("wpd2text", converterName);

        BlobHolder hg = getBlobFromPath("test-docs/test.wpd", "application/wordperfect");

        BlobHolder result = cs.convert(converterName, hg, null);
        assertNotNull(result);

        String txt = result.getBlob().getString();
        //System.out.println(txt);
        assertTrue(txt.contains("Zoonotic"));
        assertTrue(txt.contains("Committee"));
    }

}
