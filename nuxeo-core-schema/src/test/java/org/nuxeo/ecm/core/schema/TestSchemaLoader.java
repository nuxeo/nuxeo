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
 * $Id$
 */

package org.nuxeo.ecm.core.schema;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestSchemaLoader extends NXRuntimeTestCase {

    public static final String NS_XSD = "http://www.w3.org/2001/XMLSchema";
    private SchemaManagerImpl typeMgr;
    private XSDLoader reader;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib("nuxeo-core-schema", "OSGI-INF/SchemaService.xml");
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

    // FIXME: this tests makes too string assumptions on how the fields will be ordered when we iterate over them (fails under Java 6)
    public void XXXtestXSDReader() throws Exception {
        URL url = getResource("schema/schema.xsd");

        reader.loadSchema("MySchema", "", url);
        //Collection<Schema> schemas = typeMgr.getSchemas();
        // do not check schemas size - this is dynamic
        //assertEquals(4, schemas.size()); // file, common, MySchema
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

    public void testFeature() throws Exception {
        deployContrib("CoreTestExtensions.xml");
        DocumentType docType = typeMgr.getDocumentType("myDoc");

        assertNotNull(docType);
        assertEquals(1, docType.getSchemas().size());
        Schema schema = docType.getSchema("schema2");
        assertNotNull(schema);
        assertEquals(2, schema.getFields().size());

        Field field = schema.getField("title");
        assertNotNull(field);

        field = schema.getField("description");
        assertNotNull(field);
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
        Assert.assertNotNull(rule);
        Field name = ((ComplexType) rule.getType()).getField("name");
        Assert.assertNotNull(name);
        Assert.assertEquals(name.getType().getName(), StringType.INSTANCE.getName());

        // recursivity

        Field ruleGroup = schema.getField("RULE-GROUP");
        Assert.assertNotNull(ruleGroup);
        ComplexType ct = (ComplexType) ruleGroup.getType();
        ruleGroup = ct.getField("RULE-GROUP");
        Assert.assertNotNull(ruleGroup);
        Assert.assertNotNull(ct.getField("RULE"));
    }

}
