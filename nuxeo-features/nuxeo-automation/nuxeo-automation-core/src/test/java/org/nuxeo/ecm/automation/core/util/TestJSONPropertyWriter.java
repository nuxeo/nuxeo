/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.automation.core.util;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.DocumentPartImpl;
import org.nuxeo.ecm.core.api.model.impl.PropertyFactory;
import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.ComplexTypeImpl;
import org.nuxeo.ecm.core.schema.types.FieldImpl;
import org.nuxeo.ecm.core.schema.types.ListTypeImpl;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.SchemaImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class })
@Deploy({ "org.nuxeo.ecm.core.schema" })
@LocalDeploy({ "org.nuxeo.ecm.webengine.core:OSGI-INF/json-service.xml" })
public class TestJSONPropertyWriter {

    public static final String SCHEMA_NAME = "schema";

    public static final SchemaImpl SCHEMA = new SchemaImpl(SCHEMA_NAME, new Namespace("", ""));

    @Test
    public void testWritePropertyWithNullProperty() throws IOException {
        // write null
        testWriteProperty(JSONPropertyWriter.create(), StringType.INSTANCE, null, "{\"property\":null}");
        // don't write null
        testWriteProperty(JSONPropertyWriter.create().writeNull(false), StringType.INSTANCE, null, "{}");
    }

    @Test
    public void testWritePropertyWithBooleanProperty() throws IOException {
        testWriteProperty(JSONPropertyWriter.create(), BooleanType.INSTANCE, Boolean.TRUE, "{\"property\":true}");
    }

    @Test
    public void testWritePropertyWithLongProperty() throws IOException {
        testWriteProperty(JSONPropertyWriter.create(), LongType.INSTANCE, Long.valueOf(10L), "{\"property\":10}");
    }

    @Test
    public void testWritePropertyWithDoubleProperty() throws IOException {
        testWriteProperty(JSONPropertyWriter.create(), DoubleType.INSTANCE, Double.valueOf(2.5), "{\"property\":2.5}");
    }

    @Test
    @Ignore("PropertyFactory returns a LongProperty for IntegerType -> ClassCast exception")
    public void testWritePropertyWithIntegerProperty() throws IOException {
        testWriteProperty(JSONPropertyWriter.create(), IntegerType.INSTANCE, Integer.valueOf(2),
                "{\"property\":\"string value\"}");
    }

    @Test
    public void testWritePropertyWithDatePropertyDate() throws IOException {
        Date value = new Date();
        value.setTime(0L);
        testWriteProperty(JSONPropertyWriter.create(), DateType.INSTANCE, value,
                "{\"property\":\"1970-01-01T00:00:00.000Z\"}");
    }

    @Test
    public void testWritePropertyWithDatePropertyDateInMillis() throws IOException {
        Date value = new Date();
        value.setTime(1_000L);
        testWriteProperty(JSONPropertyWriter.create().dateTimeFormat(DateTimeFormat.TIME_IN_MILLIS), DateType.INSTANCE,
                value, "{\"property\":1000}");
    }

    @Test
    public void testWritePropertyWithDatePropertyCalendar() throws IOException {
        Calendar value = Calendar.getInstance();
        value.setTimeInMillis(0);
        testWriteProperty(JSONPropertyWriter.create(), DateType.INSTANCE, value,
                "{\"property\":\"1970-01-01T00:00:00.000Z\"}");
    }

    @Test
    public void testWritePropertyWithDatePropertyCalendarInMillis() throws IOException {
        Date value = new Date();
        value.setTime(1_000L);
        testWriteProperty(JSONPropertyWriter.create().dateTimeFormat(DateTimeFormat.TIME_IN_MILLIS), DateType.INSTANCE,
                value, "{\"property\":1000}");
    }

    @Test
    public void testWritePropertyWithStringProperty() throws IOException {
        testWriteProperty(JSONPropertyWriter.create(), StringType.INSTANCE, "string value",
                "{\"property\":\"string value\"}");
    }

    @Test
    public void testWritePropertyWithArrayProperty() throws IOException {
        Type type = new ListTypeImpl(SCHEMA_NAME, "strings", StringType.INSTANCE);
        List<String> values = Arrays.asList("string1", "string2");
        testWriteProperty(JSONPropertyWriter.create(), type, values, "{\"property\":[\"string1\",\"string2\"]}");
    }

    @Test
    public void testWritePropertyWithArrayPropertyNullValue() throws IOException {
        Type type = new ListTypeImpl(SCHEMA_NAME, "strings", StringType.INSTANCE);
        // write empty
        testWriteProperty(JSONPropertyWriter.create(), type, null, "{\"property\":[]}");
        // don't write empty
        testWriteProperty(JSONPropertyWriter.create().writeEmpty(false), type, null, "{}");
    }

