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

package org.nuxeo.ecm.platform.transform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.transform.service.TransformService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public abstract class AbstractPluginTestCase extends NXRuntimeTestCase {

    protected TransformService service;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.transform.plugin.tests",
                "PlatformService.xml");
        deployContrib("org.nuxeo.ecm.platform.transform.plugin.tests",
                "DefaultPlatform.xml");
        deployContrib("org.nuxeo.ecm.platform.transform.plugin.tests",
                "nxmimetype-bundle.xml");
        deployContrib("org.nuxeo.ecm.platform.transform.plugin.tests",
                "nxtransform-bundle.xml");
        deployContrib("org.nuxeo.ecm.platform.transform.plugin.tests",
                "nxtransform-plugins-bundle.xml");
        deployContrib("org.nuxeo.ecm.platform.transform.plugin.tests",
                "nxtransform-platform-contrib.xml");
        service = (TransformService) Framework.getRuntime().getComponent(TransformService.NAME);
    }

    protected static File createTempFile(String extension) throws IOException {
        File tempFile = File.createTempFile("document", '.' + extension);
        tempFile.deleteOnExit();
        return tempFile;
    }

    protected static void assertFileCreated(File file) {
        assertTrue("file created", file.isFile() && file.length() > 0);
    }

    protected static Blob getBlobFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        return new FileBlob(file);
    }

    protected static File getFileFromInputStream(InputStream stream, String ext)
            throws IOException {
        File file = createTempFile(ext);
        FileUtils.copyToFile(stream, file);
        file.deleteOnExit();
        return file;
    }

}
