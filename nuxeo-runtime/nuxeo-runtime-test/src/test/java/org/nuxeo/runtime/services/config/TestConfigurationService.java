/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestConfigurationService {

    @Inject
    public ConfigurationService cs;

    @Inject
    protected HotDeployer hotDeployer;

    @Test
    public void testExtensionPoint() throws Exception {
        // Assert properties don't exist
        assertNull(cs.getProperty("nuxeo.test.dummyBooleanProperty"));
        assertNull(cs.getProperty("nuxeo.test.anotherDummyBooleanProperty"));
        assertNull(cs.getProperty("nuxeo.test.dummyStringProperty"));
        // Deploy contribution with properties
        hotDeployer.deploy("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml");
        assertNotNull(cs.getProperty("nuxeo.test.dummyBooleanProperty"));
        assertNotNull(cs.getProperty("nuxeo.test.anotherDummyBooleanProperty"));
        assertNotNull(cs.getProperty("nuxeo.test.dummyStringProperty"));
        assertNull(cs.getProperty("nuxeo.test.invalidDummyProperty"));
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml")
    public void testProperties() throws Exception {
        assertTrue(cs.isBooleanPropertyTrue("nuxeo.test.dummyBooleanProperty"));
        assertFalse(cs.isBooleanPropertyTrue("nuxeo.test.anotherDummyBooleanProperty"));
        assertEquals("dummyValue", cs.getProperty("nuxeo.test.dummyStringProperty"));
    }

    @Test
    @Deploy("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml")
    public void testOverride() throws Exception {
        // Assert property has overridden value
        assertEquals("dummyValue", cs.getProperty("nuxeo.test.dummyStringProperty"));
        // Assert property don't exist
        assertNull(cs.getProperty("nuxeo.test.overrideContribDummyProperty"));
        // Deploy another contrib with a new property and override existing properties
        hotDeployer.deploy("org.nuxeo.runtime.test.tests:configuration-override-contrib.xml");
        // Assert new property was added
        assertEquals("overrideContrib", cs.getProperty("nuxeo.test.overrideContribDummyProperty"));
        // Assert framework property does not takes precedence
        assertEquals("dummyStringValueOverridden", cs.getProperty("nuxeo.test.dummyStringProperty"));
        Framework.getProperties().setProperty("nuxeo.test.dummyStringProperty", "anotherDummyValue");
        assertEquals("dummyStringValueOverridden", cs.getProperty("nuxeo.test.dummyStringProperty"));
        Framework.getProperties().remove("nuxeo.test.dummyStringProperty");
        // Assert old properties have overridden values
        assertEquals("dummyStringValueOverridden", cs.getProperty("nuxeo.test.dummyStringProperty"));
        assertTrue(cs.isBooleanPropertyFalse("nuxeo.test.dummyBooleanProperty"));

        // Undeploy contrib
        hotDeployer.undeploy("org.nuxeo.runtime.test.tests:configuration-override-contrib.xml");
        // Assert property was removed
        assertNull(cs.getProperty("nuxeo.test.overrideContribDummyProperty"));
        // Assert overridden values were restored
        assertEquals("dummyValue", cs.getProperty("nuxeo.test.dummyStringProperty"));
        assertTrue(cs.isBooleanPropertyTrue("nuxeo.test.dummyBooleanProperty"));
    }

    @Test
    public void testCompatWarn() throws Exception {
        Framework.getProperties().setProperty("nuxeo.test.dummyStringProperty", "anotherDummyValue");
        assertEquals(0, Framework.getRuntime().getMessageHandler().getWarnings().size());
        // Deploy contribution with properties
        hotDeployer.deploy("org.nuxeo.runtime.test.tests:configuration-test-contrib.xml");
        assertEquals(1, Framework.getRuntime().getMessageHandler().getWarnings().size());
        assertEquals(
                "Property 'nuxeo.test.dummyStringProperty' should now be contributed to "
                        + "extension point 'org.nuxeo.runtime.ConfigurationService', using target 'configuration'",
                Framework.getRuntime().getMessageHandler().getWarnings().get(0));
        Framework.getProperties().remove("nuxeo.test.dummyStringProperty");
    }

    /**
     * @since 10.3
     */
    @Test
    @Deploy("org.nuxeo.runtime.test.tests:configuration-another-test-contrib.xml")
    public void testMerge() throws Exception {
        assertEquals("dummyValue", cs.getProperty("nuxeo.test.listStringProperty"));
        assertEquals("anotherDummyValue", cs.getProperty("nuxeo.test.notListStringProperty"));
        assertEquals("newValue", cs.getProperty("nuxeo.test.listStringPropertytoBeReplaced"));
        // Deploy another contrib merging existing and overriding properties
        hotDeployer.deploy("org.nuxeo.runtime.test.tests:configuration-merge-contrib.xml");
        // Assert new property was merged
        assertEquals("dummyValue,mergedValue,anotherMergedValue", cs.getProperty("nuxeo.test.listStringProperty"));
        // Assert new property was not merged
        assertEquals("notMergedValue", cs.getProperty("nuxeo.test.notListStringProperty"));
        assertEquals("newValue,thisPropertyWasOverridenButIsStillAList",
                cs.getProperty("nuxeo.test.listStringPropertytoBeReplaced"));
    }

    /**
     * @since 10.3
     */
    @Test
    @Deploy("org.nuxeo.runtime.test.tests:configuration-namespace-contrib.xml")
    public void testByNamespace() throws Exception {
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
        String expected =
                "{\"namespace\":{" +
                    "\"test\":{" +
                        "\"anotherDummyBooleanProperty\":\"false\"," +
                        "\"dummyBooleanProperty\":\"true\"," +
                        "\"dummyStringProperty\":[\"dummyValue\"," +
                        "\"anotherDummyValue\"]}," +
                    "\"anothertest\":{" +
                        "\"dummyBooleanProperty\":\"true\"," +
                        "\"pouet\":\"toto\"}," +
                    "\"yetanothertest\":{" +
                        "\"pouet\":\"bar\"," +
                        "\"truc\":[\"foo\",\"bar\"]}}}}";
        String json = cs.getPropertiesAsJson("nuxeo");
        JSONAssert.assertEquals(expected, json, false);
    }

}
