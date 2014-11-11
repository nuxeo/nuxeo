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

package org.nuxeo.ecm.platform.picture.core.im;

import java.io.File;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class TestIMImageUtils extends NXRuntimeTestCase {

    IMImageUtils service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployContrib("org.nuxeo.ecm.platform.picture.core",
                "OSGI-INF/commandline-imagemagick-contrib.xml");
        service = new IMImageUtils();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        service = null;
    }

    public void testGetTempSuffixUsingIM() throws Exception {
        File file = FileUtils.getResourceFileFromContext("images/test.jpg");
        assertNotNull(file);
        Blob blob = new FileBlob(file);
        assertNotNull(blob);
        String suffix = service.getTempSuffix(blob, file);
        assertEquals(".jpeg", suffix.toLowerCase());
    }

    public void testGetTempSuffixWithBlobFileName() throws Exception {
        File file = FileUtils.getResourceFileFromContext("images/dummy.raw");
        assertNotNull(file);
        Blob blob = new FileBlob(file);
        blob.setFilename("dummy.raw");
        assertNotNull(blob);
        String suffix = service.getTempSuffix(blob, file);
        assertTrue(suffix.toLowerCase().endsWith(".raw"));
    }

}
