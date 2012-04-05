/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */
package org.nuxeo.ecm.diff;

import static org.junit.Assert.*;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.nuxeo.ecm.diff.model.DifferenceType;
import org.nuxeo.ecm.diff.model.DocumentDiff;
import org.nuxeo.ecm.diff.model.PropertyDiff;
import org.nuxeo.ecm.diff.model.PropertyType;
import org.nuxeo.ecm.diff.model.SchemaDiff;
import org.nuxeo.ecm.diff.model.impl.ComplexPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.diff.model.impl.SimplePropertyDiff;

/**
 * Super class for diff test cases.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
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
        assertNull(String.format("Schema diff should be null for schema %s",
                schema), schemaDiff);
    }

    /**
     * Checks a schema diff.
     *
     * @param docDiff the doc diff
     * @param schema the schema
     * @param expectedFieldCount the expected field count
     * @return the schema diff
     */
    protected final SchemaDiff checkSchemaDiff(DocumentDiff docDiff,
            String schema, int expectedFieldCount) {

        SchemaDiff schemaDiff = docDiff.getSchemaDiff(schema);
        assertNotNull("Schema diff should not be null", schemaDiff);
        assertEquals("Wrong field count", expectedFieldCount,
                schemaDiff.getFieldCount());

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
     * Checks a simple field diff.
     *
     * @param fieldDiff the field diff
     * @param expectedPropertyType the expected property type
     * @param expectedDifferenceType the expected difference type
     * @param expectedLeftValue the expected left value
     * @param expectedRightValue the expected right value
     * @return the property diff
     */
    protected final SimplePropertyDiff checkSimpleFieldDiff(
            PropertyDiff fieldDiff, String expectedPropertyType,
            DifferenceType expectedDifferenceType, String expectedLeftValue,
            String expectedRightValue) {

        assertNotNull("Field diff should not be null", fieldDiff);
        assertTrue("Wrong PropertyDiff implementation",
                fieldDiff instanceof SimplePropertyDiff);

        assertEquals("Wrong property type", expectedPropertyType,
                fieldDiff.getPropertyType());
        SimplePropertyDiff simpleFieldDiff = (SimplePropertyDiff) fieldDiff;
        assertEquals("Wrong difference type", expectedDifferenceType,
                simpleFieldDiff.getDifferenceType());
        assertEquals("Wrong left value", expectedLeftValue,
                simpleFieldDiff.getLeftValue());
        assertEquals("Wrong right value", expectedRightValue,
                simpleFieldDiff.getRightValue());

        return simpleFieldDiff;
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
    protected final SimplePropertyDiff checkSimpleFieldDiff(
            PropertyDiff fieldDiff, String expectedPropertyType,
            String expectedLeftValue, String expectedRightValue) {

        return checkSimpleFieldDiff(fieldDiff, expectedPropertyType,
                DifferenceType.different, expectedLeftValue, expectedRightValue);
    }

    // TODO: should use BlobPropertyDiff?
    protected final SimplePropertyDiff checkContentFieldDiff(
            PropertyDiff fieldDiff) {

        assertNotNull("Field diff should not be null", fieldDiff);
        assertEquals("Wrong property type", PropertyType.CONTENT,
                fieldDiff.getPropertyType());

        assertTrue("Wrong PropertyDiff implementation",
                fieldDiff instanceof ComplexPropertyDiff);
        ComplexPropertyDiff complexFieldDiff = (ComplexPropertyDiff) fieldDiff;

        PropertyDiff dataDiff = complexFieldDiff.getDiff("data");
        assertTrue("Wrong PropertyDiff implementation",
                dataDiff instanceof SimplePropertyDiff);

        SimplePropertyDiff simpleDataDiff = (SimplePropertyDiff) dataDiff;
        assertTrue(!simpleDataDiff.getLeftValue().equals(
                simpleDataDiff.getRightValue()));

        return simpleDataDiff;

    }

    /**
     * Checks a list field diff.
     *
     * @param field the field
     * @param expectedListFieldDiff the expected list field diff
     * @return the property diff
     */
    protected final PropertyDiff checkListFieldDiff(PropertyDiff fieldDiff,
            ListPropertyDiff expectedListFieldDiff) {

        assertNotNull("Field diff should not be null", fieldDiff);
        assertTrue("Wrong PropertyDiff implementation",
                fieldDiff instanceof ListPropertyDiff);

        ListPropertyDiff listFieldDiff = (ListPropertyDiff) fieldDiff;
        assertEquals("Wrong list diff", expectedListFieldDiff, listFieldDiff);

        return listFieldDiff;

    }

    /**
     * Checks a complex field diff.
     *
     * @param field the field
     * @param expectedComplexFieldDiff the expected complex field diff
     * @return the property diff
     */
    protected final PropertyDiff checkComplexFieldDiff(PropertyDiff fieldDiff,
            ComplexPropertyDiff expectedComplexFieldDiff) {

        assertNotNull("Field diff should not be null", fieldDiff);
        assertTrue("Wrong PropertyDiff implementation",
                fieldDiff instanceof ComplexPropertyDiff);

        ComplexPropertyDiff complexFieldDiff = (ComplexPropertyDiff) fieldDiff;
        assertEquals("Wrong complex diff", expectedComplexFieldDiff,
                complexFieldDiff);

        return complexFieldDiff;

    }

}
