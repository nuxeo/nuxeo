/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.diff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.nuxeo.ecm.diff.model.DifferenceType;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.PropertyDiff;
import org.nuxeo.ecm.diff.model.SchemaDiff;
import org.nuxeo.ecm.diff.model.impl.ComplexPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.ContentPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.SimplePropertyDiff;

/**
 * Super class for diff test cases.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class DiffTestCase {

    protected XpathEngine xPathEngine = XMLUnit.newXpathEngine();

    /**
     * Checks a null schema diff: docDiff holds no diff for schema.
     *
     * @param docDiff the doc diff
     * @param schema the schema
     * @return the schema diff
     */
    protected final void checkNullSchemaDiff(DocumentDiff docDiff, String schema) {

        SchemaDiff schemaDiff = docDiff.getSchemaDiff(schema);
        assertNull(String.format("Schema diff should be null for schema %s", schema), schemaDiff);
    }

    /**
     * Checks a schema diff.
     *
     * @param docDiff the doc diff
     * @param schema the schema
     * @param expectedFieldCount the expected field count
     * @return the schema diff
     */
    protected final SchemaDiff checkSchemaDiff(DocumentDiff docDiff, String schema, int expectedFieldCount) {

        SchemaDiff schemaDiff = docDiff.getSchemaDiff(schema);
        assertNotNull("Schema diff should not be null", schemaDiff);
        assertEquals("Wrong field count", expectedFieldCount, schemaDiff.getFieldCount());

        return schemaDiff;
    }

    /**
     * Check identical field.
     *
     * @param fieldDiff the field diff
     */
    protected final void checkIdenticalField(PropertyDiff fieldDiff) {

        assertNull("Field diff should be null", fieldDiff);
    }

    /**
     * Check simple field diff.
     *
     * @param fieldDiff the field diff
     * @param expectedPropertyType the expected property type
     * @param expectedLeftValue the expected left value
     * @param expectedRightValue the expected right value
     * @return the simple property diff
     */
    protected final SimplePropertyDiff checkSimpleFieldDiff(PropertyDiff fieldDiff, String expectedPropertyType,
            String expectedLeftValue, String expectedRightValue) {

        return checkSimpleFieldDiff(fieldDiff, expectedPropertyType, DifferenceType.different, expectedLeftValue,
                expectedRightValue);
    }

    /**
     * Checks a simple field diff.
     *
     * @param fieldDiff the field diff
     * @param expectedPropertyType the expected property type
     * @param expectedDifferenceType the expected difference type
     * @param expectedLeftValue the expected left value
     * @param expectedRightValue the expected right value
     * @return the property diff
     */
    protected final SimplePropertyDiff checkSimpleFieldDiff(PropertyDiff fieldDiff, String expectedPropertyType,
            DifferenceType expectedDifferenceType, String expectedLeftValue, String expectedRightValue) {

        assertNotNull("Field diff should not be null", fieldDiff);
        assertTrue("Wrong PropertyDiff implementation", fieldDiff instanceof SimplePropertyDiff);

        assertEquals("Wrong property type", expectedPropertyType, fieldDiff.getPropertyType());
        SimplePropertyDiff simpleFieldDiff = (SimplePropertyDiff) fieldDiff;
        assertEquals("Wrong difference type", expectedDifferenceType, simpleFieldDiff.getDifferenceType());
        assertEquals("Wrong left value", expectedLeftValue, simpleFieldDiff.getLeftValue());
        assertEquals("Wrong right value", expectedRightValue, simpleFieldDiff.getRightValue());

        return simpleFieldDiff;
    }

    /**
     * Checks a content field diff.
     *
     * @param fieldDiff the field diff
     * @param expectedContentFieldDiff the expected content field diff
     * @return the property diff
     */
    protected final ContentPropertyDiff checkContentFieldDiff(PropertyDiff fieldDiff,
            ContentPropertyDiff expectedContentFieldDiff) {

        assertNotNull("Field diff should not be null", fieldDiff);
        assertTrue("Wrong PropertyDiff implementation", fieldDiff instanceof ContentPropertyDiff);

        ContentPropertyDiff contentFieldDiff = (ContentPropertyDiff) fieldDiff;
        assertEquals("Wrong list diff", expectedContentFieldDiff, contentFieldDiff);

        return contentFieldDiff;
    }

    /**
     * Checks a list field diff.
     *
     * @param fieldDiff the field diff
     * @param expectedListFieldDiff the expected list field diff
     * @return the property diff
     */
    protected final PropertyDiff checkListFieldDiff(PropertyDiff fieldDiff, ListPropertyDiff expectedListFieldDiff) {

        assertNotNull("Field diff should not be null", fieldDiff);
        assertTrue("Wrong PropertyDiff implementation", fieldDiff instanceof ListPropertyDiff);

        ListPropertyDiff listFieldDiff = (ListPropertyDiff) fieldDiff;
        assertEquals("Wrong list diff", expectedListFieldDiff, listFieldDiff);

        return listFieldDiff;
    }

    /**
     * Checks a complex field diff.
     *
     * @param fieldDiff the field diff
     * @param expectedComplexFieldDiff the expected complex field diff
     * @return the property diff
     */
    protected final PropertyDiff checkComplexFieldDiff(PropertyDiff fieldDiff,
            ComplexPropertyDiff expectedComplexFieldDiff) {

        assertNotNull("Field diff should not be null", fieldDiff);
        assertTrue("Wrong PropertyDiff implementation", fieldDiff instanceof ComplexPropertyDiff);

        ComplexPropertyDiff complexFieldDiff = (ComplexPropertyDiff) fieldDiff;
        assertEquals("Wrong complex diff", expectedComplexFieldDiff, complexFieldDiff);

        return complexFieldDiff;
    }

}
