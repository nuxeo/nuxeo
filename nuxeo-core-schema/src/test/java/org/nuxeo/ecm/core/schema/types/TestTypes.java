/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.schema.types;

import java.util.Calendar;
import java.util.Date;

import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.SchemaNames;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestTypes extends NXRuntimeTestCase {


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib("nuxeo-core-schema", "OSGI-INF/SchemaService.xml");
    }


    // ANY type

    public void testAnyType() throws TypeException {
        Type type = AnyType.INSTANCE;

        assertEquals(AnyType.ID, type.getName());
        assertTrue(type.isAnyType());
        assertFalse(type.isSimpleType());
        //assertFalse(type.isPrimitive());
        assertFalse(type.isComplexType());

        assertNull(type.getSuperType());
        assertEquals(0, type.getTypeHierarchy().length);

        // ANY validates anything
        assertTrue(type.validate(0));
        assertTrue(type.validate(""));
        assertTrue(type.validate(true));
    }

    // Primitive types

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

        //TODO: test convert method
    }

    @SuppressWarnings({"SimplifiableJUnitAssertion", "AssertEqualsBetweenInconvertibleTypes"})
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
        assertEquals(true, type.convert(true));
        assertEquals(false, type.convert(false));
        assertEquals(true, type.convert("true"));
        assertEquals(false, type.convert("false"));
        assertEquals(true, type.convert(1));
        assertEquals(false, type.convert(0));
        assertEquals(true, type.decode("true"));
    }

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
        assertEquals(0, type.convert(0));
        assertEquals(0, type.convert("0"));
        assertEquals(0, type.convert(0.5));
        assertNull(type.convert("abc"));
        assertEquals(0, type.decode("0"));
    }

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
        assertEquals(0.0, type.convert(0));
        assertEquals(0.5, type.convert(0.5));
        assertEquals(3.14, type.convert("3.14"));
        assertNull(type.convert("abc"));
        assertEquals(0.0, type.decode("0.0"));
        assertEquals(3.14, type.decode("3.14"));
    }

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
        assertEquals(0L, type.convert(0));
        assertEquals(0L, type.convert("0"));
        assertEquals(0L, type.convert(0.5));
        assertNull(type.convert("abc"));
        assertEquals(0L, type.decode("0"));
    }

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
        //TODO (after the DateType class is fully implemented)
    }

    // Custom types

    public void testListType() throws TypeException {
        ListType type = new ListTypeImpl(SchemaNames.BUILTIN,  "list type",  AnyType.INSTANCE);

        assertTrue(type.isListType());
        assertEquals("list type", type.getName());
        assertFalse(type.isCompositeType());
        assertFalse(type.isSimpleType());
        assertFalse(type.isComplexType());
        assertFalse(type.isAnyType());
        assertNull(type.getSuperType());
        assertEquals(Type.ANY, type.getFieldType());

        // Validation tests
        assertTrue(type.validate(new Integer[3]));
        assertFalse(type.isNotNull());
        assertTrue(type.validate(null));
        assertFalse(type.validate(0));
        assertFalse(type.validate(""));
        assertFalse(type.validate(true));

        //TODO: add tests for collections once this is implemented
    }

    public void testCompositeType() {
        CompositeTypeImpl type = new CompositeTypeImpl((CompositeType)null, SchemaNames.BUILTIN,  "composite type", null);

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

    public void testSchema() {
        String name = "name";
        String uri = "uri";
        String prefix = "prefix";
        Schema schema = new SchemaImpl(name, new Namespace(uri, prefix));

        assertEquals(schema.getName(), name);
        assertEquals(schema.getNamespace().uri, uri);
        assertEquals(schema.getNamespace().prefix, prefix);
    }

}
