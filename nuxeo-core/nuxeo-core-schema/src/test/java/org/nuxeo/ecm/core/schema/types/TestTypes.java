/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.schema.types;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.SchemaNames;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestTypes extends NXRuntimeTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
    }

    // ANY type
    @Test
    public void testAnyType() throws TypeException {
        Type type = AnyType.INSTANCE;

        assertEquals(AnyType.ID, type.getName());
        assertTrue(type.isAnyType());
        assertFalse(type.isSimpleType());
        // assertFalse(type.isPrimitive());
        assertFalse(type.isComplexType());

        assertNull(type.getSuperType());
        assertEquals(0, type.getTypeHierarchy().length);

        // ANY validates anything
        assertTrue(type.validate(0));
        assertTrue(type.validate(""));
        assertTrue(type.validate(true));
    }

    // Primitive types
    @Test
    public void testStringType() throws TypeException {
        SimpleType type = StringType.INSTANCE;

        assertEquals("string", type.getName());
        assertTrue(type.isSimpleType());
        assertTrue(type.isPrimitive());
        assertFalse(type.isComplexType());
        assertFalse(type.isAnyType());

        assertNull(type.getSuperType());
        assertEquals(0, type.getTypeHierarchy().length);

        assertTrue(type.validate(0));
        assertTrue(type.validate(""));
        assertTrue(type.validate(true));

        // TODO: test convert method
    }

    @SuppressWarnings({ "SimplifiableJUnitAssertion", "AssertEqualsBetweenInconvertibleTypes" })
    @Test
    public void testBooleanType() throws TypeException {
        SimpleType type = BooleanType.INSTANCE;

        assertEquals("boolean", type.getName());
        assertTrue(type.isSimpleType());
        assertTrue(type.isPrimitive());
        assertFalse(type.isComplexType());
        assertFalse(type.isAnyType());

        assertNull(type.getSuperType());
        assertEquals(0, type.getTypeHierarchy().length);

        // Validation tests
        assertFalse(type.validate(0));
        assertFalse(type.validate(""));
        assertTrue(type.validate(true));
        assertTrue(type.validate(false));

        // Conversion tests
        assertNull(type.decode(""));
        assertEquals(true, type.convert(true));
        assertEquals(false, type.convert(false));
        assertEquals(true, type.convert("true"));
        assertEquals(false, type.convert("false"));
        assertEquals(true, type.convert(1));
        assertEquals(false, type.convert(0));
        assertEquals(true, type.decode("true"));
    }

    @Test
    public void testIntegerType() throws TypeException {
        SimpleType type = IntegerType.INSTANCE;

        assertEquals("integer", type.getName());
        assertTrue(type.isSimpleType());
        assertTrue(type.isPrimitive());
        assertFalse(type.isComplexType());
        assertFalse(type.isAnyType());

        assertNull(type.getSuperType());
        assertEquals(0, type.getTypeHierarchy().length);

        // Validation tests
        assertTrue(type.validate(0));
        assertFalse(type.validate(""));
        assertFalse(type.validate(true));

        // Conversion tests
        assertNull(type.decode(""));
        assertEquals(0, type.convert(0));
        assertEquals(0, type.convert("0"));
        assertEquals(0, type.convert(0.5));
        assertNull(type.convert("abc"));
        assertEquals(0, type.decode("0"));
    }

    @Test
    public void testDoubleType() throws TypeException {
        SimpleType type = DoubleType.INSTANCE;

        assertEquals("double", type.getName());
        assertTrue(type.isSimpleType());
        assertTrue(type.isPrimitive());
        assertFalse(type.isComplexType());
        assertFalse(type.isAnyType());

        assertNull(type.getSuperType());
        assertEquals(0, type.getTypeHierarchy().length);

        // Validation tests
        assertTrue(type.validate(0));
        assertTrue(type.validate(0.0));
        assertFalse(type.validate(true));
        assertFalse(type.validate(""));

        // Conversion tests
        assertNull(type.decode(""));
        assertEquals(0.0, type.convert(0));
        assertEquals(0.5, type.convert(0.5));
        assertEquals(3.14, type.convert("3.14"));
        assertNull(type.convert("abc"));
        assertEquals(0.0, type.decode("0.0"));
        assertEquals(3.14, type.decode("3.14"));
    }

    @Test
    public void testLongType() throws TypeException {
        SimpleType type = LongType.INSTANCE;

        assertEquals("long", type.getName());
        assertTrue(type.isSimpleType());
        assertTrue(type.isPrimitive());
        assertFalse(type.isComplexType());
        assertFalse(type.isAnyType());

        assertNull(type.getSuperType());
        assertEquals(0, type.getTypeHierarchy().length);

        // Validation tests
        assertTrue(type.validate(0));
        assertFalse(type.validate(""));
        assertFalse(type.validate(true));

        // Conversion tests
        assertNull(type.decode(""));
        assertEquals(0L, type.convert(0));
        assertEquals(0L, type.convert("0"));
        assertEquals(0L, type.convert(0.5));
        assertNull(type.convert("abc"));
        assertEquals(0L, type.decode("0"));
    }

    @Test
    public void testDateType() throws TypeException {
        SimpleType type = DateType.INSTANCE;

        assertEquals("date", type.getName());
        assertTrue(type.isSimpleType());
        assertTrue(type.isPrimitive());
        assertFalse(type.isComplexType());
        assertFalse(type.isAnyType());

        assertNull(type.getSuperType());
        assertEquals(0, type.getTypeHierarchy().length);

        // Validation tests
        assertTrue(type.validate(new Date()));
        assertTrue(type.validate(Calendar.getInstance()));

        // Conversion tests
        assertNull(type.decode(""));
        // TODO (after the DateType class is fully implemented)
    }

    // Custom types
    @Test
    public void testListType() throws TypeException {
        ListType type = new ListTypeImpl(SchemaNames.BUILTIN, "list type", StringType.INSTANCE);

        assertTrue(type.isListType());
        assertEquals("list type", type.getName());
        assertFalse(type.isCompositeType());
        assertFalse(type.isSimpleType());
        assertFalse(type.isComplexType());
        assertFalse(type.isAnyType());
        assertNull(type.getSuperType());
        assertEquals(StringType.INSTANCE, type.getFieldType());

        // Validation tests
        assertTrue(type.validate(new Integer[3]));
        assertTrue(type.validate(null));
        assertFalse(type.validate(0));
        assertFalse(type.validate(""));
        assertFalse(type.validate(true));

        // Conversion tests
        assertNull(type.decode(""));
        Object[] array = (Object[]) type.decode("foo bar");
        assertEquals(Arrays.asList("foo", "bar"), Arrays.asList(array));

        // TODO: add tests for collections once this is implemented
    }

    @Test
    public void testCompositeType() {
        CompositeTypeImpl type = new CompositeTypeImpl((CompositeType) null, SchemaNames.BUILTIN, "composite type",
                null);

        assertTrue(type.isCompositeType());
        assertEquals("composite type", type.getName());
        assertFalse(type.isSimpleType());
        // XXX: Not is CompositeType API. Why?
        // assertFalse(type.isPrimitive());
        assertFalse(type.isComplexType());
        assertFalse(type.isAnyType());
        assertNull(type.getSuperType());

        // Author schemas API
        assertFalse(type.hasSchemas());
        assertFalse(type.hasSchema("inexisting schema"));
        assertTrue(type.getSchemas().isEmpty());

        // Author fields API
        assertTrue(type.getFields().isEmpty());
    }

    @Test
    public void testSchema() {
        String name = "name";
        String uri = "uri";
        String prefix = "prefix";
        Schema schema = new SchemaImpl(name, new Namespace(uri, prefix));

        assertEquals(schema.getName(), name);
        assertEquals(schema.getNamespace().uri, uri);
        assertEquals(schema.getNamespace().prefix, prefix);
    }

    @Test
    public void testFieldFromXpath() throws Exception {
        deployTestContrib("org.nuxeo.ecm.core.schema", "OSGI-INF/test-advanced-schema.xml");
        SchemaManager sm = Framework.getService(SchemaManager.class);
        assertNotNull(sm);
        Field field = sm.getField("tp:foo");
        assertNull(field);
        field = sm.getField("dc:title");
        assertEquals("title", field.getName().getLocalName());
        assertEquals("string", field.getType().getName());
        field = sm.getField("dc:contributors");
        assertEquals("contributors", field.getName().getLocalName());
        assertEquals("contributorList", field.getType().getName());
        field = sm.getField("tp:stringArray");
        assertEquals("stringArray", field.getName().getLocalName());
        assertEquals("stringArrayType", field.getType().getName());
        field = sm.getField("tp:stringArray/*");
        assertEquals("item", field.getName().getLocalName());
        assertEquals("string", field.getType().getName());
        field = sm.getField("tp:stringArray/1");
        assertEquals("item", field.getName().getLocalName());
        assertEquals("string", field.getType().getName());
        field = sm.getField("tp:complexChain");
        assertEquals("complexChain", field.getName().getLocalName());
        assertEquals("complexChain", field.getType().getName());
        field = sm.getField("tp:complexChain/stringItem");
        assertEquals("stringItem", field.getName().getLocalName());
        assertEquals("string", field.getType().getName());
        field = sm.getField("tp:complexChain/complexItem");
        assertEquals("complexItem", field.getName().getLocalName());
        assertEquals("complexType", field.getType().getName());
        field = sm.getField("tp:complexList/*/stringItem");
        assertEquals("stringItem", field.getName().getLocalName());
        assertEquals("string", field.getType().getName());
        field = sm.getField("tp:complexList/0/stringItem");
        assertEquals("stringItem", field.getName().getLocalName());
        assertEquals("string", field.getType().getName());

        field = sm.getField("tp:listOfLists");
        assertEquals("listOfLists", field.getName().getLocalName());
        assertEquals("listOfListsType", field.getType().getName());
        field = sm.getField("tp:listOfLists/*");
        assertEquals("listOfListsItem", field.getName().getLocalName());
        assertEquals("listOfListsItemType", field.getType().getName());
        field = sm.getField("tp:listOfLists/*/stringItem");
        assertEquals("stringItem", field.getName().getLocalName());
        assertEquals("string", field.getType().getName());
        // XXX maybe this one should resolve to null (invalid item index in xpath) (?)
        field = sm.getField("tp:listOfLists/stringItem");
        assertEquals("stringItem", field.getName().getLocalName());
        assertEquals("string", field.getType().getName());
        field = sm.getField("tp:listOfLists/*/stringListItem/*");
        assertEquals("item", field.getName().getLocalName());
        assertEquals("string", field.getType().getName());
        field = sm.getField("tp:listOfLists/*/stringListItem/0");
        assertEquals("item", field.getName().getLocalName());
        assertEquals("string", field.getType().getName());
        // XXX maybe this one should resolve to null (invalid item index in xpath) (?)
        field = sm.getField("tp:listOfLists/stringListItem");
        assertEquals("stringListItem", field.getName().getLocalName());
        assertEquals("stringList", field.getType().getName());
    }

    @Test
    public void testSchemaFromType() throws Exception {
        deployTestContrib("org.nuxeo.ecm.core.schema", "OSGI-INF/test-advanced-schema.xml");
        Schema schema = getSchema("foo");
        assertNull(schema);
        schema = getSchema("dc:title");
        assertEquals("dublincore", schema.getName());
        schema = getSchema("dc:contributors");
        assertEquals("dublincore", schema.getName());
        schema = getSchema("tp:stringArray");
        assertEquals("testProperties", schema.getName());
        schema = getSchema("tp:complexChain");
        assertEquals("testProperties", schema.getName());
        schema = getSchema("tp:complexChain/stringItem");
        assertEquals("testProperties", schema.getName());
        schema = getSchema("tp:complexChain/complexItem");
        assertEquals("testProperties", schema.getName());
        schema = getSchema("tp:complexList/*/stringItem");
        assertEquals("testProperties", schema.getName());
        schema = getSchema("tp:complexList/0/stringItem");
        assertEquals("testProperties", schema.getName());

        schema = getSchema("tp:listOfLists");
        assertEquals("testProperties", schema.getName());
        schema = getSchema("tp:listOfLists/*");
        assertEquals("testProperties", schema.getName());
        schema = getSchema("tp:listOfLists/*/stringItem");
        assertEquals("testProperties", schema.getName());
        // XXX maybe this one should resolve to null (invalid item index in xpath) (?)
        schema = getSchema("tp:listOfLists/stringItem");
        assertEquals("testProperties", schema.getName());
        schema = getSchema("tp:listOfLists/*/stringListItem/*");
        assertEquals("testProperties", schema.getName());
        schema = getSchema("tp:listOfLists/*/stringListItem/0");
        assertEquals("testProperties", schema.getName());
        // XXX maybe this one should resolve to null (invalid item index in xpath) (?)
        schema = getSchema("tp:listOfLists/stringListItem");
        assertEquals("testProperties", schema.getName());
    }

    protected Schema getSchema(String xpath) {
        SchemaManager sm = Framework.getService(SchemaManager.class);
        assertNotNull(sm);
        Field field = sm.getField(xpath);
        if (field != null) {
            return field.getDeclaringType().getSchema();
        }
        return null;
    }

}
