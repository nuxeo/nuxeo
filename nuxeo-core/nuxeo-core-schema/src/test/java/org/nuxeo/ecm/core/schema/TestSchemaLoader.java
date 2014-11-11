/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.schema;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Constraint;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.constraints.EnumConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.StringLengthConstraint;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestSchemaLoader extends NXRuntimeTestCase {

    public static final String NS_XSD = "http://www.w3.org/2001/XMLSchema";

    private SchemaManagerImpl typeMgr;

    private XSDLoader reader;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        typeMgr = getTypeManager();
        reader = new XSDLoader(typeMgr);
    }

    public static TypeService getTypeService() {
        return (TypeService) Framework.getRuntime().getComponent(
                TypeService.NAME);
    }

    public static SchemaManagerImpl getTypeManager() {
        return (SchemaManagerImpl) getTypeService().getTypeManager();
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
        assertEquals("http://www.nuxeo.org/ecm/schemas/MySchema",
                schema.getNamespace().uri);
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
        deployContrib("org.nuxeo.ecm.core.schema.tests",
                "CoreTestExtensions.xml");
        Framework.getLocalService(SchemaManager.class).flushPendingsRegistration();
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
        assertEquals(Arrays.asList("schema1", "schema2"),
                Arrays.asList(docType.getSchemaNames()));
    }

    @SuppressWarnings("unchecked")
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

        List<String> defaultValue = (List<String>) field.getDefaultValue();
        assertEquals(3, defaultValue.size());
        assertEquals("titi", defaultValue.get(0));
        assertEquals("toto", defaultValue.get(1));
        assertEquals("tata", defaultValue.get(2));
    }

    @SuppressWarnings("unchecked")
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

        List<String> defaultValue = (List<String>) field.getDefaultValue();
        assertEquals(3, defaultValue.size());
        assertEquals("titi", defaultValue.get(0));
        assertEquals("toto", defaultValue.get(1));
        assertEquals("tata", defaultValue.get(2));
    }

    @SuppressWarnings("unchecked")
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
    public void testRestriction() throws Exception {
        URL url = getResource("schema/testrestriction.xsd");
        assertNotNull(url);
        Schema schema = reader.loadSchema("testrestriction", "", url);
        Field field = schema.getField("shortstring");
        assertEquals(50, field.getMaxLength());

        Type type = field.getType();
        assertTrue(type instanceof SimpleTypeImpl);
        SimpleTypeImpl t = (SimpleTypeImpl) type;
        Type st = t.getSuperType();
        assertEquals(st.getName(), StringType.INSTANCE.getName());
        Constraint[] constraints = t.getConstraints();
        assertNotNull(constraints);
        Constraint c = constraints[0];
        assertTrue(c instanceof StringLengthConstraint);
        StringLengthConstraint slc = (StringLengthConstraint) c;
        assertEquals(0, slc.getMin());
        assertEquals(50, slc.getMax());

        Field genderField = schema.getField("gender");
        Type genderType = genderField.getType();
        assertEquals("Gender", genderType.getName());
        Type superType = genderType.getSuperType();
        assertEquals("string", superType.getName());
        assertTrue(genderType instanceof SimpleTypeImpl);
        SimpleTypeImpl sGenderType = (SimpleTypeImpl) genderType;

        constraints = sGenderType.getConstraints();
        assertNotNull(constraints);
        Constraint enumConstraint = constraints[0];
        assertTrue(enumConstraint instanceof EnumConstraint);
        EnumConstraint ec = (EnumConstraint) enumConstraint;
        assertTrue(ec.getPossibleValues().contains("Male"));
        assertTrue(ec.getPossibleValues().contains("Female"));
        assertTrue(ec.getPossibleValues().contains("Unknown"));
        assertFalse(ec.getPossibleValues().contains("Depends"));

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
        Constraint[] constraints = sSimpleFieldType.getConstraints();
        assertNotNull(constraints);
        Constraint enumConstraint = constraints[0];
        assertTrue(enumConstraint instanceof EnumConstraint);
        EnumConstraint ec = (EnumConstraint) enumConstraint;
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
        enumConstraint = constraints[0];
        assertTrue(enumConstraint instanceof EnumConstraint);
        ec = (EnumConstraint) enumConstraint;
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
        assertEquals(
                "TestNestedChoicesWithListType#anonymousListType",
                ct.getField("TestNestedChoicesWithListType#anonymousList").getType().getName());

        Field listField = ct.getField("TestNestedChoicesWithListType#anonymousList");
        assertTrue(listField.getType().isListType());

        ListType listType = (ListType) listField.getType();

        Field listItemField = listType.getField();
        assertEquals("TestNestedChoicesWithListType#anonymousListItem",
                listItemField.getType().getName());

        assertTrue(listItemField.getType().isComplexType());

        // check 2 subfields
        ComplexType ctitem = (ComplexType) listItemField.getType();
        Field field3A = ctitem.getField("field3A");
        assertNotNull(field3A);
        Field field3B = ctitem.getField("field3B");
        assertNotNull(field3B);
    }

}
