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
 *     Antoine Taillefer
 */

package org.nuxeo.ecm.core.convert.plugins.tests.advanced;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestAdvancedMSOfficeConversion extends AdvancedMSOfficeConverterTest {

    // Test msoffice2text
    @Test
    public void testWordConverter() throws Exception {
        doTestTextConverter("application/msword", "msoffice2text", "advanced/paragraphs.doc");
    }

    @Test
    public void testPptConverter() throws Exception {
        doTestTextConverter("application/vnd.ms-powerpoint", "msoffice2text", "advanced/paragraphs.ppt");
    }
}