    @Test
    public void testWritePropertyWithArrayPropertyEmptyValue() throws IOException {
        Type type = new ListTypeImpl(SCHEMA_NAME, "strings", StringType.INSTANCE);
        testWriteProperty(JSONPropertyWriter.create(), type, new ArrayList<>(), "{\"property\":[]}");
    }

    @Test
    public void testWritePropertyWithListProperty() throws IOException {
        Type type = new ListTypeImpl(SCHEMA_NAME, "strings", StringType.INSTANCE, "string", null, 0, -1);
        List<String> values = Arrays.asList("string1", "string2");
        testWriteProperty(JSONPropertyWriter.create(), type, values, "{\"property\":[\"string1\",\"string2\"]}");
    }

    @Test
    public void testWritePropertyWithListPropertyNullValue() throws IOException {
        Type type = new ListTypeImpl(SCHEMA_NAME, "strings", StringType.INSTANCE);
        // write empty
        testWriteProperty(JSONPropertyWriter.create(), type, null, "{\"property\":[]}");
        // don't write empty
        testWriteProperty(JSONPropertyWriter.create().writeEmpty(false), type, null, "{}");
    }

    @Test
    public void testWritePropertyWithListPropertyEmptyValue() throws IOException {
        Type type = new ListTypeImpl(SCHEMA_NAME, "strings", StringType.INSTANCE);
        // write empty
        testWriteProperty(JSONPropertyWriter.create(), type, Collections.emptyList(), "{\"property\":[]}");
        // don't write empty
        testWriteProperty(JSONPropertyWriter.create().writeEmpty(false), type, Collections.emptyList(), "{}");
    }

    @Test
    public void testWritePropertyWithMapProperty() throws IOException {
        ComplexType type = new ComplexTypeImpl(SCHEMA, SCHEMA_NAME, "complex");
        type.addField("key1", StringType.INSTANCE, null, 0, null);
        type.addField("key2", StringType.INSTANCE, null, 0, null);
        Map<String, String> values = new HashMap<>();
        values.put("key1", "value1");
        values.put("key2", "value2");
        testWriteProperty(JSONPropertyWriter.create(), type, values,
                "{\"property\":{\"key1\":\"value1\",\"key2\":\"value2\"}}");
    }

    @Test
    public void testWritePropertyWithMapPropertyNullValue() throws IOException {
        Type type = new ComplexTypeImpl(SCHEMA, SCHEMA_NAME, "complex");
        // write empty
        testWriteProperty(JSONPropertyWriter.create(), type, null, "{\"property\":{}}");
        // don't write empty
        testWriteProperty(JSONPropertyWriter.create().writeEmpty(false), type, null, "{}");
    }

    @Test
    public void testWritePropertyWithMapPropertyEmptyValue() throws IOException {
        Type type = new ComplexTypeImpl(SCHEMA, SCHEMA_NAME, "complex");
        // write empty
        testWriteProperty(JSONPropertyWriter.create(), type, Collections.emptyMap(), "{\"property\":{}}");
        // don't write empty
        testWriteProperty(JSONPropertyWriter.create().writeEmpty(false), type, Collections.emptyMap(), "{}");
    }

    @Test
    public void testWritePropertyWithBlobProperty() throws IOException {
        Type type = new ComplexTypeImpl(SCHEMA, SCHEMA_NAME, TypeConstants.CONTENT);
        Blob value = Blobs.createBlob("content");
        // write null
        testWriteProperty(JSONPropertyWriter.create(), type, value,
                "{\"property\":{\"name\":null,\"mime-type\":\"text/plain\",\"encoding\":\"UTF-8\","
                        + "\"digest\":null,\"length\":7}}");
        // don't write null
        testWriteProperty(JSONPropertyWriter.create().writeNull(false), type, value,
                "{\"property\":{\"mime-type\":\"text/plain\",\"encoding\":\"UTF-8\",\"length\":7}}");
    }

    @Test
    public void testWritePropertyWithBlobPropertyWithFileBaseUrl() throws IOException {
        Type type = new ComplexTypeImpl(SCHEMA, SCHEMA_NAME, TypeConstants.CONTENT);
        Blob value = Blobs.createBlob("content", "text/plain", "utf-8", "content.txt");
        testWriteProperty(JSONPropertyWriter.create().filesBaseUrl("http://fileBaseUrl"), type, value,
                "{\"property\":{\"name\":\"content.txt\",\"mime-type\":\"text/plain\",\"encoding\":\"utf-8\","
                        + "\"digest\":null,\"length\":7,\"data\":\"http://fileBaseUrl/schema:property/content.txt\"}}");
    }

