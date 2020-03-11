/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *      Andre Justo
 */
package org.nuxeo.runtime.services.config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, LogCaptureFeature.class })
public class TestConfigurationService {

    @Inject
    public ConfigurationService cs;

    @Inject
    protected HotDeployer hotDeployer;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Test
    public void testExtensionPoint() throws Exception {
        // Assert properties don't exist
        assertFalse(cs.getString("nuxeo.test.dummyBooleanProperty").isPresent());
        assertFalse(cs.getString("nuxeo.test.anotherDummyBooleanProperty").isPresent());
        assertFalse(cs.getString("nuxeo.test.dummyStringProperty").isPresent());
        // Deploy contribution with properties
        hotDeployer.deploy("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml");
        assertTrue(cs.getString("nuxeo.test.dummyBooleanProperty").isPresent());
        assertTrue(cs.getString("nuxeo.test.anotherDummyBooleanProperty").isPresent());
        assertTrue(cs.getString("nuxeo.test.dummyStringProperty").isPresent());
        assertFalse(cs.getString("nuxeo.test.invalidDummyProperty").isPresent());
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml")
    public void testProperties() {
        assertTrue(cs.getBoolean("nuxeo.test.dummyBooleanProperty").orElseThrow(AssertionError::new));
        assertFalse(cs.getBoolean("nuxeo.test.anotherDummyBooleanProperty").orElseThrow(AssertionError::new));
        assertEquals("dummyValue", cs.getString("nuxeo.test.dummyStringProperty", null));
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml")
    public void testOverride() throws Exception {
        // Assert property has overridden value
        assertEquals("dummyValue", cs.getString("nuxeo.test.dummyStringProperty", null));
        // Assert property don't exist
        assertFalse(cs.getString("nuxeo.test.overrideContribDummyProperty").isPresent());
        // Deploy another contrib with a new property and override existing properties
        hotDeployer.deploy("org.nuxeo.runtime.test.tests:configuration-override-contrib.xml");
        // Assert new property was added
        assertEquals("overrideContrib", cs.getString("nuxeo.test.overrideContribDummyProperty", null));
        // Assert framework property does not takes precedence
        assertEquals("dummyStringValueOverridden", cs.getString("nuxeo.test.dummyStringProperty", null));
        Framework.getProperties().setProperty("nuxeo.test.dummyStringProperty", "anotherDummyValue");
        assertEquals("dummyStringValueOverridden", cs.getString("nuxeo.test.dummyStringProperty", null));
        Framework.getProperties().remove("nuxeo.test.dummyStringProperty");
        // Assert old properties have overridden values
        assertEquals("dummyStringValueOverridden", cs.getString("nuxeo.test.dummyStringProperty", null));
        assertFalse(cs.getBoolean("nuxeo.test.dummyBooleanProperty").orElseThrow(AssertionError::new));

        // Undeploy contrib
        hotDeployer.undeploy("org.nuxeo.runtime.test.tests:configuration-override-contrib.xml");
        // Assert property was removed
        assertFalse(cs.getString("nuxeo.test.overrideContribDummyProperty").isPresent());
        // Assert overridden values were restored
        assertEquals("dummyValue", cs.getString("nuxeo.test.dummyStringProperty", null));
        assertTrue(cs.getBoolean("nuxeo.test.dummyBooleanProperty").orElseThrow(AssertionError::new));
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN")
    public void testCompatWarn() throws Exception {
        Framework.getProperties().setProperty("nuxeo.test.dummyStringProperty", "anotherDummyValue");
        assertEquals(0, Framework.getRuntime().getMessageHandler().getWarnings().size());
        hotDeployer.deploy("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml");

        // The deprecation warning messages should not be appended to the runtime, but logged by the DeprecationLogger class
        assertEquals(0, Framework.getRuntime().getMessageHandler().getWarnings().size());

        List<String> caughtEvents = logCaptureResult.getCaughtEventMessages();
        assertEquals(1, caughtEvents.size());
        String message = "Since version 7.4: Property 'nuxeo.test.dummyStringProperty' should now be contributed to "
                + "extension point 'org.nuxeo.runtime.ConfigurationService', using target 'configuration'";
        assertEquals(message, caughtEvents.get(0));

        Framework.getProperties().remove("nuxeo.test.dummyStringProperty");
    }

    /**
     * @since 10.3
     */
    @Test
    @Deploy("org.nuxeo.runtime.test.tests:configuration-another-test-contrib.xml")
    public void testMerge() throws Exception {
        assertEquals("dummyValue", cs.getString("nuxeo.test.listStringProperty", null));
        assertEquals("anotherDummyValue", cs.getString("nuxeo.test.notListStringProperty", null));
        assertEquals("newValue", cs.getString("nuxeo.test.listStringPropertytoBeReplaced", null));
        // Deploy another contrib merging existing and overriding properties
        hotDeployer.deploy("org.nuxeo.runtime.test.tests:configuration-merge-contrib.xml");
        // Assert new property was merged
        assertEquals("dummyValue,mergedValue,anotherMergedValue", cs.getString("nuxeo.test.listStringProperty", null));
        // Assert new property was not merged
        assertEquals("notMergedValue", cs.getString("nuxeo.test.notListStringProperty", null));
        assertEquals("newValue,thisPropertyWasOverridenButIsStillAList",
                cs.getString("nuxeo.test.listStringPropertytoBeReplaced", null));
    }

    /**
     * @since 10.3
     */
    @Test
    @Deploy("org.nuxeo.runtime.test.tests:configuration-namespace-contrib.xml")
    public void testByNamespace() {
        assertEquals(3, cs.getProperties("nuxeo.namespace.test").size());
        assertEquals(2, cs.getProperties("nuxeo.namespace.anothertest").size());
        assertEquals(7, cs.getProperties("nuxeo.namespace").size());
        assertEquals(2, cs.getProperties("nuxeo.namespace.yetanothertest").size());
        Serializable pouet = cs.getProperties("nuxeo.namespace.yetanothertest").get("pouet");
        assertTrue(pouet instanceof String);
        assertEquals("bar", pouet);
        assertTrue(cs.getProperties("nuxeo.namespace.test.dummyStringProperty").isEmpty());
        Serializable truc = cs.getProperties("nuxeo.namespace.yetanothertest").get("truc");
        assertTrue(truc instanceof String[]);
        String[] trucArray = (String[]) truc;
        assertEquals(2, trucArray.length);
        assertArrayEquals(new String[] { "foo", "bar" }, trucArray);
        assertTrue(cs.getProperties("nuxeo.namespace.test.dummyStringProperty").isEmpty());
        assertTrue(cs.getProperties("nuxeo.namespace.t").isEmpty());
        assertTrue(cs.getProperties("nuxeo.namespace.te").isEmpty());
        assertTrue(cs.getProperties("nuxeo.namespace.tes").isEmpty());
        try {
            assertTrue(cs.getProperties("nuxeo.namespace.test.").isEmpty());
            fail("Should not be able with invalid namspace");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    /**
     * @since 10.3
     */
    @Test
    @Deploy("org.nuxeo.runtime.test.tests:configuration-namespace-contrib.xml")
    public void testToJson() throws Exception {
        String expected = "{\n" + //
                "   \"namespace\": {\n" + //
                "      \"test\": {\n" + //
                "         \"anotherDummyBooleanProperty\": \"false\",\n" + //
                "         \"dummyBooleanProperty\": \"true\",\n" + //
                "         \"dummyStringProperty\": [\n" + //
                "            \"dummyValue\",\n" + //
                "            \"anotherDummyValue\"\n" + //
                "         ]\n" + //
                "      },\n" + //
                "      \"anothertest\": {\n" + //
                "         \"dummyBooleanProperty\": \"true\",\n" + //
                "         \"pouet\": \"toto\"\n" + //
                "      },\n" + //
                "      \"yetanothertest\": {\n" + //
                "         \"pouet\": \"bar\",\n" + //
                "         \"truc\": [\n" + //
                "            \"foo\",\n" + //
                "            \"bar\"\n" + //
                "         ]\n" + //
                "      }\n" + //
                "   }\n" + //
                "}";
        String json = cs.getPropertiesAsJson("nuxeo");
        JSONAssert.assertEquals(expected, json, false);
    }

    /**
     * @since 11.1
     */
    @Test
    @Deploy("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml")
    public void testGetInteger() {
        assertEquals(10, cs.getInteger("nuxeo.test.dummyIntegerProperty", 0));
        assertFalse(cs.getInteger("nuxeo.test.dummyStringProperty").isPresent());
    }

    /**
     * @since 11.1
     */
    @Test
    @Deploy("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml")
    public void testGetLong() {
        assertEquals(20, cs.getLong("nuxeo.test.dummyLongProperty", 0));
        assertFalse(cs.getLong("nuxeo.test.dummyStringProperty").isPresent());
    }

    /**
     * @since 11.1
     */
    @Test
    @Deploy("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml")
    public void testGetDuration() {
        assertEquals(Duration.ofMinutes(30), cs.getDuration("nuxeo.test.dummyDurationProperty", null));
        assertFalse(cs.getDuration("nuxeo.test.dummyStringProperty").isPresent());
    }

    /**
     * @since 11.1
     */
    @Test
    @Deploy("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml")
    public void testBlankPropertyValue() {
        assertBlankPropertyValue("nuxeo.test.dummyEmptyProperty");
        assertBlankPropertyValue("nuxeo.test.dummyBlankProperty");
    }

    protected void assertBlankPropertyValue(String key) {
        assertFalse(cs.getString(key).isPresent());
        assertFalse(cs.getInteger(key).isPresent());
        assertFalse(cs.getLong(key).isPresent());
        assertFalse(cs.getDuration(key).isPresent());
        assertFalse(cs.getBoolean(key).isPresent());
    }

    /**
     * @since 11.1
     */
    @Test
    @Deploy("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml")
    public void testIsBooleanTrue() {
        assertTrue(cs.isBooleanTrue("nuxeo.test.dummyBooleanProperty"));
        assertFalse(cs.isBooleanTrue("nuxeo.test.anotherDummyBooleanProperty"));
        assertFalse(cs.isBooleanTrue("nuxeo.test.dummyEmptyProperty"));
        assertFalse(cs.isBooleanTrue("nuxeo.test.dummyBlankProperty"));
        assertFalse(cs.isBooleanTrue("nuxeo.test.dummyStringProperty"));
    }

    /**
     * @since 11.1
     */
    @Test
    @Deploy("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml")
    public void testIsBooleanFalse() {
        assertFalse(cs.isBooleanFalse("nuxeo.test.dummyBooleanProperty"));
        assertTrue(cs.isBooleanFalse("nuxeo.test.anotherDummyBooleanProperty"));
        assertFalse(cs.isBooleanFalse("nuxeo.test.dummyEmptyProperty"));
        assertFalse(cs.isBooleanFalse("nuxeo.test.dummyBlankProperty"));
        assertFalse(cs.isBooleanFalse("nuxeo.test.dummyStringProperty"));
    }

}
