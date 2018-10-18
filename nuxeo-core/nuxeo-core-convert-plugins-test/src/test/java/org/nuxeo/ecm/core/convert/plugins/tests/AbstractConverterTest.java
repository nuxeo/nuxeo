/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.core.convert.plugins.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.SystemUtils;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.runtime.api.Framework;

public abstract class AbstractConverterTest {

    /**
     * @deprecated Since 7.4. Use {@link SystemUtils#IS_OS_WINDOWS}
     */
    @Deprecated
    protected final boolean isWindows() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    protected final BlobHolder getBlobFromPath(String path) throws IOException {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        Blob blob = Blobs.createBlob(file);
        MimetypeRegistry mimetypeRegistry = Framework.getService(MimetypeRegistry.class);
        String mimeType = mimetypeRegistry.getMimetypeFromFilenameAndBlobWithDefault(file.getName(), blob, null);
        blob.setMimeType(mimeType);
        return new SimpleBlobHolder(blob);
    }

    protected String doTestTextConverter(String srcMT, String converter, String fileName) throws Exception {
        ConversionService cs = Framework.getService(ConversionService.class);
        String converterName = cs.getConverterName(srcMT, "text/plain");
        assertEquals(converter, converterName);

        BlobHolder hg;
        if (SystemUtils.IS_OS_WINDOWS) {
            hg = getBlobFromPath("test-docs\\" + fileName);
        } else {
            hg = getBlobFromPath("test-docs/" + fileName);
        }
        hg.getBlob().setMimeType(srcMT);

        Map<String, Serializable> parameters = new HashMap<>();
        BlobHolder result = cs.convert(converterName, hg, parameters);
        assertNotNull(result);

        String textContent = result.getBlob().getString();
        checkTextConversion(textContent);
        return textContent;
    }

    protected String doTestAny2TextConverter(String srcMT, String converterName, String fileName) throws Exception {
        ConversionService cs = Framework.getService(ConversionService.class);

        BlobHolder hg;
        if (SystemUtils.IS_OS_WINDOWS) {
            hg = getBlobFromPath("test-docs\\" + fileName);
        } else {
            hg = getBlobFromPath("test-docs/" + fileName);
        }
        hg.getBlob().setMimeType(srcMT);

        Map<String, Serializable> parameters = new HashMap<>();
        BlobHolder result = cs.convert(converterName, hg, parameters);
        assertNotNull(result);

        String textContent = result.getBlob().getString();
        checkAny2TextConversion(textContent);
        return textContent;
    }

    protected String doTestArabicTextConverter(String srcMT, String converter, String fileName) throws Exception {
        ConversionService cs = Framework.getService(ConversionService.class);
        assertTrue(cs.isConverterAvailable(converter).isAvailable());

        String converterName = cs.getConverterName(srcMT, "text/plain");
        assertEquals(converter, converterName);

        BlobHolder hg;
        if (SystemUtils.IS_OS_WINDOWS) {
            hg = getBlobFromPath("test-docs\\right-to-left\\" + fileName);
        } else {
            hg = getBlobFromPath("test-docs/right-to-left/" + fileName);
        }

        BlobHolder result = cs.convert(converterName, hg, null);
        assertNotNull(result);

        String textContent = result.getBlob().getString();
        checkArabicConversion(textContent);
        return textContent;
    }

    protected abstract void checkTextConversion(String textContent);

    protected abstract void checkAny2TextConversion(String textContent);

    protected abstract void checkArabicConversion(String textContent);

}
