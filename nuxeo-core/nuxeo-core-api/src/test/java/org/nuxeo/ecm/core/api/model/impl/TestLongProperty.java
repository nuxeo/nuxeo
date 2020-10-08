/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.api.model.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.model.impl.primitives.LongProperty;

/**
 * since 11.3
 */
public class TestLongProperty {

    // NXP-21725
    @Test
    public void testNormalize() {
        LongProperty prop = new LongProperty(null, null, -1);

        // conversion ok
        assertNull(prop.normalize(null));
        assertEquals(1L, prop.normalize(1));
        assertEquals(1L, prop.normalize(1L));
        assertEquals(10L, prop.normalize("10"));
        assertEquals(10L, prop.normalize(10.5));

        // conversion ko
        String message = "Property Conversion failed from class java.lang.String to class java.lang.Long: For input string: \"10.5\"";
        assertConversionFailed(message, "10.5");
        message = "Property Conversion failed from class java.lang.String to class java.lang.Long: For input string: \"foo\"";
        assertConversionFailed(message, "foo");
        message = "Property Conversion failed from class java.lang.Boolean to class java.lang.Long";
        assertConversionFailed(message, true);
    }

    private void assertConversionFailed(String expectedMessage, Object value) {
        LongProperty prop = new LongProperty(null, null, -1);
        try {
            prop.normalize(value);
            fail("Exception expected");
        } catch (PropertyConversionException e) {
            assertEquals(expectedMessage, e.getMessage());
        }
    }
}
