/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.convert.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 5.2
 */
@LocalDeploy("org.nuxeo.ecm.platform.convert:OSGI-INF/test-command-line-converter-contrib.xml")
public class TestCommandLineConverter extends BaseConverterTest {

    @Test
    public void testCommandLineConverter() throws Exception {
        checkConverterAvailability("testCommandLineConverter");
        checkCommandAvailability("pdftoimage");

        BlobHolder pdfBH = getBlobFromPath("test-docs/hello.pdf");
        Map<String, Serializable> parameters = new HashMap<>();
        parameters.put("targetFileName", "hello.png");

        BlobHolder result = cs.convert("testCommandLineConverter", pdfBH, parameters);
        assertNotNull(result);

        List<Blob> blobs = result.getBlobs();
        assertNotNull(blobs);
        assertEquals(1, blobs.size());

        Blob mainBlob = result.getBlob();
        assertEquals("hello.png", mainBlob.getFilename());
    }

}
