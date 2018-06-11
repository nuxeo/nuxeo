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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.ConstraintUtils;
import org.nuxeo.ecm.core.schema.types.constraints.EnumConstraint;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestSchemaLoader extends NXRuntimeTestCase {

    public static final String NS_XSD = "http://www.w3.org/2001/XMLSchema";

    private SchemaManager typeMgr;

    private XSDLoader reader;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        typeMgr = Framework.getLocalService(SchemaManager.class);
        reader = new XSDLoader((SchemaManagerImpl) typeMgr);
    }

    // FIXME: this tests makes too string assumptions on how the fields will be
    // ordered when we iterate over them (fails under Java 6)
    @Test
    @Ignore
    public void testXSDReader() throws Exception {
        URL url = getResource("schema/schema.xsd");

        reader.loadSchema("MySchema", "", url);
        // Collection<Schema> schemas = typeMgr.getSchemas();
        // do not check schemas size - this is dynamic
        // assertEquals(4, schemas.size()); // file, common, MySchema
        Schema schema = typeMgr.getSchema("MySchema");
        assertEquals("MySchema", schema.getName());
        assertEquals("http://www.nuxeo.org/ecm/schemas/MySchema", schema.getNamespace().uri);
        assertEquals("", schema.getNamespace().prefix);

        Collection<Field> fields = schema.getFields();
        assertEquals(5, fields.size());

        Iterator<Field> it = fields.iterator();
        Field field;
        field = it.next();
        assertEquals("title", field.getName().getPrefixedName());
        assertEquals("myString", field.getType().getName());

        field = it.next();
        assertEquals("numericId", field.getName().getPrefixedName());
        assertEquals("long", field.getType().getName());

        field = it.next();
        assertEquals("data", field.getName().getPrefixedName());
        assertEquals("newsml", field.getType().getName());

        field = it.next();
        assertEquals("description", field.getName().getPrefixedName());
        assertEquals("string", field.getType().getName());

        field = it.next();
        assertEquals("person", field.getName().getPrefixedName());
        assertEquals("personInfo", field.getType().getName());
    }

    @Test
    public void testContribs() throws Exception {
        deployContrib("org.nuxeo.ecm.core.schema.tests", "OSGI-INF/CoreTestExtensions.xml");
        DocumentType docType = typeMgr.getDocumentType("myDoc");

        assertNotNull(docType);
        assertEquals(1, docType.getSchemas().size());

        Schema schema = docType.getSchema("schema2");
        assertNotNull(schema);
        assertEquals(2, schema.getFields().size());

        Field field = schema.getField("title");
        assertNotNull(field);
        assertEquals(-1, field.getMaxLength());

        field = schema.getField("description");
        assertNotNull(field);

        CompositeType facet = typeMgr.getFacet("myfacet");
        assertNotNull(facet);
        docType = typeMgr.getDocumentType("myDoc2");
        assertNotNull(docType);
        assertEquals(2, docType.getSchemas().size());
        assertEquals(Arrays.asList("schema1", "schema2"), Arrays.asList(docType.getSchemaNames()));
    }

    @Test
    public void testSequence() throws Exception {
        URL url = getResource("schema/testList.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("testList", "", url);
        Field field = schema.getField("participants");
        ListType type = (ListType) field.getType();
        assertEquals("item", type.getFieldName());
        assertEquals(-1, type.getMaxCount());
        assertEquals(0, type.getMinCount());
        assertEquals("stringSequence", type.getName());

        String[] defaultValue = (String[]) field.getDefaultValue();
        assertEquals(Arrays.asList("titi", "toto", "tata"), Arrays.asList(defaultValue));
    }

    @Test
    public void testList() throws Exception {
        URL url = getResource("schema/testList.xsd");
        assertNotNull(url);

        Schema schema = reader.loadSchema("testList", "", url);
        Field field = schema.getField("strings");
        ListType type = (ListType) field.getType();
        assertEquals("item", type.getFieldName());
        assertEquals(-1, type.getMaxCount());
        assertEquals(0, type.getMinCount());
        assertEquals("stringList", type.getName());

        String[] defaultValue = (String[]) field.getDefaultValue();
        assertEquals(Arrays.asList("titi", "toto", "tata"), Arrays.asList(defaultValue));
    }

    @Test
    public void testComplexSubList() throws Exception {
        URL url = getResource("schema/testList.xsd");
        assertNotNull(url);

        Schema schema = reader.loadSchema("testList", "", url);

        Field field = schema.getField("cplxWithSubList");
        assertTrue(field.getType().isComplexType());

        ComplexType ct = (ComplexType) field.getType();
        assertEquals(4, ct.getFieldsCount());

        Field fieldA = ct.getField("fieldA");
        assertNotNull(fieldA);
        assertEquals("string", fieldA.getType().getName());
        Field fieldB = ct.getField("fieldB");
        assertNotNull(fieldB);
        assertEquals("string", fieldB.getType().getName());

        Field items = ct.getField("items");
        assertNotNull(items);
        assertTrue(items.getType().isListType());

        Field moreitems = ct.getField("moreitems");
        assertNotNull(moreitems);
        assertTrue(moreitems.getType().isListType());

    }

    @Test
    public void testComplexSchema() throws Exception {
        URL url = getResource("schema/policy.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("policy", "", url);

        // test attributes
        Field rule = schema.getField("RULE");
        assertNotNull(rule);
        Field name = ((ComplexType) rule.getType()).getField("name");
        assertNotNull(name);
        assertEquals(name.getType().getName(), StringType.INSTANCE.getName());

        // recursivity

        Field ruleGroup = schema.getField("RULE-GROUP");
        assertNotNull(ruleGroup);

        ComplexType ct = (ComplexType) ruleGroup.getType();
        ruleGroup = ct.getField("RULE-GROUP");
        assertNotNull(ruleGroup);
        assertNotNull(ct.getField("RULE"));
    }

    @Test
    public void testAdvancedTyping() throws Exception {
        URL url = getResource("schema/advancedSchema.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("advancedSchema", "", url);

        Field durField = schema.getField("dur");
        assertEquals("string", durField.getType().getName());

        Field anyField = schema.getField("any");
        assertEquals("string", anyField.getType().getName());

        Field extField = schema.getField("ext");
        Type type = extField.getType();
        assertTrue(type.isComplexType());
    }

    @Test
    public void testUseAttributeAsElements() throws Exception {
        URL url = getResource("schema/advancedSchema.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("advancedSchema", "", url);

        Field el = schema.getField("el");
        assertNotNull(el);
        assertEquals("string", el.getType().getName());

        Field att = schema.getField("att");
        assertNotNull(att);
        assertEquals("string", att.getType().getName());
    }

    @Test
    public void testInlineTyping() throws Exception {
        URL url = getResource("schema/advancedSchema.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("advancedSchema", "", url);

        // check regular definition of simple type with restriction Field
        Field simpleField = schema.getField("bureau");
        Type simpleFieldType = simpleField.getType();
        assertEquals("BureauType", simpleFieldType.getName());
        Type superType = simpleFieldType.getSuperType();
        assertEquals("string", superType.getName());
        assertTrue(simpleFieldType instanceof SimpleTypeImpl);
        SimpleTypeImpl sSimpleFieldType = (SimpleTypeImpl) simpleFieldType;
        Set<Constraint> constraints = sSimpleFieldType.getConstraints();
        assertNotNull(constraints);
        EnumConstraint ec = ConstraintUtils.getConstraint(constraints, EnumConstraint.class);
        assertNotNull(ec);
        assertTrue(ec.getPossibleValues().contains("EFU"));

        // check inline definition of simple type with restriction Field
        Field inlineField = schema.getField("inlineBureau");
        assertNotNull(inlineField);
        Type inlineFieldType = inlineField.getType();
        assertNotNull(inlineFieldType);
        superType = inlineFieldType.getSuperType();
        assertEquals("string", superType.getName());
        assertTrue(inlineFieldType instanceof SimpleTypeImpl);
        SimpleTypeImpl sInlineFieldType = (SimpleTypeImpl) inlineFieldType;
        constraints = sInlineFieldType.getConstraints();
        assertNotNull(constraints);
        ec = ConstraintUtils.getConstraint(constraints, EnumConstraint.class);
        assertNotNull(ec);
        assertTrue(ec.getPossibleValues().contains("EFU"));

    }

    @Test
    public void testXSChoice() throws Exception {
        URL url = getResource("schema/advancedSchema.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("advancedSchema", "", url);

        Field choiceField = schema.getField("testChoice");
        assertNotNull(choiceField);
        Type choiceFieldType = choiceField.getType();
        assertTrue(choiceFieldType.isComplexType());

        ComplexType ct = (ComplexType) choiceFieldType;

        assertEquals(4, ct.getFieldsCount());

        assertNotNull(ct.getField("field1"));
        assertNotNull(ct.getField("field2A"));
        assertNotNull(ct.getField("field2B"));
        assertNotNull(ct.getField("field3"));
    }

    @Test
    public void testXSNestedChoice() throws Exception {
        URL url = getResource("schema/advancedSchema.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("advancedSchema", "", url);

        Field choiceField = schema.getField("testNestedChoices");
        assertNotNull(choiceField);
        Type choiceFieldType = choiceField.getType();
        assertTrue(choiceFieldType.isComplexType());

        ComplexType ct = (ComplexType) choiceFieldType;

        assertEquals(5, ct.getFieldsCount());

        assertNotNull(ct.getField("field1"));
        assertNotNull(ct.getField("field2A"));
        assertNotNull(ct.getField("field2B"));
        assertNotNull(ct.getField("field3A"));
        assertNotNull(ct.getField("field3B"));
    }

    @Test
    public void testXSNestedChoiceWithList() throws Exception {
        URL url = getResource("schema/advancedSchema.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("advancedSchema", "", url);

        Field choiceField = schema.getField("testNestedChoicesWithList");
        assertNotNull(choiceField);
        Type choiceFieldType = choiceField.getType();
        assertTrue(choiceFieldType.isComplexType());

        ComplexType ct = (ComplexType) choiceFieldType;

        assertEquals(4, ct.getFieldsCount());

        assertNotNull(ct.getField("field1"));
        assertEquals("string", ct.getField("field1").getType().getName());
        assertNotNull(ct.getField("field2A"));
        assertEquals("string", ct.getField("field2A").getType().getName());
        assertNotNull(ct.getField("field2B"));
        assertEquals("string", ct.getField("field2B").getType().getName());
        assertNotNull(ct.getField("TestNestedChoicesWithListType#anonymousList"));
        assertEquals("TestNestedChoicesWithListType#anonymousListType",
                ct.getField("TestNestedChoicesWithListType#anonymousList").getType().getName());

        Field listField = ct.getField("TestNestedChoicesWithListType#anonymousList");
        assertTrue(listField.getType().isListType());

        ListType listType = (ListType) listField.getType();

        Field listItemField = listType.getField();
        assertEquals("TestNestedChoicesWithListType#anonymousListItem", listItemField.getType().getName());

        assertTrue(listItemField.getType().isComplexType());

        // check 2 subfields
        ComplexType ctitem = (ComplexType) listItemField.getType();
        Field field3A = ctitem.getField("field3A");
        assertNotNull(field3A);
        Field field3B = ctitem.getField("field3B");
        assertNotNull(field3B);
    }

    @Test
    public void testSchema12() throws Exception {

        URL url = getResource("schema/schema12.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("schema12", "", url);

        Field field1 = schema.getField("field1");
        assertNotNull(field1);

        assertTrue(field1.getType().isComplexType());

        ComplexType ct = (ComplexType) field1.getType();
        assertEquals(2, ct.getFieldsCount());

        assertTrue(ct.hasField("label"));
        assertTrue(ct.hasField("roles"));

        assertTrue(ct.getField("roles").getType().isListType());
        assertFalse(ct.getField("label").getType().isListType());

        Field field2 = schema.getField("self");
        assertNotNull(field2);
        assertEquals("string", field2.getType().getName());

    }

    @Test
    public void testClassifier() throws Exception {

        URL url = getResource("schema/test-classifier.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("classifier", "", url);

        assertNotNull(schema);

        Field field1 = schema.getField("integerBins");
        assertNotNull(field1);

        Field field2 = schema.getField("stringBins");
        assertNotNull(field2);

        // integerBins
        assertTrue(field1.getType().isListType());
        ListType ltype = (ListType) field1.getType();

        Type integerBinType = ltype.getField().getType();

        assertTrue(integerBinType.isComplexType());
        assertEquals("integerBin", integerBinType.getName());

        Field matchingValuesField = ((ComplexType) integerBinType).getField("matchingValues");
        assertNotNull(matchingValuesField);

        assertEquals("integerValues", matchingValuesField.getType().getName());

        assertTrue(matchingValuesField.getType().isListType());

        ListType integerValuesType = (ListType) matchingValuesField.getType();

        assertTrue(integerValuesType.getField().getType().isSimpleType());
        assertEquals("long", integerValuesType.getField().getType().getName());

    }

    @Test
    public void testXSExtension() throws Exception {

        URL url = getResource("schema/testExtension.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("extension", "", url);

        assertNotNull(schema);

        Field field = schema.getField("employee");
        assertNotNull(field);

        assertTrue(field.getType().isComplexType());

        ComplexType ct = (ComplexType) field.getType();

        // additional fields
        assertTrue(ct.hasField("address"));
        assertTrue(ct.hasField("city"));
        assertTrue(ct.hasField("country"));

        // inherited fields
        assertTrue(ct.hasField("firstname"));
        assertTrue(ct.hasField("lastname"));

        // super parent inherited fields
        assertTrue(ct.hasField("race"));

        assertEquals(6, ct.getFieldsCount());
    }

    @Test
    public void testXSDRebase() throws Exception {

        URL url = getResource("schema/testExtension.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("extension", "prefix", url, "employee");

        assertNotNull(schema);

        // additional fields
        assertTrue(schema.hasField("address"));
        assertTrue(schema.hasField("city"));
        assertTrue(schema.hasField("country"));

        // inherited fields
        assertTrue(schema.hasField("firstname"));
        assertTrue(schema.hasField("lastname"));

        // super parent inherited fields
        assertTrue(schema.hasField("race"));

        assertEquals(6, schema.getFieldsCount());

        // ensure the field are rebased
        Field address = schema.getField("address");
        assertEquals("prefix:address", address.getName().getPrefixedName());
        assertEquals("extension", address.getDeclaringType().getName());
    }

    /**
     * NXP-24660
     */
    @Test
    public void testComplexWithSameInnerField() throws Exception {

        URL url = getResource("schema/testComplexWithSameInnerField.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("extension", "prefix", url, "employee");

        assertNotNull(schema);

        // check complex fields
        assertTrue(schema.hasField("complexWithString"));
        assertTrue(schema.hasField("complexWithInteger"));

        // check complex types
        checkComplexTypes(schema, "t_complexWithString", StringType.class);
        checkComplexTypes(schema, "t_complexWithInteger", LongType.class);
    }

    private void checkComplexTypes(Schema schema, String typeName, Class<? extends Type> expectedInnerType) {
        Type type = schema.getType(typeName);
        assertNotNull(type);
        assertTrue(type.isComplexType());
        ComplexType complexType = (ComplexType) type;
        assertEquals(1, complexType.getFieldsCount());

        // check inner field/type
        Field valueField = complexType.getFields().iterator().next();
        assertNotNull(valueField);
        Type valueType = valueField.getType();
        assertTrue(typeName + " inner value is a " + valueType.getSuperType().getClass(),
                expectedInnerType.isInstance(valueType.getSuperType()));
    }

    /**
     * NXP-24660
     */
    @Test
    public void testListWithSameInnerField() throws Exception {

        URL url = getResource("schema/testListWithSameInnerField.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("extension", "prefix", url, "employee");

        assertNotNull(schema);

        // check complex fields
        assertTrue(schema.hasField("listWithString"));
        assertTrue(schema.hasField("listWithInteger"));

        // check complex types
        checkListTypes(schema, "t_listWithString", StringType.class);
        checkListTypes(schema, "t_listWithInteger", LongType.class);
    }

    private void checkListTypes(Schema schema, String typeName, Class<? extends Type> expectedInnerType) {
        Type type = schema.getType(typeName);
        assertNotNull(type);
        assertTrue(type.isListType());
        ListType listType = (ListType) type;

        // check inner field/type
        Field valueField = listType.getField();
        assertNotNull(valueField);
        Type valueType = valueField.getType();
        assertTrue(typeName + " inner value is a " + valueType.getSuperType().getClass(),
                expectedInnerType.isInstance(valueType.getSuperType()));
    }

    /**
     * NXP-24660
     */
    @Test
    public void testComplexWithSameInnerListAndField() throws Exception {

        URL url = getResource("schema/testComplexWithSameInnerListAndField.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("extension", "prefix", url, "employee");

        assertNotNull(schema);

        // check complex fields
        assertTrue(schema.hasField("complexWithString"));
        assertTrue(schema.hasField("complexWithInteger"));

        // check complex types
        checkComplexWithListAndFieldTypes(schema, "t_complexWithString", StringType.class);
        checkComplexWithListAndFieldTypes(schema, "t_complexWithInteger", LongType.class);
    }

    private void checkComplexWithListAndFieldTypes(Schema schema, String typeName,
            Class<? extends Type> expectedInnerType) {
        Type type = schema.getType(typeName);
        assertNotNull(type);
        assertTrue(type.isComplexType());
        ComplexType complexType = (ComplexType) type;
        assertEquals(2, complexType.getFieldsCount());

        List<Field> fields = new ArrayList<>(complexType.getFields());
        fields.sort(Comparator.comparing(f -> f.getName().getPrefixedName()));

        // check inner field/type
        Field listField = fields.get(0);
        assertNotNull(listField);
        Type listType = listField.getType();
        assertTrue(listType instanceof ListType);
        Type listItemType = ((ListType) listType).getFieldType();
        assertTrue(typeName + " inner value is a " + listItemType.getSuperType().getClass(),
                expectedInnerType.isInstance(listItemType.getSuperType()));

        Field valueField = fields.get(1);
        assertNotNull(valueField);
        Type valueType = valueField.getType();
        assertTrue(typeName + " inner value is a " + valueType.getSuperType().getClass(),
                expectedInnerType.isInstance(valueType.getSuperType()));
    }

}
