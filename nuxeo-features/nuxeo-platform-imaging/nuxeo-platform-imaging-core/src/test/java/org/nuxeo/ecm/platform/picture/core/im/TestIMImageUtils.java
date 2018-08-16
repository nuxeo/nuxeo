/*
 * (C) Copyright 2010-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.picture.core.im;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.platform.picture.core.im.IMImageUtils.ImageMagickCaller;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.platform.picture.core:OSGI-INF/commandline-imagemagick-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.convert:OSGI-INF/commandline-imagemagick-convert-contrib.xml")
public class TestIMImageUtils {

    protected ImageMagickCaller imc = new ImageMagickCaller() {
        @Override
        public void callImageMagick() {
            return;
        }
    };

    protected String checkFileBlob(String filename, boolean usefilename, String targetExt) throws Exception {
        File file = FileUtils.getResourceFileFromContext(filename);
        Blob blob = Blobs.createBlob(file);
        if (usefilename) {
            blob.setFilename(filename);
        }
        return check(blob, targetExt);
    }

    protected String checkStringBlob(String filename, boolean usefilename, String targetExt) throws Exception {
        File file = FileUtils.getResourceFileFromContext(filename);
        byte[] bytes = org.apache.commons.io.FileUtils.readFileToByteArray(file);
        Blob blob = Blobs.createBlob(bytes);
        if (usefilename) {
            blob.setFilename(filename);
        }
        return check(blob, targetExt);
    }

    protected String check(Blob blob, String targetExt) throws Exception {
        assertNotNull(blob);
        imc.makeFiles(blob, targetExt);
        return "src=" + FilenameUtils.getExtension(imc.sourceFile.getName()) + " dst="
                + FilenameUtils.getExtension(imc.targetFile.getName()) + " tmp="
                + FilenameUtils.getExtension(imc.tmpFile == null ? ""
                        : imc.tmpFile.getName());
    }

    @Test
    public void testImageMagickCaller_MakeFiles() throws Exception {
        String filename = "images/test.jpg";
        // FileBlob
        assertEquals("src=jpg dst=jpg tmp=", checkFileBlob(filename, true, null));
        assertEquals("src=jpg dst=jpg tmp=", checkFileBlob(filename, false, null));
        assertEquals("src=jpg dst=png tmp=", checkFileBlob(filename, true, "png"));
        assertEquals("src=jpg dst=png tmp=", checkFileBlob(filename, false, "png"));
        // StringBlob
        assertEquals("src=jpg dst=jpg tmp=jpg", checkStringBlob(filename, true, null));
        assertEquals("src=JPEG dst=JPEG tmp=JPEG", checkStringBlob(filename, false, null));
        assertEquals("src=jpg dst=png tmp=jpg", checkStringBlob(filename, true, "png"));
        assertEquals("src=JPEG dst=png tmp=JPEG", checkStringBlob(filename, false, "png"));
    }

    @Test
    public void testImageMagickCaller_CallSetsFilename() throws IOException {
        File file = FileUtils.getResourceFileFromContext("images/test.jpg");
        Blob blob = Blobs.createBlob(file);
        Blob result = imc.call(blob, "pdf", "converter");
        assertEquals("test.pdf", result.getFilename());
    }

}
