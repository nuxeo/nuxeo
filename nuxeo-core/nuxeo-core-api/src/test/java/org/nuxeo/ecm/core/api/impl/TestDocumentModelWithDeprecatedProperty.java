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
package org.nuxeo.ecm.core.api.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.impl.AbstractProperty;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 9.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, LogCaptureFeature.class })
@Deploy("org.nuxeo.ecm.core.schema")
@Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-documentmodel-deprecated-types-contrib.xml")
@LogCaptureFeature.FilterOn(logLevel = "WARN", loggerClass = AbstractProperty.class)
public class TestDocumentModelWithDeprecatedProperty {

    protected static final String DEPRECATED_SCHEMA = "deprecated";

    protected static final String GET_VALUE_LOG = "Property '%s' is marked as deprecated from '%s' schema, don't use it anymore. "
            + "Return value from deprecated property";

    protected static final String SET_VALUE_LOG = "Property '%s' is marked as deprecated from '%s' schema, don't use it anymore. "
            + "Set value to deprecated property";

    protected static final String GET_VALUE_FALLBACK_LOG = "Property '%s' is marked as deprecated from '%s' schema, don't use it anymore. "
            + "Return value from '%s' if not null, from deprecated property otherwise";

    protected static final String SET_VALUE_FALLBACK_LOG = "Property '%s' is marked as deprecated from '%s' schema, don't use it anymore. "
            + "Set value to deprecated property and to fallback property '%s'";

    protected static final String GET_VALUE_PARENT_LOG = "Property '%s' is marked as deprecated from '%s' schema because property '%s' is marked as deprecated, don't use it anymore. "
            + "Return value from deprecated property";

    protected static final String SET_VALUE_PARENT_LOG = "Property '%s' is marked as deprecated from '%s' schema because property '%s' is marked as deprecated, don't use it anymore. "
            + "Set value to deprecated property";

    protected static final String GET_VALUE_FALLBACK_PARENT_LOG = "Property '%s' is marked as deprecated from '%s' schema because property '%s' is marked as deprecated, don't use it anymore. "
            + "Return value from '%s' if not null, from deprecated property otherwise";

    protected static final String SET_VALUE_FALLBACK_PARENT_LOG = "Property '%s' is marked as deprecated from '%s' schema because property '%s' is marked as deprecated, don't use it anymore. "
            + "Set value to deprecated property and to fallback property '%s'";

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    // --------------------------------
    // Tests with deprecated properties
    // --------------------------------