    @Test
    public void testWritePropertyWithBlobPropertyNull() throws IOException {
        Type type = new ComplexTypeImpl(SCHEMA, SCHEMA_NAME, TypeConstants.CONTENT);
        // write null
        testWriteProperty(JSONPropertyWriter.create(), type, null, "{\"property\":null}");
        // don't write null
        testWriteProperty(JSONPropertyWriter.create().writeNull(false), type, null, "{}");
    }

    @Test
    public void testWritePropertyValueWithNullProperty() throws IOException {
        testWritePropertyValue(StringType.INSTANCE, null, "null");
    }

    @Test
    public void testWritePropertyValueWithBooleanProperty() throws IOException {
        testWritePropertyValue(BooleanType.INSTANCE, Boolean.TRUE, "true");
    }

    @Test
    public void testWritePropertyValueWithLongProperty() throws IOException {
        testWritePropertyValue(LongType.INSTANCE, Long.valueOf(10L), "10");
    }

    @Test
    public void testWritePropertyValueWithDoubleProperty() throws IOException {
        testWritePropertyValue(DoubleType.INSTANCE, Double.valueOf(2.5), "2.5");
    }

    @Test
    @Ignore("PropertyFactory returns a LongProperty for IntegerType -> ClassCast exception")
    public void testWritePropertyValueWithIntegerProperty() throws IOException {
        testWritePropertyValue(IntegerType.INSTANCE, Integer.valueOf(2), "\"string value\"");
    }

    @Test
    public void testWritePropertyValueWithDatePropertyDate() throws IOException {
        Date value = new Date();
        value.setTime(0L);
        testWritePropertyValue(DateType.INSTANCE, value, "\"1970-01-01T00:00:00.000Z\"");
    }

    @Test
    public void testWritePropertyValueWithDatePropertyDateInMillis() throws IOException {
        Date value = new Date();
        value.setTime(1_000L);
        testWritePropertyValue(DateType.INSTANCE, value, "1000", DateTimeFormat.TIME_IN_MILLIS);
    }

    @Test
    public void testWritePropertyValueWithDatePropertyCalendar() throws IOException {
        Calendar value = Calendar.getInstance();
        value.setTimeInMillis(0);
        testWritePropertyValue(DateType.INSTANCE, value, "\"1970-01-01T00:00:00.000Z\"");
    }

    @Test
    public void testWritePropertyValueWithDatePropertyCalendarInMillis() throws IOException {
        Date value = new Date();
        value.setTime(1_000L);
        testWritePropertyValue(DateType.INSTANCE, value, "1000", DateTimeFormat.TIME_IN_MILLIS);
    }

    @Test
    public void testWritePropertyValueWithStringProperty() throws IOException {
        testWritePropertyValue(StringType.INSTANCE, "string value", "\"string value\"");
    }

    @Test
    public void testWritePropertyValueWithArrayProperty() throws IOException {
        Type type = new ListTypeImpl(SCHEMA_NAME, "strings", StringType.INSTANCE);
        List<String> values = Arrays.asList("string1", "string2");
        testWritePropertyValue(type, values, "[\"string1\",\"string2\"]");
    }

    @Test
    public void testWritePropertyValueWithArrayPropertyNullValue() throws IOException {
        Type type = new ListTypeImpl(SCHEMA_NAME, "strings", StringType.INSTANCE);
        testWritePropertyValue(type, null, "[]");
    }

    @Test
    public void testWritePropertyValueWithArrayPropertyEmptyValue() throws IOException {
        Type type = new ListTypeImpl(SCHEMA_NAME, "strings", StringType.INSTANCE);
        testWritePropertyValue(type, new ArrayList<>(), "[]");
    }

    @Test
    public void testWritePropertyValueWithListProperty() throws IOException {
        Type type = new ListTypeImpl(SCHEMA_NAME, "strings", StringType.INSTANCE, "string", null, 0, -1);
        List<String> values = Arrays.asList("string1", "string2");
        testWritePropertyValue(type, values, "[\"string1\",\"string2\"]");
    }

    @Test
    public void testWritePropertyValueWithListPropertyNullValue() throws IOException {
        Type type = new ListTypeImpl(SCHEMA_NAME, "strings", StringType.INSTANCE);
        testWritePropertyValue(type, null, "[]");
    }

    @Test
    public void testWritePropertyValueWithListPropertyEmptyValue() throws IOException {
        Type type = new ListTypeImpl(SCHEMA_NAME, "strings", StringType.INSTANCE);
        testWritePropertyValue(type, Collections.emptyList(), "[]");
    }

