/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.convert.plugins.tests.advanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.convert.plugins.tests.SimpleConverterTest;
import org.nuxeo.ecm.core.convert.plugins.text.extractors.DOCX2TextConverter;
import org.nuxeo.ecm.core.convert.plugins.text.extractors.MSOffice2TextConverter;
import org.nuxeo.ecm.core.convert.plugins.text.extractors.PPTX2TextConverter;

public class AdvancedMSOfficeConverterTest extends SimpleConverterTest {

    protected String expectedContentFilename;

    /**
     * Note that {@link MSOffice2TextConverter}, contrary to {@link DOCX2TextConverter} or {@link PPTX2TextConverter}
     * cannot distinguish headings from paragraphs, so there are no additional new lines before headings in the
     * converted text.
     */
    @Override
    protected void checkTextConversion(String textContent) {

        try {
            Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-docs/" + expectedContentFilename));
            blob.setEncoding("UTF-8");

            // Get blob string with Unix end of line characters
            String expectedContent = blob.getString().replace("\r\n", "\n");

            assertEquals(expectedContent.trim(), textContent.trim());
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
    }
}
