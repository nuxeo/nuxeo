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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
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
@Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-documentmodel-removed-types-contrib.xml")
@LogCaptureFeature.FilterOn(logLevel = "ERROR", loggerName = TestDocumentModelWithRemovedProperty.LOGGER_NAME)
public class TestDocumentModelWithRemovedProperty {

    public static final String LOGGER_NAME = "org.nuxeo.ecm.core.api.model.impl.RemovedProperty";

    protected static final String REMOVED_SCHEMA = "removed";

    protected static final String GET_VALUE_LOG = "Property '%s' is marked as removed from '%s' schema, don't use it anymore. "
            + "Return null";

    protected static final String SET_VALUE_LOG = "Property '%s' is marked as removed from '%s' schema, don't use it anymore. "
            + "Do nothing";

    protected static final String GET_VALUE_FALLBACK_LOG = "Property '%s' is marked as removed from '%s' schema, don't use it anymore. "
            + "Return value from '%s'";

    protected static final String SET_VALUE_FALLBACK_LOG = "Property '%s' is marked as removed from '%s' schema, don't use it anymore. "
            + "Set value to fallback property '%s'";

    protected static final String GET_VALUE_PARENT_LOG = "Property '%s' is marked as removed from '%s' schema because property '%s' is marked as removed, don't use it anymore. "
            + "Return null";

    protected static final String SET_VALUE_PARENT_LOG = "Property '%s' is marked as removed from '%s' schema because property '%s' is marked as removed, don't use it anymore. "
            + "Do nothing";

    protected static final String GET_VALUE_FALLBACK_PARENT_LOG = "Property '%s' is marked as removed from '%s' schema because property '%s' is marked as removed, don't use it anymore. "
            + "Return value from '%s'";

    protected static final String SET_VALUE_FALLBACK_PARENT_LOG = "Property '%s' is marked as removed from '%s' schema because property '%s' is marked as removed, don't use it anymore. "
            + "Set value to fallback property '%s'";

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    // -----------------------------
    // Tests with removed properties
    // -----------------------------

    @Test
    public void testSetRemovedScalarProperty() throws Exception {
        testProperty("scalar", "test scalar");
    }

    @Test
    public void testSetRemovedScalarPropertyValue() throws Exception {
        testPropertyValue("scalar", "test scalar");
    }

