/*
 * (C) Copyright 2010-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thomas Roger
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.picture.core.im;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.commons.io.FilenameUtils;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.picture.core.im.IMImageUtils.ImageMagickCaller;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestIMImageUtils extends NXRuntimeTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.commandline.executor");
        deployContrib("org.nuxeo.ecm.platform.picture.core",
                "OSGI-INF/commandline-imagemagick-contrib.xml");
    }

    protected String checkFileBlob(String filename, boolean usefilename,
            String targetExt) throws Exception {
        File file = FileUtils.getResourceFileFromContext(filename);
        Blob blob = new FileBlob(file);
        if (usefilename) {
            blob.setFilename(filename);
        }
        return check(blob, targetExt);
    }

    protected String checkStringBlob(String filename, boolean usefilename,
            String targetExt) throws Exception {
        File file = FileUtils.getResourceFileFromContext(filename);
        byte[] bytes = org.apache.commons.io.FileUtils.readFileToByteArray(file);
        Blob blob = new ByteArrayBlob(bytes);
        if (usefilename) {
            blob.setFilename(filename);
        }
        return check(blob, targetExt);
    }

    protected String check(Blob blob, String targetExt) throws Exception {
        assertNotNull(blob);
        ImageMagickCaller imc = new ImageMagickCaller() {
            @Override
            public void callImageMagick() throws Exception {
                return;
            }
        };
        try {
            imc.makeFiles(blob, targetExt);
            return "src="
                    + FilenameUtils.getExtension(imc.sourceFile.getName())
                    + " dst="
                    + FilenameUtils.getExtension(imc.targetFile.getName())
                    + " tmp="
                    + FilenameUtils.getExtension(imc.tmpFile == null ? ""
                            : imc.tmpFile.getName());
        } finally {
            if (imc.targetFile != null) {
                imc.targetFile.delete();
            }
            if (imc.tmpFile != null) {
                imc.tmpFile.delete();
            }
        }
    }

    @Test
    public void testImageMagickCaller_MakeFiles() throws Exception {
        String filename = "images/test.jpg";
        // FileBlob
        assertEquals("src=jpg dst=jpg tmp=",
                checkFileBlob(filename, true, null));
        assertEquals("src=jpg dst=jpg tmp=",
                checkFileBlob(filename, false, null));
        assertEquals("src=jpg dst=png tmp=",
                checkFileBlob(filename, true, "png"));
        assertEquals("src=jpg dst=png tmp=",
                checkFileBlob(filename, false, "png"));
        // StringBlob
        assertEquals("src=jpg dst=jpg tmp=jpg",
                checkStringBlob(filename, true, null));
        assertEquals("src=JPEG dst=JPEG tmp=JPEG",
                checkStringBlob(filename, false, null));
        assertEquals("src=jpg dst=png tmp=jpg",
                checkStringBlob(filename, true, "png"));
        assertEquals("src=JPEG dst=png tmp=JPEG",
                checkStringBlob(filename, false, "png"));
    }

}
