/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.core.convert.plugins.tests.advanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.plugins.tests.SimpleConverterTest;
import org.nuxeo.ecm.core.convert.plugins.text.extractors.DOCX2TextConverter;
import org.nuxeo.ecm.core.convert.plugins.text.extractors.MSOffice2TextConverter;
import org.nuxeo.ecm.core.convert.plugins.text.extractors.PPTX2TextConverter;

public class AdvancedMSOfficeConverterTest extends SimpleConverterTest {

    /**
     * Note that {@link MSOffice2TextConverter}, contrary to
     * {@link DOCX2TextConverter} or {@link PPTX2TextConverter} cannot
     * distinguish headings from paragraphs, so there are no additional new
     * lines before headings in the converted text.
     */
    @Override
    protected void checkTextConversion(String textContent) {

        try {
            Blob blob = new FileBlob(
                    FileUtils.getResourceFileFromContext("test-docs/advanced/MSOffice_paragraphs.txt"));
            blob.setEncoding("UTF-8");

            // Get blob string with Unix end of line characters
            String expectedContent = blob.getString().replace("\r\n", "\n");

            assertEquals(expectedContent.trim(), textContent.trim());
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
    }
}