    @Test
    public void testSetRemovedScalarProperties() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties(REMOVED_SCHEMA, Collections.singletonMap("scalar", "test scalar"));
        assertNull(doc.getProperties(REMOVED_SCHEMA).get("scalar"));
    }

    @Test
    public void testSetRemovedComplexProperty() throws Exception {
        testProperty("complexRem", Collections.singletonMap("scalar", "test scalar"));
    }

    @Test
    public void testSetRemovedComplexPropertyValue() throws Exception {
        testPropertyValue("complexRem", (Serializable) Collections.singletonMap("scalar", "test scalar"));
    }

    @Test
    public void testSetRemovedComplexProperties() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties(REMOVED_SCHEMA,
                Collections.singletonMap("complexRem", Collections.singletonMap("scalar", "test scalar")));
        assertNull(doc.getProperties(REMOVED_SCHEMA).get("complexRem"));
    }

    @Test
    public void testSetScalarOnRemovedComplexProperty() throws Exception {
        testProperty("complexRem/scalar", "test scalar");
    }

    @Test
    public void testSetScalarOnRemovedComplexPropertyValue() throws Exception {
        testPropertyValue("complexRem/scalar", "test scalar");
    }

    @Test
    public void testSetRemovedScalarOnComplexProperty() throws Exception {
        testProperty("complex/scalar", "test scalar");
    }

    @Test
    public void testSetRemovedScalarOnComplexPropertyValue() throws Exception {
        testPropertyValue("complex/scalar", "test scalar");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetRemovedScalarOnComplexProperties() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties(REMOVED_SCHEMA,
                Collections.singletonMap("complex", Collections.singletonMap("scalar", "test scalar")));
        assertNull(((Map<String, Serializable>) doc.getProperties(REMOVED_SCHEMA).get("complex")).get("scalar"));
    }

    // ----------------------------------------------------------------------------------
    // Tests previous behavior - no contribution to deprecation system still raise errors
    // ----------------------------------------------------------------------------------

    @Test
    public void testSetComplexPropertyRemovedFromSchemaWithoutRemovedContribution() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        // First test: if we try to set a property deleted from schema without contribution still leads to error
        try {
            doc.setProperty(REMOVED_SCHEMA, "deleted", null);
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("deleted", pnfe.getPath());
        }
        // Second test: the get
        try {
            doc.getProperty(REMOVED_SCHEMA, "deleted");
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("deleted", pnfe.getPath());
        }
        // Third test: set a property deleted from schema whose the last segment has a contribution to deprecation
        // system, this has to lead to an error (issue faced during development)
        // Here complexfallback/complexRem doesn't exist in schema definition and complexRem exists in deprecation
        // contribution
        try {
            doc.setProperty(REMOVED_SCHEMA, "complexfallback/complexRem", null);
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("complexfallback/complexRem", pnfe.getPath());
        }
        // Fourth test: the get in the same way
        try {
            doc.getProperty(REMOVED_SCHEMA, "complexfallback/complexRem");
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("complexfallback/complexRem", pnfe.getPath());
        }
    }

    @Test
    public void testSetComplexPropertyValueRemovedFromSchemaWithoutRemovedContribution() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        // First test: if we try to set a property deleted from schema without contribution still leads to error
        try {
            doc.setPropertyValue("removed:deleted", null);
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("removed:deleted", pnfe.getPath());
        }
        // Second test: the get
        try {
            doc.getPropertyValue("removed:deleted");
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("removed:deleted", pnfe.getPath());
        }
        // Third test: set a property deleted from schema whose the last segment has a contribution to deprecation
        // system, this has to lead to an error (issue faced during development)
        // Here complexfallback/complexRem doesn't exist in schema definition and complexRem exists in deprecation
        // contribution
        try {
            doc.setPropertyValue("removed:complexfallback/complexRem", null);
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("removed:complexfallback/complexRem", pnfe.getPath());
        }
        // Fourth test: the get in the same way
        try {
            doc.getPropertyValue("removed:complexfallback/complexRem");
        } catch (PropertyNotFoundException pnfe) {
            assertEquals("removed:complexfallback/complexRem", pnfe.getPath());
        }
    }

    // -------------------------------------------
    // Tests with removed properties with fallback
    // -------------------------------------------

    @Test
    public void testSetRemovedScalarPropertyWithFallbackOnScalar() throws Exception {
        testProperty("scalar2scalar", "scalarfallback", "test scalar");
    }

    @Test
    public void testSetRemovedScalarPropertyValueWithFallbackOnScalar() throws Exception {
        testPropertyValue("scalar2scalar", "scalarfallback", "test scalar");
    }

    @Test
    public void testSetRemovedScalarPropertiesWithFallbackOnScalar() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties(REMOVED_SCHEMA, Collections.singletonMap("scalar2scalar", "test scalar"));
        assertEquals("test scalar", doc.getProperties(REMOVED_SCHEMA).get("scalarfallback").toString());
    }

    @Test
    public void testSetRemovedScalarPropertyWithFallbackOnComplex() throws Exception {
        testProperty("scalar2complex", "complexfallback/scalar", "test scalar");
    }

    @Test
    public void testSetRemovedScalarPropertyValueWithFallbackOnComplex() throws Exception {
        testPropertyValue("scalar2complex", "complexfallback/scalar", "test scalar");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetRemovedScalarPropertiesWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties(REMOVED_SCHEMA, Collections.singletonMap("scalar2complex", "test scalar"));
        assertEquals("test scalar",
                ((Map<String, Serializable>) doc.getProperties(REMOVED_SCHEMA).get("complexfallback")).get("scalar")
                                                                                                      .toString());
    }

    @Test
    public void testSetRemovedComplexPropertyWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty(REMOVED_SCHEMA, "complex2complex", Collections.singletonMap("scalar", "test scalar"));
        assertEquals("test scalar", doc.getProperty(REMOVED_SCHEMA, "complex2complex/scalar"));
        assertEquals("test scalar", doc.getProperty(REMOVED_SCHEMA, "complexfallback/scalar"));
    }

    @Test
    public void testSetRemovedComplexPropertyValueWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue("removed:complex2complex",
                (Serializable) Collections.singletonMap("scalar", "test scalar"));
        assertEquals("test scalar", doc.getPropertyValue("removed:complex2complex/scalar"));
        assertEquals("test scalar", doc.getPropertyValue("removed:complexfallback/scalar"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetRemovedComplexPropertiesWithFallbackOnComplex() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties(REMOVED_SCHEMA,
                Collections.singletonMap("complex2complex", Collections.singletonMap("scalar", "test scalar")));
        assertEquals("test scalar",
                ((Map<String, Serializable>) doc.getProperties(REMOVED_SCHEMA).get("complexfallback")).get("scalar")
                                                                                                      .toString());
    }

    @Test
    public void testSetScalarOnRemovedComplexPropertyWithFallbackOnComplex() throws Exception {
        testProperty("complex2complex/scalar", "complexfallback/scalar", "test scalar");
    }

    @Test
    public void testSetScalarOnRemovedComplexPropertyValueWithFallbackOnComplex() throws Exception {
        testPropertyValue("complex2complex/scalar", "complexfallback/scalar", "test scalar");
    }

    @Test
    public void testSetRemovedScalarOnListPropertyWithFallbackInsideList() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        // First create a valid object
        doc.setProperty(REMOVED_SCHEMA, "list",
                Collections.singletonList(Collections.singletonMap("scalar", "test scalar")));
        // Second try to set the removed property inside the item
        doc.setProperty(REMOVED_SCHEMA, "list/0/renamed", "test scalar 2");
        assertEquals("test scalar 2", doc.getProperty(REMOVED_SCHEMA, "list/0/scalar"));
        assertEquals("test scalar 2", doc.getProperty(REMOVED_SCHEMA, "list/0/renamed"));
    }

    @Test
    public void testSetRemovedScalarOnListPropertyValueWithFallbackInsideList() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        // First create a valid object
        doc.setProperty(REMOVED_SCHEMA, "list",
                Collections.singletonList(Collections.singletonMap("scalar", "test scalar")));
        // Second try to set the removed property inside the item
        doc.setPropertyValue("removed:list/0/renamed", "test scalar 2");
        assertEquals("test scalar 2", doc.getPropertyValue("removed:list/0/scalar"));
        assertEquals("test scalar 2", doc.getPropertyValue("removed:list/0/renamed"));
    }

    @Test
    public void testSetRemovedScalarOnListPropertiesWithFallbackInsideList() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperties(REMOVED_SCHEMA, Collections.singletonMap("list",
                Collections.singletonList(Collections.singletonMap("renamed", "test scalar"))));
        assertEquals("test scalar", doc.getPropertyValue("removed:list/0/scalar"));
        assertEquals("test scalar", doc.getPropertyValue("removed:list/0/renamed"));
    }

    // -------------------------------------
    // Tests with removed properties on blob
    // -------------------------------------

    @Test
    public void testSetRemovedScalarPropertyWithFallbackOnBlob() throws Exception {
        String removedProperty = "blobnameRem";
        String fallbackProperty = "blobfallback/name";
        String value = "test filename";

        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        // we assume blob already exists when we set a property removed from schema definition with a fallback on blob
        doc.setProperty(REMOVED_SCHEMA, "blobfallback", Blobs.createBlob("test blob"));
        doc.setProperty(REMOVED_SCHEMA, removedProperty, value);
        // When fallback is present return value from fallback
        assertEquals(value, doc.getProperty(REMOVED_SCHEMA, removedProperty));
        assertEquals(value, doc.getProperty(REMOVED_SCHEMA, fallbackProperty));

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(removedProperty, fallbackProperty, events);
    }

    @Test
    public void testSetRemovedScalarPropertyValueWithFallbackOnBlob() throws Exception {
        String removedProperty = "blobnameRem";
        String fallbackProperty = "blobfallback/name";
        String value = "test filename";
        String xpath = "removed:" + removedProperty;
        String fallbackXPath = "removed:" + fallbackProperty;

        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        // we assume blob already exists when we set a property removed from schema definition with a fallback on blob
        doc.setProperty(REMOVED_SCHEMA, "blobfallback", Blobs.createBlob("test blob"));
        doc.setPropertyValue(xpath, value);
        assertEquals(value, doc.getPropertyValue(xpath));
        assertEquals(value, doc.getPropertyValue(fallbackXPath));

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(removedProperty, fallbackProperty, events);
        logCaptureResult.clear();

        // Test also with a xpath without schema
        doc = new DocumentModelImpl("/", "doc", "File");
        // we assume blob already exists when we set a property removed from schema definition with a fallback on blob
        doc.setProperty(REMOVED_SCHEMA, "blobfallback", Blobs.createBlob("test blob"));
        doc.setPropertyValue(removedProperty, value);
        assertEquals(value, doc.getPropertyValue(removedProperty));
        assertEquals(value, doc.getPropertyValue(fallbackProperty));

        logCaptureResult.assertHasEvent();
        events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(removedProperty, fallbackProperty, events);
    }

    @Test
    public void testSetRemovedScalarPropertiesWithFallbackOnBlob() {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        // compat on set not work alright if Blob is null in the first place (cannot set value on null blob), still
        // works alright on get:
        assertNull(doc.getPropertyValue("file:filename"));
        assertNull(doc.getPropertyValue("file:content"));

        // fill value
        StringBlob blob = new StringBlob("test content");
        blob.setFilename("first filename");
        doc.setPropertyValue("file:content", blob);
        assertEquals("first filename", ((Blob) doc.getPropertyValue("file:content")).getFilename());
        // check property retrieval, failing because of phantom props
        // assertEquals("first filename", doc.getProperty("file:content/name").getValue());
        assertNull(doc.getProperty("file:content/name").getValue());
        assertTrue(doc.getProperty("file:content/name").isPhantom());
        // check xpath retrieval, failing similarly
        // assertEquals("first filename", doc.getPropertyValue("file:content/name"));
        assertNull(doc.getPropertyValue("file:content/name"));
        // check compat retrieval
        assertNull(doc.getPropertyValue("file:filename"));

        // check compat
        doc.setPropertyValue("file:filename", "test filename");
        assertEquals("test filename", ((Blob) doc.getPropertyValue("file:content")).getFilename());
        // check property retrieval
        assertEquals("test filename", doc.getProperty("file:content/name").getValue());
        // check xpath retrieval
        assertEquals("test filename", doc.getPropertyValue("file:content/name"));
        // check compat retrieval
        assertEquals("test filename", doc.getPropertyValue("file:filename"));
    }

    /**
     * @param removedProperty removed property path to test
     * @param value the value to set, depending on property field type
     */
    protected void testProperty(String removedProperty, Object value) throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty(REMOVED_SCHEMA, removedProperty, value);
        assertNull(doc.getProperty(REMOVED_SCHEMA, removedProperty));

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(removedProperty, events);
    }

    /**
     * @param removedProperty removed property path to test
     * @param value the value to set, depending on property field type
     */
    protected void testPropertyValue(String removedProperty, Serializable value) throws Exception {
        String xpath = "removed:" + removedProperty;

        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue(xpath, value);
        assertNull(doc.getPropertyValue(xpath));

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(removedProperty, events);
        logCaptureResult.clear();

        // Test also with a xpath without schema
        doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue(removedProperty, value);
        assertNull(doc.getPropertyValue(removedProperty));

        logCaptureResult.assertHasEvent();
        events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(removedProperty, events);
    }

    /**
     * @param removedProperty removed property path to test
     * @param fallbackProperty fallback property
     * @param value the value to set, depending on property field type
     */
    protected void testProperty(String removedProperty, String fallbackProperty, Object value) throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setProperty(REMOVED_SCHEMA, removedProperty, value);
        // When fallback is present return value from fallback
        assertEquals(value, doc.getProperty(REMOVED_SCHEMA, removedProperty));
        assertEquals(value, doc.getProperty(REMOVED_SCHEMA, fallbackProperty));

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(removedProperty, fallbackProperty, events);
    }

    /**
     * @param removedProperty removed property path to test
     * @param fallbackProperty fallback property
     * @param value the value to set, depending on property field type
     */
    protected void testPropertyValue(String removedProperty, String fallbackProperty, Serializable value)
            throws Exception {
        String xpath = "removed:" + removedProperty;
        String fallbackXPath = "removed:" + fallbackProperty;

        DocumentModel doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue(xpath, value);
        assertEquals(value, doc.getPropertyValue(xpath));
        assertEquals(value, doc.getPropertyValue(fallbackXPath));

        logCaptureResult.assertHasEvent();
        List<String> events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(removedProperty, fallbackProperty, events);
        logCaptureResult.clear();

        // Test also with a xpath without schema
        doc = new DocumentModelImpl("/", "doc", "File");
        doc.setPropertyValue(removedProperty, value);
        assertEquals(value, doc.getPropertyValue(removedProperty));
        assertEquals(value, doc.getPropertyValue(fallbackProperty));

        logCaptureResult.assertHasEvent();
        events = logCaptureResult.getCaughtEventMessages();
        assertLogMessages(removedProperty, fallbackProperty, events);
    }

    protected void assertLogMessages(String removedProperty, List<String> events) {
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        int index = removedProperty.indexOf("Rem/");
        if (index == -1) {
            assertEquals(String.format(GET_VALUE_LOG, removedProperty, REMOVED_SCHEMA), events.get(0));
            assertEquals(String.format(SET_VALUE_LOG, removedProperty, REMOVED_SCHEMA), events.get(1));
            assertEquals(String.format(GET_VALUE_LOG, removedProperty, REMOVED_SCHEMA), events.get(2));
        } else {
            String removedParent = removedProperty.substring(0, index + 3);
            assertEquals(String.format(GET_VALUE_PARENT_LOG, removedProperty, REMOVED_SCHEMA, removedParent),
                    events.get(0));
            assertEquals(String.format(SET_VALUE_PARENT_LOG, removedProperty, REMOVED_SCHEMA, removedParent),
                    events.get(1));
            assertEquals(String.format(GET_VALUE_PARENT_LOG, removedProperty, REMOVED_SCHEMA, removedParent),
                    events.get(2));
        }
    }

    protected void assertLogMessages(String removedProperty, String fallbackProperty, List<String> events) {
        // 3 logs:
        // - a set is basically one get then one set if value are different
        // - a get to assert property
        assertEquals(3, events.size());
        int index = removedProperty.indexOf('/');
        if (index == -1) {
            assertEquals(String.format(GET_VALUE_FALLBACK_LOG, removedProperty, REMOVED_SCHEMA, fallbackProperty),
                    events.get(0));
            assertEquals(String.format(SET_VALUE_FALLBACK_LOG, removedProperty, REMOVED_SCHEMA, fallbackProperty),
                    events.get(1));
            assertEquals(String.format(GET_VALUE_FALLBACK_LOG, removedProperty, REMOVED_SCHEMA, fallbackProperty),
                    events.get(2));
        } else {
            String removedParentParent = removedProperty.substring(0, index);
            assertEquals(String.format(GET_VALUE_FALLBACK_PARENT_LOG, removedProperty, REMOVED_SCHEMA,
                    removedParentParent, fallbackProperty), events.get(0));
            assertEquals(String.format(SET_VALUE_FALLBACK_PARENT_LOG, removedProperty, REMOVED_SCHEMA,
                    removedParentParent, fallbackProperty), events.get(1));
            assertEquals(String.format(GET_VALUE_FALLBACK_PARENT_LOG, removedProperty, REMOVED_SCHEMA,
                    removedParentParent, fallbackProperty), events.get(2));
        }
    }

}
