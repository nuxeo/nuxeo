/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.webapp.action;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestConversionActionBean {

    private ConversionActionBean conversionAction;

    @Before
    public void setUp() throws Exception {
        conversionAction = new ConversionActionBean();
    }

    @Test
    public void testIsExportableToPDFWithNullBlob() {
        boolean isExportable = conversionAction.isExportableToPDF((Blob) null);
        assertFalse("A null blob can't be export to PDF.", isExportable);
    }

    @Test
    public void testIsExportableToPDFWithCache() {
        String mimeType = "text/plain";
        conversionAction.pdfConverterForTypes = new HashMap<>();
        // Put true in cache
        ConverterCheckResult converter = new ConverterCheckResult();
        conversionAction.pdfConverterForTypes.put(mimeType, converter);
        Blob blob = new StringBlob("Blob for test", mimeType);
        boolean isExportable = conversionAction.isExportableToPDF(blob);
        assertTrue("Due to cache, text/plain is exportable.", isExportable);
        // Put false in cache
        converter = new ConverterCheckResult("installation messsage", "error message");
        conversionAction.pdfConverterForTypes.put(mimeType, converter);
        isExportable = conversionAction.isExportableToPDF(blob);
        assertFalse("Due to cache, text/plain is not exportable.", isExportable);
    }

}
