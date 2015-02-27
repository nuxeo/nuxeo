/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.DateIntervalConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.EnumConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.LengthConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.NotNullConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.NumericIntervalConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.PatternConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.TypeConstraint;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestSchemaLoaderRestriction extends NXRuntimeTestCase {

    public static final String NS_XSD = "http://www.w3.org/2001/XMLSchema";

    private Schema schema;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        SchemaManager typeMgr = Framework.getLocalService(SchemaManager.class);
        XSDLoader reader = new XSDLoader((SchemaManagerImpl) typeMgr);
        URL url = getResource("schema/testrestriction.xsd");
        schema = reader.loadSchema("testrestriction", "", url);
    }

    @Test
    public void testBinaryRestrictions() {
        Field field = schema.getField("binaryConstraints");
        assertNotNull(field);
        Set<Constraint> constraints = field.getConstraints();
        assertEquals(2, constraints.size());
        assertTrue(constraints.contains(new TypeConstraint(BinaryType.INSTANCE)));
        assertTrue(constraints.contains(NotNullConstraint.get()));
    }

    @Test
    public void testBooleanRestrictions() {
        Field field = schema.getField("booleanConstraints");
        assertNotNull(field);
        Set<Constraint> constraints = field.getConstraints();
        assertEquals(2, constraints.size());
        assertTrue(constraints.contains(new TypeConstraint(BooleanType.INSTANCE)));
        assertTrue(constraints.contains(NotNullConstraint.get()));
    }

    @Test
    public void testStringRestrictions() {
        Field field = schema.getField("stringConstraints");
        assertNotNull(field);
        Set<Constraint> constraints = field.getConstraints();
        assertEquals(5, constraints.size());
        assertTrue(constraints.contains(NotNullConstraint.get()));
        assertTrue(constraints.contains(new TypeConstraint(StringType.INSTANCE)));
        assertTrue(constraints.contains(new PatternConstraint("[^3]*")));
        assertTrue(constraints.contains(new LengthConstraint(2, 4)));
        assertTrue(constraints.contains(new EnumConstraint(Arrays.asList("1", "1234", "22", "333", "4444", "55555"))));
    }

    @Test
    public void testNumericRestrictions() {
        Field field = schema.getField("decimalConstraints");
        assertNotNull(field);
        Set<Constraint> constraints = field.getConstraints();
        assertEquals(5, constraints.size());
        assertTrue(constraints.contains(new TypeConstraint(DoubleType.INSTANCE)));
        assertTrue(constraints.contains(NotNullConstraint.get()));
        assertTrue(constraints.contains(new PatternConstraint("[^6]*")));
        assertTrue(constraints.contains(new EnumConstraint(Arrays.asList("2014.15", "2015.1555", "2016.15", "2017.15",
                "2017.15555", "2017.155555", "2018.15"))));
        assertTrue(constraints.contains(new NumericIntervalConstraint(2015.001d, true, 2017.999d, true)));
    }

    @Test
    public void testDateRestrictions() {
        Field field = schema.getField("dateConstraints");
        assertNotNull(field);
        Set<Constraint> constraints = field.getConstraints();
        assertEquals(3, constraints.size());
        assertTrue(constraints.contains(new TypeConstraint(DateType.INSTANCE)));
        assertTrue(constraints.contains(NotNullConstraint.get()));
        assertTrue(constraints.contains(new DateIntervalConstraint(new GregorianCalendar(2015, 0, 1), true,
                new GregorianCalendar(2016, 11, 31), true)));
    }

    @Test
    public void testMaxLength() {
        Field field = schema.getField("stringConstraints");
        assertEquals(4, field.getMaxLength());
    }

    @Test
    public void testElementNillable() {
        doTestNillableCase("elementShouldNotBeNullButCanBeNull1", true);
        doTestNillableCase("elementShouldNotBeNullButCanBeNull2", true);
        doTestNillableCase("elementShouldNotBeNullButCanBeNull3", true);
        doTestNillableCase("elementCanBeNull1", true);
        doTestNillableCase("elementCanBeNull2", true);
        doTestNillableCase("elementCanBeNull3", true);
        doTestNillableCase("elementCanBeNull4", true);
        doTestNillableCase("elementCannotBeNull1", false);
        doTestNillableCase("elementCannotBeNull2", false);
    }

    @Test
    public void testAttributeRestriction() {
        Field field = schema.getField("attributeWithConstraints");
        Field attr = ((ComplexType) field.getType()).getField("attr");
        assertNotNull(attr);
        Set<Constraint> constraints = attr.getConstraints();
        assertEquals(5, constraints.size());
        assertTrue(constraints.contains(new TypeConstraint(StringType.INSTANCE)));
        assertTrue(constraints.contains(NotNullConstraint.get()));
        assertTrue(constraints.contains(new PatternConstraint("[^3]*")));
        assertTrue(constraints.contains(new LengthConstraint(2, 4)));
        assertTrue(constraints.contains(new EnumConstraint(Arrays.asList("1", "1234", "22", "333", "4444", "55555"))));
    }

    @Test
    public void testComplexTypeAttributeNillable() {
        doTestNillableCaseOnComplexTypeAttribute("attributeCanBeNull1", true);
        doTestNillableCaseOnComplexTypeAttribute("attributeCanBeNull2", true);
        doTestNillableCaseOnComplexTypeAttribute("attributeCannotBeNull1", false);
    }

    @Test
    public void testSchemaAttributeRestriction() {
        Field attr = schema.getField("schemaAttributeWithConstraints");
        assertNotNull(attr);
        Set<Constraint> constraints = attr.getConstraints();
        assertEquals(4, constraints.size());
        assertTrue(constraints.contains(new TypeConstraint(StringType.INSTANCE)));
        assertTrue(constraints.contains(new PatternConstraint("[^3]*")));
        assertTrue(constraints.contains(new LengthConstraint(2, 4)));
        assertTrue(constraints.contains(new EnumConstraint(Arrays.asList("1", "1234", "22", "333", "4444", "55555"))));
    }

    @Test
    public void testSchemaAttributeNillable() {
        doTestNillableCase("schemaAttributeCanBeNull1", true);
    }

    private void doTestNillableCase(String name, boolean expected) {
        Field field = schema.getField(name);
        assertEquals(!expected, field.getConstraints().contains(NotNullConstraint.get()));
        assertEquals(expected, field.isNillable());
    }

    private void doTestNillableCaseOnComplexTypeAttribute(String name, boolean expected) {
        Field field = schema.getField(name);
        Field attr = ((ComplexType) field.getType()).getField("attr");
        assertEquals(!expected, attr.getConstraints().contains(NotNullConstraint.get()));
        assertEquals(expected, attr.isNillable());
    }

    @Test
    public void testSimpleListWithConstraints() {
        Field subList = schema.getField("simpleListWithConstraints");
        assertNotNull(subList);
        Type type = ((ListType) subList.getType()).getFieldType();
        Set<Constraint> constraints = type.getConstraints();
        assertEquals(2, constraints.size());
    }

    @Test
    public void testListOfListRestriction() {
        ListType listType = (ListType) schema.getField("listOfLists").getType();
        ComplexType itemType = (ComplexType) listType.getFieldType();
        ListType subListType = (ListType) itemType.getField("stringListItem").getType();
        Set<Constraint> constraints = subListType.getFieldType().getConstraints();
        assertEquals(2, constraints.size());
    }

}