    @Test
    public void testWritePropertyValueWithMapProperty() throws IOException {
        ComplexType type = new ComplexTypeImpl(SCHEMA, SCHEMA_NAME, "complex");
        type.addField("key1", StringType.INSTANCE, null, 0, null);
        type.addField("key2", StringType.INSTANCE, null, 0, null);
        Map<String, String> values = new HashMap<>();
        values.put("key1", "value1");
        values.put("key2", "value2");
        testWritePropertyValue(type, values, "{\"key1\":\"value1\",\"key2\":\"value2\"}");
    }

    @Test
    public void testWritePropertyValueWithMapPropertyNullValue() throws IOException {
        Type type = new ComplexTypeImpl(SCHEMA, SCHEMA_NAME, "complex");
        testWritePropertyValue(type, null, "{}");
    }

    @Test
    public void testWritePropertyValueWithMapPropertyEmptyValue() throws IOException {
        Type type = new ComplexTypeImpl(SCHEMA, SCHEMA_NAME, "complex");
        testWritePropertyValue(type, Collections.emptyMap(), "{}");
    }

    @Test
    public void testWritePropertyValueWithBlobProperty() throws IOException {
        Type type = new ComplexTypeImpl(SCHEMA, SCHEMA_NAME, TypeConstants.CONTENT);
        Blob value = Blobs.createBlob("content");
        testWritePropertyValue(type, value, "{\"name\":null,\"mime-type\":\"text/plain\",\"encoding\":\"UTF-8\","
                + "\"digest\":null,\"length\":7}");
    }

    @Test
    public void testWritePropertyValueWithBlobPropertyWithFileBaseUrl() throws IOException {
        Type type = new ComplexTypeImpl(SCHEMA, SCHEMA_NAME, TypeConstants.CONTENT);
        Blob value = Blobs.createBlob("content", "text/plain", "utf-8", "content.txt");
        testWritePropertyValue(type, value,
                "{\"name\":\"content.txt\",\"mime-type\":\"text/plain\",\"encoding\":\"utf-8\",\"digest\":null,"
                        + "\"length\":7,\"data\":\"http://fileBaseUrl/schema:property/content.txt\"}",
                "http://fileBaseUrl/");
    }

    @Test
    public void testWritePropertyValueWithBlobPropertyNullValue() throws IOException {
        Type type = new ComplexTypeImpl(SCHEMA, SCHEMA_NAME, TypeConstants.CONTENT);
        testWritePropertyValue(type, null, "null");
    }

    protected void testWriteProperty(JSONPropertyWriter propertyWriter, Type type, Object value, String expectedValue)
            throws IOException {
        // Init generator
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getFactory().createJsonGenerator(out);

        // Write value
        Property property = createProperty(type, value);
        jg.writeStartObject();
        propertyWriter.writeProperty(jg, property);
        jg.writeEndObject();
        jg.flush();
        jg.close();
        String result = out.toString("UTF-8");

        // Assert result
        assertEquals(expectedValue, result);
    }

    protected void testWritePropertyValue(Type type, Object value, String expectedValue) throws IOException {
        testWritePropertyValue(type, value, expectedValue, DateTimeFormat.W3C, null);
    }

    protected void testWritePropertyValue(Type type, Object value, String expectedValue, String filesBaseUrl)
            throws IOException {
        testWritePropertyValue(type, value, expectedValue, DateTimeFormat.W3C, filesBaseUrl);
    }

    protected void testWritePropertyValue(Type type, Object value, String expectedValue, DateTimeFormat dateTimeFormat)
            throws IOException {
        testWritePropertyValue(type, value, expectedValue, dateTimeFormat, null);
    }

    protected void testWritePropertyValue(Type type, Object value, String expectedValue, DateTimeFormat dateTimeFormat,
            String filesBaseUrl) throws IOException {
        // Init generator
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = getFactory().createJsonGenerator(out);

        // Write value
        Property property = createProperty(type, value);
        JSONPropertyWriter.writePropertyValue(jg, property, dateTimeFormat, filesBaseUrl);
        jg.flush();
        jg.close();
        String result = out.toString("UTF-8");

        // Assert result
        assertEquals(expectedValue, result);
    }

    private JsonFactory getFactory() {
        JsonFactoryManager jsonFactoryManager = Framework.getService(JsonFactoryManager.class);
        return jsonFactoryManager.getJsonFactory();
    }

    private Property createProperty(Type type, Object value) {
        Property property = PropertyFactory.createProperty(new DocumentPartImpl(SCHEMA),
                new FieldImpl(new QName("property"), type, type), 0);
        property.setValue(value);
        return property;
    }

}
