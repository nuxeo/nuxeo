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

package org.nuxeo.ecm.platform.convert.tests;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.convert.ooomanager.OOoManagerService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

public abstract class BaseConverterTest extends Assert {

    private static final Log log = LogFactory.getLog(BaseConverterTest.class);

    NXRuntimeTestCase tc = new NXRuntimeTestCase();

    OOoManagerService oooManagerService;

    protected static BlobHolder getBlobFromPath(String path, String srcMT) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);

        Blob blob = new FileBlob(file);
        if (srcMT != null) {
            blob.setMimeType(srcMT);
        }
        blob.setFilename(file.getName());
        return new SimpleBlobHolder(blob);
    }

    protected static BlobHolder getBlobFromPath(String path) {
        return getBlobFromPath(path, null);
    }

    @Before
    public void setUp() throws Exception {
        tc.setUp();
        tc.deployBundle("org.nuxeo.ecm.core.api");
        tc.deployBundle("org.nuxeo.ecm.core.convert.api");
        tc.deployBundle("org.nuxeo.ecm.core.convert");
        tc.deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        tc.deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        tc.deployBundle("org.nuxeo.ecm.platform.convert");

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

    public static String readPdfText(File pdfFile) throws IOException {
        PDFTextStripper textStripper = new PDFTextStripper();
        PDDocument document = PDDocument.load(pdfFile);
        String text = textStripper.getText(document);
        document.close();
        return text.trim();
    }

}