    @Test
    public void testSetDeprecatedScalarProperty() throws Exception {
        testProperty("scalar", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarPropertyValue() throws Exception {
        testPropertyValue("scalar", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarProperties() throws Exception {
        testProperties("scalar", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarArrayProperty() throws Exception {
        testProperty("scalars", new String[] { "test scalar", "test scalar2" });
    }

    @Test
    public void testSetDeprecatedScalarArrayPropertyValue() throws Exception {
        testPropertyValue("scalars", new String[] { "test scalar", "test scalar2" });
    }

    @Test
    public void testSetDeprecatedScalarArrayProperties() throws Exception {
        testProperties("scalars", new String[] { "test scalar", "test scalar2" });
    }

    @Test
    public void testSetDeprecatedComplexProperty() throws Exception {
        String deprecatedProperty = "complexDep";
        Map<String, String> value = Collections.singletonMap("scalar", "test scalar");
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty(DEPRECATED_SCHEMA, deprecatedProperty, value);
        // As property is just deprecated we still store the value
        assertEquals(value, doc.getProperty(DEPRECATED_SCHEMA, deprecatedProperty));

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        // 4 logs:
        // - a set while setting the value
        // - a set for deprecated parent as this is this value we set
        // - a get for deprecated parent as this is this value we get
        // - a get to assert property
        assertEquals(4, events.size());
        assertEquals(String.format(SET_VALUE_PARENT_LOG, deprecatedProperty + "/scalar", DEPRECATED_SCHEMA,
                deprecatedProperty), events.get(0));
        assertEquals(String.format(SET_VALUE_LOG, deprecatedProperty, DEPRECATED_SCHEMA), events.get(1));
        assertEquals(String.format(GET_VALUE_LOG, deprecatedProperty, DEPRECATED_SCHEMA), events.get(2));
        assertEquals(String.format(GET_VALUE_PARENT_LOG, deprecatedProperty + "/scalar", DEPRECATED_SCHEMA,
                deprecatedProperty), events.get(3));
        logCaptureResult.clear();
    }

    @Test
    public void testSetDeprecatedComplexPropertyValue() throws Exception {
        String deprecatedProperty = "complexDep";
        Serializable value = (Serializable) Collections.singletonMap("scalar", "test scalar");
        String xpath = DEPRECATED_SCHEMA + ':' + deprecatedProperty;

        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue(xpath, value);
        assertEquals(value, doc.getPropertyValue(xpath));

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        // 4 logs:
        // - a set while setting the value
        // - a set for deprecated parent as this is this value we set
        // - a get for deprecated parent as this is this value we get
        // - a get to assert property
        assertEquals(4, events.size());
        assertEquals(String.format(SET_VALUE_PARENT_LOG, deprecatedProperty + "/scalar", DEPRECATED_SCHEMA,
                deprecatedProperty), events.get(0));
        assertEquals(String.format(SET_VALUE_LOG, deprecatedProperty, DEPRECATED_SCHEMA), events.get(1));
        assertEquals(String.format(GET_VALUE_LOG, deprecatedProperty, DEPRECATED_SCHEMA), events.get(2));
        assertEquals(String.format(GET_VALUE_PARENT_LOG, deprecatedProperty + "/scalar", DEPRECATED_SCHEMA,
                deprecatedProperty), events.get(3));
        logCaptureResult.clear();

        // Test also with a xpath without schema
        doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue(deprecatedProperty, value);
        assertEquals(value, doc.getProperty(xpath).getValue());

        logCaptureResult.assertHasEvent();
        events = logCaptureResult.getCaughtEventMessages();
        // 4 logs:
        // - a set while setting the value
        // - a set for deprecated parent as this is this value we set
        // - a get for deprecated parent as this is this value we get
        // - a get to assert property
        assertEquals(4, events.size());
        assertEquals(String.format(SET_VALUE_PARENT_LOG, deprecatedProperty + "/scalar", DEPRECATED_SCHEMA,
                deprecatedProperty), events.get(0));
        assertEquals(String.format(SET_VALUE_LOG, deprecatedProperty, DEPRECATED_SCHEMA), events.get(1));
        assertEquals(String.format(GET_VALUE_LOG, deprecatedProperty, DEPRECATED_SCHEMA), events.get(2));
        assertEquals(String.format(GET_VALUE_PARENT_LOG, deprecatedProperty + "/scalar", DEPRECATED_SCHEMA,
                deprecatedProperty), events.get(3));
        logCaptureResult.clear();
    }

    @Test
    public void testSetDeprecatedComplexProperties() throws Exception {
        String deprecatedProperty = "complexDep";
        Serializable value = (Serializable) Collections.singletonMap("scalar", "test scalar");
        Map<String, Object> map = Collections.singletonMap(deprecatedProperty, value);

        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties(DEPRECATED_SCHEMA, map);
        assertEquals(value, doc.getProperties(DEPRECATED_SCHEMA).get(deprecatedProperty));

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        // 4 logs:
        // - a set while setting the value
        // - a set for deprecated parent as this is this value we set
        // - a get for deprecated parent as this is this value we get
        // - a get to assert property
        assertEquals(4, events.size());
        assertEquals(String.format(SET_VALUE_PARENT_LOG, deprecatedProperty + "/scalar", DEPRECATED_SCHEMA,
                deprecatedProperty), events.get(0));
        assertEquals(String.format(SET_VALUE_LOG, deprecatedProperty, DEPRECATED_SCHEMA), events.get(1));
        assertEquals(String.format(GET_VALUE_LOG, deprecatedProperty, DEPRECATED_SCHEMA), events.get(2));
        assertEquals(String.format(GET_VALUE_PARENT_LOG, deprecatedProperty + "/scalar", DEPRECATED_SCHEMA,
                deprecatedProperty), events.get(3));
        logCaptureResult.clear();
    }

    @Test
    public void testSetScalarOnDeprecatedComplexProperty() throws Exception {
        testProperty("complexDep/scalar", "test scalar");
    }

    @Test
    public void testSetScalarOnDeprecatedComplexPropertyValue() throws Exception {
        testPropertyValue("complexDep/scalar", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarOnComplexProperty() throws Exception {
        testProperty("complex/scalar", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarOnComplexPropertyValue() throws Exception {
        testPropertyValue("complex/scalar", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarOnComplexProperties() throws Exception {
        testProperties("complex/scalar", "test scalar");
    }

    // ----------------------------------------------
    // Tests with deprecated properties with fallback
    // ----------------------------------------------

    @Test
    public void testSetDeprecatedScalarPropertyWithFallbackOnScalar() throws Exception {
        testProperty("scalar2scalar", "scalarfallback", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarPropertyValueWithFallbackOnScalar() throws Exception {
        testPropertyValue("scalar2scalar", "scalarfallback", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarPropertiesWithFallbackOnScalar() throws Exception {
        testProperties("scalar2scalar", "scalarfallback", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarPropertyWithFallbackOnComplex() throws Exception {
        testProperty("scalar2complex", "complexfallback/scalar", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarPropertyValueWithFallbackOnComplex() throws Exception {
        testPropertyValue("scalar2complex", "complexfallback/scalar", "test scalar");
    }

    @Test
    public void testSetDeprecatedScalarPropertiesWithFallbackOnComplex() throws Exception {
        testProperties("scalar2scalar", "scalarfallback", "test scalar");
    }

    @Test
    public void testSetDeprecatedComplexPropertyWithFallbackOnComplex() throws Exception {
        String deprecatedProperty = "complex2complex";
        String fallbackProperty = "complexfallback";
        Object value = Collections.singletonMap("scalar", "test scalar");
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty(DEPRECATED_SCHEMA, "complex2complex", value);
        assertEquals(value, doc.getProperty(DEPRECATED_SCHEMA, deprecatedProperty));
        assertEquals(value, doc.getProperty(DEPRECATED_SCHEMA, fallbackProperty));

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        // 3 logs:
        // - a set is done on scalar property
        // - a set is done on its container
        // - a get which retrieve the scalar (that's why there's no 4th log on scalar property)
        assertEquals(3, events.size());
        assertEquals(String.format(SET_VALUE_FALLBACK_PARENT_LOG, deprecatedProperty + "/scalar", DEPRECATED_SCHEMA,
                deprecatedProperty, fallbackProperty + "/scalar"), events.get(0));
        assertEquals(String.format(SET_VALUE_FALLBACK_LOG, deprecatedProperty, DEPRECATED_SCHEMA, fallbackProperty),
                events.get(1));
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, deprecatedProperty, DEPRECATED_SCHEMA, fallbackProperty),
                events.get(2));
    }

    @Test
    public void testSetDeprecatedComplexPropertyValueWithFallbackOnComplex() throws Exception {
        String deprecatedProperty = "complex2complex";
        String fallbackProperty = "complexfallback";
        Serializable value = (Serializable) Collections.singletonMap("scalar", "test scalar");
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue(deprecatedProperty, value);
        assertEquals(value, doc.getPropertyValue(deprecatedProperty));
        assertEquals(value, doc.getPropertyValue(fallbackProperty));

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        // 3 logs:
        // - a set is done on scalar property
        // - a set is done on its container
        // - a get which retrieve the scalar (that's why there's no 4th log on scalar property)
        assertEquals(3, events.size());
        assertEquals(String.format(SET_VALUE_FALLBACK_PARENT_LOG, deprecatedProperty + "/scalar", DEPRECATED_SCHEMA,
                deprecatedProperty, fallbackProperty + "/scalar"), events.get(0));
        assertEquals(String.format(SET_VALUE_FALLBACK_LOG, deprecatedProperty, DEPRECATED_SCHEMA, fallbackProperty),
                events.get(1));
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, deprecatedProperty, DEPRECATED_SCHEMA, fallbackProperty),
                events.get(2));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetDeprecatedComplexPropertiesWithFallbackOnComplex() throws Exception {
        String deprecatedProperty = "complex2complex";
        String fallbackProperty = "complexfallback";
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties(DEPRECATED_SCHEMA,
                Collections.singletonMap("complex2complex", Collections.singletonMap("scalar", "test scalar")));
        Map<String, Object> properties = doc.getProperties(DEPRECATED_SCHEMA);
        assertEquals("test scalar",
                ((Map<String, Serializable>) properties.get("complex2complex")).get("scalar").toString());
        assertEquals("test scalar",
                ((Map<String, Serializable>) properties.get("complexfallback")).get("scalar").toString());

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        // 3 logs:
        // - a set is done on scalar property
        // - a set is done on its container
        // - a get which retrieve the scalar (that's why there's no 4th log on scalar property)
        assertEquals(3, events.size());
        assertEquals(String.format(SET_VALUE_FALLBACK_PARENT_LOG, deprecatedProperty + "/scalar", DEPRECATED_SCHEMA,
                deprecatedProperty, fallbackProperty + "/scalar"), events.get(0));
        assertEquals(String.format(SET_VALUE_FALLBACK_LOG, deprecatedProperty, DEPRECATED_SCHEMA, fallbackProperty),
                events.get(1));
        assertEquals(String.format(GET_VALUE_FALLBACK_LOG, deprecatedProperty, DEPRECATED_SCHEMA, fallbackProperty),
                events.get(2));
    }

    @Test
    public void testSetScalarOnDeprecatedComplexPropertyWithFallbackOnComplex() throws Exception {
        testProperty("complex2complex/scalar", "complexfallback/scalar", "test scalar");
    }

    @Test
    public void testSetScalarOnDeprecatedComplexPropertyValueWithFallbackOnComplex() throws Exception {
        testPropertyValue("complex2complex/scalar", "complexfallback/scalar", "test scalar");
    }

    /**
     * @param deprecatedProperty deprecated property path to test
     * @param value the value to set, depending on property field type
     */
    protected void testProperty(String deprecatedProperty, Object value) throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty(DEPRECATED_SCHEMA, deprecatedProperty, value);
        // As property is just deprecated we still store the value
        if (value instanceof Object[]) {
            assertArrayEquals((Object[]) value, (Object[]) doc.getProperty(DEPRECATED_SCHEMA, deprecatedProperty));
        } else {
            assertEquals(value, doc.getProperty(DEPRECATED_SCHEMA, deprecatedProperty));
        }

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(deprecatedProperty, events);
    }

    /**
     * @param deprecatedProperty deprecated property path to test
     * @param value the value to set, depending on property field type
     */
    protected void testPropertyValue(String deprecatedProperty, Serializable value) throws Exception {
        String xpath = DEPRECATED_SCHEMA + ':' + deprecatedProperty;

        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue(xpath, value);
        if (value instanceof Object[]) {
            assertArrayEquals((Object[]) value, (Object[]) doc.getPropertyValue(xpath));
        } else {
            assertEquals(value, doc.getPropertyValue(xpath));
        }

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(deprecatedProperty, events);
        logCaptureResult.clear();

        // Test also with a xpath without schema
        doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue(deprecatedProperty, value);
        if (value instanceof Object[]) {
            assertArrayEquals((Object[]) value, (Object[]) doc.getPropertyValue(deprecatedProperty));
        } else {
            assertEquals(value, doc.getPropertyValue(deprecatedProperty));
        }

        logCaptureResult.assertHasEvent();
        events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(deprecatedProperty, events);
    }

    /**
     * @param deprecatedProperty deprecated property path to test
     * @param value the value to set, depending on property field type
     */
    @SuppressWarnings("unchecked")
    protected void testProperties(String deprecatedProperty, Object value) throws Exception {
        Map<String, Object> map;
        // We define that deprecated property is the last property in path
        int i = deprecatedProperty.indexOf('/');
        boolean hasParent = i != -1;
        String parent = hasParent ? deprecatedProperty.substring(0, i) : null;
        String child = hasParent ? deprecatedProperty.substring(i + 1) : null;
        if (hasParent) {
            map = Collections.singletonMap(parent, Collections.singletonMap(child, value));
        } else {
            map = Collections.singletonMap(deprecatedProperty, value);
        }

        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties(DEPRECATED_SCHEMA, map);
        Object actualValue;
        if (hasParent) {
            actualValue = ((Map<String, Serializable>) doc.getProperties(DEPRECATED_SCHEMA).get(parent)).get(child);
        } else {
            actualValue = doc.getProperties(DEPRECATED_SCHEMA).get(deprecatedProperty);
        }
        if (value instanceof Object[]) {
            assertArrayEquals((Object[]) value, (Object[]) actualValue);
        } else {
            assertEquals(value, actualValue);
        }

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(deprecatedProperty, events);
    }

    /**
     * @param deprecatedProperty deprecated property path to test
     * @param fallbackProperty fallback property
     * @param value the value to set, depending on property field type
     */
    protected void testProperty(String deprecatedProperty, String fallbackProperty, Object value) throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty(DEPRECATED_SCHEMA, deprecatedProperty, value);
        // As property is just deprecated we still store the value
        if (value instanceof Object[]) {
            assertArrayEquals((Object[]) value, (Object[]) doc.getProperty(DEPRECATED_SCHEMA, deprecatedProperty));
            assertArrayEquals((Object[]) value, (Object[]) doc.getProperty(DEPRECATED_SCHEMA, fallbackProperty));
        } else {
            assertEquals(value, doc.getProperty(DEPRECATED_SCHEMA, deprecatedProperty));
            assertEquals(value, doc.getProperty(DEPRECATED_SCHEMA, fallbackProperty));
        }

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(deprecatedProperty, fallbackProperty, events);
    }

    /**
     * @param deprecatedProperty deprecated property path to test
     * @param fallbackProperty fallback property
     * @param value the value to set, depending on property field type
     */
    protected void testPropertyValue(String deprecatedProperty, String fallbackProperty, Serializable value)
            throws Exception {
        String xpath = DEPRECATED_SCHEMA + ':' + deprecatedProperty;
        String fallbackXPath = DEPRECATED_SCHEMA + ':' + fallbackProperty;

        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue(xpath, value);
        if (value instanceof Object[]) {
            assertArrayEquals((Object[]) value, (Object[]) doc.getPropertyValue(xpath));
            assertArrayEquals((Object[]) value, (Object[]) doc.getPropertyValue(fallbackXPath));
        } else {
            assertEquals(value, doc.getPropertyValue(xpath));
            assertEquals(value, doc.getPropertyValue(fallbackXPath));
        }

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(deprecatedProperty, fallbackProperty, events);
        logCaptureResult.clear();

        // Test also with a xpath without schema
        doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue(deprecatedProperty, value);
        if (value instanceof Object[]) {
            assertArrayEquals((Object[]) value, (Object[]) doc.getPropertyValue(deprecatedProperty));
            assertArrayEquals((Object[]) value, (Object[]) doc.getPropertyValue(fallbackProperty));
        } else {
            assertEquals(value, doc.getPropertyValue(deprecatedProperty));
            assertEquals(value, doc.getPropertyValue(fallbackProperty));
        }

        logCaptureResult.assertHasEvent();
        events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(deprecatedProperty, fallbackProperty, events);
    }

    /**
     * @param deprecatedProperty deprecated property path to test
     * @param fallbackProperty fallback property
     * @param value the value to set, depending on property field type
     */
    @SuppressWarnings("unchecked")
    protected void testProperties(String deprecatedProperty, String fallbackProperty, Object value) throws Exception {
        Map<String, Object> map;
        // We define that deprecated property is the last property in path
        int i = deprecatedProperty.indexOf('/');
        boolean hasParent = i != -1;
        String parent = hasParent ? deprecatedProperty.substring(0, i) : null;
        String child = hasParent ? deprecatedProperty.substring(i + 1) : null;
        if (hasParent) {
            map = Collections.singletonMap(parent, Collections.singletonMap(child, value));
        } else {
            map = Collections.singletonMap(deprecatedProperty, value);
        }

        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties(DEPRECATED_SCHEMA, map);
        Object actualValue;
        if (hasParent) {
            actualValue = ((Map<String, Serializable>) doc.getProperties(DEPRECATED_SCHEMA).get(parent)).get(child);
        } else {
            actualValue = doc.getProperties(DEPRECATED_SCHEMA).get(deprecatedProperty);
        }
        if (value instanceof Object[]) {
            assertArrayEquals((Object[]) value, (Object[]) actualValue);
        } else {
            assertEquals(value, actualValue);
        }

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(deprecatedProperty, fallbackProperty, events);
    }

    protected void assertLogMessages(String deprecatedProperty, List<String> events) {
        // 2 logs:
        // - a set while setting the value
        // - a get to assert property
        assertEquals(2, events.size());
        int index = deprecatedProperty.indexOf("Dep/");
        if (index == -1) {
            assertEquals(String.format(SET_VALUE_LOG, deprecatedProperty, DEPRECATED_SCHEMA), events.get(0));
            assertEquals(String.format(GET_VALUE_LOG, deprecatedProperty, DEPRECATED_SCHEMA), events.get(1));
        } else {
            String deprecatedParent = deprecatedProperty.substring(0, index + 3);
            assertEquals(String.format(SET_VALUE_PARENT_LOG, deprecatedProperty, DEPRECATED_SCHEMA, deprecatedParent),
                    events.get(0));
            assertEquals(String.format(GET_VALUE_PARENT_LOG, deprecatedProperty, DEPRECATED_SCHEMA, deprecatedParent),
                    events.get(1));
        }
    }

    protected void assertLogMessages(String deprecatedProperty, String fallbackProperty, List<String> events) {
        // 2 logs:
        // - a set while setting the value
        // - a get to assert property
        assertEquals(2, events.size());
        int index = deprecatedProperty.indexOf('/');
        if (index == -1) {
            assertEquals(String.format(SET_VALUE_FALLBACK_LOG, deprecatedProperty, DEPRECATED_SCHEMA, fallbackProperty),
                    events.get(0));
            assertEquals(String.format(GET_VALUE_FALLBACK_LOG, deprecatedProperty, DEPRECATED_SCHEMA, fallbackProperty),
                    events.get(1));
        } else {
            String deprecatedParentProperty = deprecatedProperty.substring(0, index);
            assertEquals(String.format(SET_VALUE_FALLBACK_PARENT_LOG, deprecatedProperty, DEPRECATED_SCHEMA,
                    deprecatedParentProperty, fallbackProperty), events.get(0));
            assertEquals(String.format(GET_VALUE_FALLBACK_PARENT_LOG, deprecatedProperty, DEPRECATED_SCHEMA,
                    deprecatedParentProperty, fallbackProperty), events.get(1));
        }
    }

}
