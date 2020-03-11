/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - Initial Implementation
 */

package org.nuxeo.ecm.core.convert.plugins.tests.advanced;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.convert.plugins.tests.SimpleConverterTest;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestAdvancedXLX2TextConversion extends SimpleConverterTest {

    @Test
    public void testCellSeparator() throws Exception {
        doTestTextConverter("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlx2text",
                "advanced/cell_separator.xlsx");
    }

    @Override
    protected void checkTextConversion(String textContent) {
        String[] cells = textContent.trim().split(" ");
        assertTrue(cells.length == 2);
        assertArrayEquals(cells, new String[] { "hello", "world" });
    }
}
