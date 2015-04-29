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

package org.nuxeo.ecm.platform.template.tests;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.platform.convert.ooomanager.OOoManagerService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public abstract class BaseConverterTest extends Assert {

    private static final Log log = LogFactory.getLog(BaseConverterTest.class);

    final NXRuntimeTestCase tc = new NXRuntimeTestCase();

    OOoManagerService oooManagerService;

    protected static BlobHolder getBlobFromPath(String path, String srcMT) throws IOException {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);

        Blob blob = Blobs.createBlob(file);
        if (srcMT != null) {
            blob.setMimeType(srcMT);
        }
        blob.setFilename(file.getName());
        return new SimpleBlobHolder(blob);
    }

    protected static BlobHolder getBlobFromPath(String path) throws IOException {
        return getBlobFromPath(path, null);
    }

    @Before
    public void setUp() throws Exception {
        tc.setUp();
        tc.deployBundle("org.nuxeo.ecm.core.api");
        tc.deployBundle("org.nuxeo.ecm.core.convert.api");
        tc.deployBundle("org.nuxeo.ecm.core.mimetype");
        tc.deployBundle("org.nuxeo.ecm.core.convert");
        tc.deployBundle("org.nuxeo.ecm.platform.convert");
        tc.deployContrib("org.nuxeo.template.manager", "OSGI-INF/convert-service-contrib.xml");

        oooManagerService = Framework.getService(OOoManagerService.class);
        try {
            oooManagerService.startOOoManager();
        } catch (Exception e) {
            log.warn("Can't run OpenOffice, JOD converter will not be available.");
        }
    }

    @After
    public void tearDown() throws Exception {
        oooManagerService = Framework.getService(OOoManagerService.class);
        if (oooManagerService.isOOoManagerStarted()) {
            oooManagerService.stopOOoManager();
        }
        tc.tearDown();
    }

}
