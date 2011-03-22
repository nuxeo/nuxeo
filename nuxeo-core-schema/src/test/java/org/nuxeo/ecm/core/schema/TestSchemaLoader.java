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

import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Constraint;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.constraints.StringLengthConstraint;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestSchemaLoader extends NXRuntimeTestCase {

    public static final String NS_XSD = "http://www.w3.org/2001/XMLSchema";

    private SchemaManagerImpl typeMgr;

    private XSDLoader reader;

    @Override
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
    public void XXXtestXSDReader() throws Exception {
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

    public void testContribs() throws Exception {
        deployContrib("org.nuxeo.ecm.core.schema.tests",
                "CoreTestExtensions.xml");
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
    }

}
