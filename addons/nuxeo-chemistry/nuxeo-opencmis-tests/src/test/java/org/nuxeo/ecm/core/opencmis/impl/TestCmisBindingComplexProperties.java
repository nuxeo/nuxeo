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
 */
package org.nuxeo.ecm.core.opencmis.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.apache.chemistry.opencmis.commons.data.Properties;
import org.apache.chemistry.opencmis.commons.data.PropertyData;
import org.apache.chemistry.opencmis.commons.data.PropertyString;
import org.apache.chemistry.opencmis.commons.spi.BindingsObjectFactory;
import org.apache.chemistry.opencmis.commons.spi.Holder;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.util.DateTimeFormat;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoTypeHelper;
import org.nuxeo.ecm.core.schema.utils.DateParser;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ CmisFeature.class, CmisFeatureConfiguration.class })
@Deploy({ "org.nuxeo.ecm.webengine.core", //
        "org.nuxeo.ecm.automation.core" //
})
@LocalDeploy("org.nuxeo.ecm.core.opencmis.tests.tests:OSGI-INF/types-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestCmisBindingComplexProperties extends TestCmisBindingBase {

    @Inject
    protected CoreSession coreSession;

    @Before
    public void setUp() throws Exception {
        setUpBinding(coreSession);
        setUpData(coreSession);
    }

    @After
    public void tearDown() {
        Framework.getProperties().remove(NuxeoTypeHelper.ENABLE_COMPLEX_PROPERTIES);
        tearDownBinding();
    }

    protected ObjectData getObjectByPath(String path) {
        return objService.getObjectByPath(repositoryId, path, null, null, null, null, null, null, null);
    }

    protected Properties createProperties(String key, String value) {
        BindingsObjectFactory factory = binding.getObjectFactory();
        PropertyString prop = factory.createPropertyStringData(key, value);
        return factory.createPropertiesData(Collections.<PropertyData<?>> singletonList(prop));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetComplexListProperty() throws Exception {
        // Enable complex properties
        Framework.getProperties().setProperty(NuxeoTypeHelper.ENABLE_COMPLEX_PROPERTIES, "true");

        // Create a complex property to encode
        List<Map<String, Object>> propList = createComplexPropertyList(3);

        // Set the property value on a document
        CoreSession session = coreSession;
        DocumentModel doc = session.createDocumentModel("/", null, "ComplexFile");
        doc.setPropertyValue("complexTest:listItem", (Serializable) propList);
        doc = session.createDocument(doc);
        session.save();
        doc.refresh();
        assertTrue(session.exists(new IdRef(doc.getId())));

        // Get the property as CMIS will see it from the object service
        Properties p = objService.getProperties(repositoryId, doc.getId(), null, null);
        assertNotNull(p);
        List<Object> cmisValues = (List<Object>) p.getProperties().get("complexTest:listItem").getValues();
        assertEquals("Wrong number of marshaled values", propList.size(), cmisValues.size());

        // Verify the JSON produced is valid and matches the original objects
        ObjectMapper mapper = new ObjectMapper();
        for (int i = 0; i < cmisValues.size(); i++) {
            JsonNode jsonNode = mapper.readTree(cmisValues.get(i).toString());
            Map<String, Object> orig = propList.get(i);
            assertComplexPropertyNodeEquals(orig, jsonNode, DateTimeFormat.W3C);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testQueryComplexListProperty() throws Exception {
        // Enable complex properties
        Framework.getProperties().setProperty(NuxeoTypeHelper.ENABLE_COMPLEX_PROPERTIES, "true");

        // Create a complex property to encode
        List<Map<String, Object>> propList = createComplexPropertyList(3);

        // Set the property value on a document
        CoreSession session = coreSession;
        DocumentModel doc = session.createDocumentModel("/", null, "ComplexFile");
        doc.setPropertyValue("complexTest:listItem", (Serializable) propList);
        doc = session.createDocument(doc);
        session.save();
        doc.refresh();
        assertTrue(session.exists(new IdRef(doc.getId())));

        // explicit select
        String statement = "SELECT complexTest:listItem FROM ComplexFile";
        ObjectList res = discService.query(repositoryId, statement, Boolean.TRUE, null, null, null, null, null, null);
        assertEquals(1, res.getNumItems().intValue());
        PropertyData<String> data = (PropertyData<String>) res.getObjects().get(0).getProperties().getProperties().get("complexTest:listItem");
        assertNotNull(data);
        // Verify the JSON produced is valid and matches the original objects
        List<String> values = data.getValues();
        for (int i = 0; i < values.size(); i++) {
            String jsonStr = values.get(i);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(jsonStr);
            Map<String, Object> propMap = (Map<String, Object>) propList.get(i);
            assertComplexPropertyNodeEquals(propMap, jsonNode, DateTimeFormat.W3C);
        }

        // star select, doesn't return lists
        statement = "SELECT * FROM ComplexFile";
        res = discService.query(repositoryId, statement, Boolean.TRUE, null, null, null, null, null, null);
        assertEquals(1, res.getNumItems().intValue());
        data = (PropertyData<String>) res.getObjects().get(0).getProperties().getProperties().get("complexTest:listItem");
        assertNull(data);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetComplexListProperty() throws Exception {
        // Enable complex properties
        Framework.getProperties().setProperty(NuxeoTypeHelper.ENABLE_COMPLEX_PROPERTIES, "true");

        // Create some JSON to pass into the CMIS service
        ArrayList<ObjectNode> nodeList = createComplexNodeList(3, DateTimeFormat.TIME_IN_MILLIS);

        // Get a document with the right property schema
        CoreSession session = coreSession;
        DocumentModel doc = session.createDocumentModel("/", null, "ComplexFile");
        doc = session.createDocument(doc);
        session.save();

        // Set the property as a JSON string through the CMIS service
        BindingsObjectFactory bof = binding.getObjectFactory();
        ArrayList<String> stringArr = new ArrayList<String>();
        for (int i = 0; i < nodeList.size(); i++) {
            stringArr.add(nodeList.get(i).toString());
        }
        PropertyString prop = bof.createPropertyStringData("complexTest:listItem", stringArr);
        Properties props = bof.createPropertiesData(Collections.<PropertyData<?>> singletonList(prop));
        Holder<String> objectIdHolder = new Holder<String>(doc.getId());
        Holder<String> changeTokenHolder = new Holder<String>(doc.getChangeToken());
        objService.updateProperties(repositoryId, objectIdHolder, changeTokenHolder, props, null);

        // Verify the properties produced in Nuxeo match the input JSON
        session.save();
        doc.refresh();
        List<Object> list = (List<Object>) doc.getPropertyValue("complexTest:listItem");
        assertEquals("Wrong number of elements in list", nodeList.size(), list.size());
        for (int i = 0; i < list.size(); i++) {
            JsonNode orig = nodeList.get(i);
            Map<String, Object> obj = (Map<String, Object>) list.get(i);
            assertComplexPropertyNodeEquals(obj, orig, DateTimeFormat.TIME_IN_MILLIS);
        }
    }

    @Test
    public void testGetComplexProperty() throws Exception {
        // Enable complex properties
        Framework.getProperties().setProperty(NuxeoTypeHelper.ENABLE_COMPLEX_PROPERTIES, "true");

        // Create a complex property to encode
        List<Map<String, Object>> list = createComplexPropertyList(1);
        Map<String, Object> propMap = list.get(0);

        // Set the property value on a document
        CoreSession session = coreSession;
        DocumentModel doc = session.createDocumentModel("/", null, "ComplexFile");
        doc.setPropertyValue("complexTest:complexItem", (Serializable) propMap);
        Blob blob = Blobs.createBlob("Test content");
        blob.setFilename("test.txt");
        doc.setProperty("file", "content", blob);
        doc = session.createDocument(doc);
        session.save();
        doc.refresh();
        assertTrue(session.exists(new IdRef(doc.getId())));

        // Get the property as CMIS will see it from the object service
        Properties p = objService.getProperties(repositoryId, doc.getId(), null, null);
        assertNotNull(p);
        String jsonStr = p.getProperties().get("complexTest:complexItem").getFirstValue().toString();
        assertEquals("Complex item should get marshaled as a single string value", 1,
                p.getProperties().get("complexTest:complexItem").getValues().size());

        // Verify the JSON produced is valid and matches the original objects
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStr);
        assertComplexPropertyNodeEquals(propMap, jsonNode, DateTimeFormat.W3C);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testQueryComplexProperty() throws Exception {
        // Enable complex properties
        Framework.getProperties().setProperty(NuxeoTypeHelper.ENABLE_COMPLEX_PROPERTIES, "true");

        // Create a complex property to encode
        List<Map<String, Object>> list = createComplexPropertyList(1);
        Map<String, Object> propMap = list.get(0);

        // Set the property value on a document
        CoreSession session = coreSession;
        DocumentModel doc = session.createDocumentModel("/", null, "ComplexFile");
        doc.setPropertyValue("complexTest:complexItem", (Serializable) propMap);
        Blob blob = Blobs.createBlob("Test content");
        blob.setFilename("test.txt");
        doc.setProperty("file", "content", blob);
        doc = session.createDocument(doc);
        session.save();
        doc.refresh();
        assertTrue(session.exists(new IdRef(doc.getId())));

        // explicit select
        String statement = "SELECT complexTest:complexItem FROM ComplexFile";
        ObjectList res = discService.query(repositoryId, statement, Boolean.TRUE, null, null, null, null, null, null);
        assertEquals(1, res.getNumItems().intValue());
        // Verify the JSON produced is valid and matches the original objects
        PropertyData<String> data = (PropertyData<String>) res.getObjects().get(0).getProperties().getProperties().get("complexTest:complexItem");
        String jsonStr = data.getFirstValue();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStr);
        assertComplexPropertyNodeEquals(propMap, jsonNode, DateTimeFormat.W3C);

        // star select, doesn't return strings from complex props
        statement = "SELECT * FROM ComplexFile";
        res = discService.query(repositoryId, statement, Boolean.TRUE, null, null, null, null, null, null);
        assertEquals(1, res.getNumItems().intValue());
        data = (PropertyData<String>) res.getObjects().get(0).getProperties().getProperties().get("complexTest:complexItem");
        assertNull(data);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetComplexProperty() throws Exception {
        // Enable complex properties
        Framework.getProperties().setProperty(NuxeoTypeHelper.ENABLE_COMPLEX_PROPERTIES, "true");

        // Create some JSON to pass into the CMIS service
        ArrayList<ObjectNode> nodeList = createComplexNodeList(1, DateTimeFormat.TIME_IN_MILLIS);
        ObjectNode jsonObj = nodeList.get(0);
        String jsonStr = jsonObj.toString();

        // Get a document with the right property schema
        CoreSession session = coreSession;
        DocumentModel doc = session.createDocumentModel("/", null, "ComplexFile");
        doc = session.createDocument(doc);
        session.save();

        // Set the property as a JSON string through the CMIS service
        Properties props = createProperties("complexTest:complexItem", jsonStr);
        Holder<String> objectIdHolder = new Holder<String>(doc.getId());
        Holder<String> changeTokenHolder = new Holder<String>(doc.getChangeToken());
        objService.updateProperties(repositoryId, objectIdHolder, changeTokenHolder, props, null);

        // Verify the properties produced in Nuxeo match the input JSON
        session.save();
        doc.refresh();
        Map<String, Object> propMap = (Map<String, Object>) doc.getPropertyValue("complexTest:complexItem");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStr);
        assertComplexPropertyNodeEquals(propMap, jsonNode, DateTimeFormat.TIME_IN_MILLIS);
    }

    /**
     * Test that complex types are not exposed unless the enabled property is set
     */
    public void testEnableComplexProperties() throws Exception {
        // Don't enable complex properties for this test

        // Set a complex property on a document
        HashMap<String, Object> propMap = new HashMap<String, Object>();
        propMap.put("stringProp", "testString");

        // Set the property value on a document
        CoreSession session = coreSession;
        DocumentModel doc = session.createDocumentModel("/", null, "ComplexFile");
        doc.setPropertyValue("complexTest:complexItem", propMap);
        doc = session.createDocument(doc);
        session.save();
        doc.refresh();
        assertTrue(session.exists(new IdRef(doc.getId())));

        // Get the property as CMIS will see it from the object service
        Properties p = objService.getProperties(repositoryId, doc.getId(), null, null);
        assertNotNull(p);
        assertNull("Complex property should not be exposed when not enabled in framework properties",
                p.getProperties().get("complexTest:complexItem"));
    }

    private List<Map<String, Object>> createComplexPropertyList(int listSize) {
        List<Map<String, Object>> list = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1234500000000L);
        for (int i = 1; i <= listSize; i++) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            list.add(map);
            map.put("stringProp", "testString" + i);
            map.put("dateProp", cal);
            map.put("enumProp", "ValueA");
            List<String> arrayProp = new ArrayList<String>();
            map.put("arrayProp", arrayProp);
            for (int j = 1; j <= i; j++) {
                arrayProp.add(Integer.toString(j));
            }
            map.put("intProp", Integer.valueOf(123));
            map.put("boolProp", Boolean.TRUE);
            map.put("floatProp", Double.valueOf(123.45));

        }
        return list;
    }

    private ArrayList<ObjectNode> createComplexNodeList(int listSize, DateTimeFormat dateTimeFormat) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayList<ObjectNode> jsonObjects = new ArrayList<ObjectNode>();
        for (int i = 1; i <= listSize; i++) {
            ObjectNode jsonObj = mapper.createObjectNode();
            jsonObj.put("stringProp", "testString" + i);
            if (dateTimeFormat.equals(DateTimeFormat.TIME_IN_MILLIS)) {
                jsonObj.put("dateProp", 1234500000000L);
            } else {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(1234500000000L);
                String dateStr = DateParser.formatW3CDateTime(cal.getTime());
                jsonObj.put("dateProp", dateStr);
            }
            jsonObj.put("enumProp", "ValueA");
            ArrayNode jsonArray = mapper.createArrayNode();
            jsonObj.put("arrayProp", jsonArray);
            for (int j = 1; j <= i; j++) {
                jsonArray.add(Integer.toString(j));
            }
            jsonObj.put("intProp", 123);
            jsonObj.put("boolProp", true);
            jsonObj.put("floatProp", 123.45d);
            jsonObjects.add(jsonObj);
        }
        return jsonObjects;
    }

    private void assertComplexPropertyNodeEquals(Map<String, Object> propMap, JsonNode jsonNode,
            DateTimeFormat dateTimeFormat) throws IOException {
        List<String> nodeKeys = copyIterator(jsonNode.getFieldNames());
        Set<String> propKeys = propMap.keySet();
        assertEquals(nodeKeys.size(), propKeys.size());
        nodeKeys.containsAll(propKeys);
        for (String key : propKeys) {
            Object origVal = propMap.get(key);
            if (origVal instanceof ArrayList || origVal instanceof Object[]) {
                List<Object> origList;
                if (origVal instanceof ArrayList) {
                    @SuppressWarnings("unchecked")
                    List<Object> l = (List<Object>) origVal;
                    origList = l;
                } else {
                    origList = Arrays.asList((Object[]) origVal);
                }
                ArrayNode jsonArray = (ArrayNode) jsonNode.get(key);
                for (int i = 0; i < origList.size(); i++) {
                    assertEquals("Wrong value at key [" + key + "] index [" + i + "]", origList.get(i).toString(),
                            jsonArray.get(i).getValueAsText());
                }
            } else {
                if (origVal instanceof Calendar) {
                    if (DateTimeFormat.TIME_IN_MILLIS.equals(dateTimeFormat)) {
                        origVal = Long.valueOf(((Calendar) origVal).getTimeInMillis());
                    } else {
                        origVal = DateParser.formatW3CDateTime(((Calendar) origVal).getTime());
                    }
                }
                assertEquals("Wrong value at key [" + key + "]", origVal.toString(), jsonNode.get(key).getValueAsText());
            }
        }
    }

    private <T> List<T> copyIterator(Iterator<T> iter) {
        List<T> copy = new ArrayList<T>();
        while (iter.hasNext())
            copy.add(iter.next());
        return copy;
    }
}
