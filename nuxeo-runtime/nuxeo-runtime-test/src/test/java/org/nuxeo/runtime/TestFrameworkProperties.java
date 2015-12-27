/*
 * (C) Copyright 2013-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class })
public class TestFrameworkProperties {

    protected RuntimeService runtime = Framework.getRuntime();

    final String aValue = "myValue";

    final String aDefaultValue = "myDefaultValue";

    final String aParam = "myParam";

    final String aStrangeValue = "${\\my.strange/value}";

    final String aParamWithDot = "my.param.with.dot";

    final String aSystemValue = "mySystemValue";

    @Test
    public void testExpandVars() {
        List<String> testExpressions = new ArrayList<>();
        testExpressions.add("<myProp>" + aValue + "</myProp>");
        testExpressions.add("<myProp>${" + aParam + "}</myProp>");
        testExpressions.add("<myProp>${" + aParam + ":=" + aDefaultValue + "}</myProp>");

        // property undefined
        assertEquals("<myProp>" + aValue + "</myProp>", runtime.expandVars(testExpressions.get(0)));
        assertEquals("<myProp>${" + aParam + "}</myProp>", runtime.expandVars(testExpressions.get(1)));
        assertEquals("<myProp>" + aDefaultValue + "</myProp>", runtime.expandVars(testExpressions.get(2)));

        // system property
        System.setProperty(aParam, aSystemValue);
        assertEquals("<myProp>" + aValue + "</myProp>", runtime.expandVars(testExpressions.get(0)));
        assertEquals("<myProp>" + aSystemValue + "</myProp>", runtime.expandVars(testExpressions.get(1)));
        assertEquals("<myProp>" + aSystemValue + "</myProp>", runtime.expandVars(testExpressions.get(2)));

        // runtime property
        runtime.setProperty(aParam, aValue);
        assertEquals("<myProp>" + aValue + "</myProp>", runtime.expandVars(testExpressions.get(0)));
        assertEquals("<myProp>" + aValue + "</myProp>", runtime.expandVars(testExpressions.get(1)));
        assertEquals("<myProp>" + aValue + "</myProp>", runtime.expandVars(testExpressions.get(2)));
    }

    @Test
    public void testExpandVarsWithSpecialChars() {
        List<String> testExpressions = new ArrayList<>();
        testExpressions.add("<myProp>" + aStrangeValue + "</myProp>");
        testExpressions.add("<myProp>${" + aParamWithDot + "}</myProp>");
        testExpressions.add("<myProp>${" + aParamWithDot + ":=" + aDefaultValue + "}</myProp>");

        // property undefined
        assertEquals("<myProp>" + aStrangeValue + "</myProp>", runtime.expandVars(testExpressions.get(0)));
        assertEquals("<myProp>${" + aParamWithDot + "}</myProp>", runtime.expandVars(testExpressions.get(1)));
        assertEquals("<myProp>" + aDefaultValue + "</myProp>", runtime.expandVars(testExpressions.get(2)));

        // system property
        System.setProperty(aParamWithDot, aSystemValue);
        assertEquals("<myProp>" + aStrangeValue + "</myProp>", runtime.expandVars(testExpressions.get(0)));
        assertEquals("<myProp>" + aSystemValue + "</myProp>", runtime.expandVars(testExpressions.get(1)));
        assertEquals("<myProp>" + aSystemValue + "</myProp>", runtime.expandVars(testExpressions.get(2)));

        // runtime property
        runtime.setProperty(aParamWithDot, aStrangeValue);
        assertEquals("<myProp>" + aStrangeValue + "</myProp>", runtime.expandVars(testExpressions.get(0)));
        assertEquals("<myProp>" + aStrangeValue + "</myProp>", runtime.expandVars(testExpressions.get(1)));
        assertEquals("<myProp>" + aStrangeValue + "</myProp>", runtime.expandVars(testExpressions.get(2)));
    }

    @Test
    public void testIsBooleanPropertyTrueFalse() throws Exception {
        String booleanVar1 = "booleanVar1";
        String booleanVar2 = "booleanVar2";
        assertNull(runtime.getProperty(booleanVar1));
        assertNull(runtime.getProperty(booleanVar2));
        assertNull(System.getProperty(booleanVar1));
        assertNull(System.getProperty(booleanVar2));
        assertFalse(Framework.isBooleanPropertyTrue(booleanVar1));
        assertFalse(Framework.isBooleanPropertyTrue(booleanVar2));
        assertFalse(Framework.isBooleanPropertyFalse(booleanVar2));
        assertFalse(Framework.isBooleanPropertyFalse(booleanVar1));
        runtime.setProperty(booleanVar1, "true");
        runtime.setProperty(booleanVar2, "false");
        assertTrue(Framework.isBooleanPropertyTrue(booleanVar1));
        assertFalse(Framework.isBooleanPropertyTrue(booleanVar2));
        assertFalse(Framework.isBooleanPropertyFalse(booleanVar1));
        assertTrue(Framework.isBooleanPropertyFalse(booleanVar2));
        runtime.setProperty(booleanVar1, "false");
        runtime.setProperty(booleanVar2, "true");
        assertFalse(Framework.isBooleanPropertyTrue(booleanVar1));
        assertTrue(Framework.isBooleanPropertyTrue(booleanVar2));
        assertTrue(Framework.isBooleanPropertyFalse(booleanVar1));
        assertFalse(Framework.isBooleanPropertyFalse(booleanVar2));
    }

    @Test
    public void testIsDevModeSet() throws Exception {
        assertTrue(Framework.isInitialized());
        // check compat
        assertEquals("org.nuxeo.dev", Framework.NUXEO_DEV_SYSTEM_PROP);
        // make sure runtime prop is not set
        assertNull(runtime.getProperty(Framework.NUXEO_DEV_SYSTEM_PROP));
        runtime.setProperty(Framework.NUXEO_DEV_SYSTEM_PROP, "true");
        assertTrue(Framework.isDevModeSet());
        runtime.setProperty(Framework.NUXEO_DEV_SYSTEM_PROP, "");
        assertFalse(Framework.isDevModeSet());
        runtime.setProperty(Framework.NUXEO_DEV_SYSTEM_PROP, "false");
        assertFalse(Framework.isDevModeSet());
    }

    @After
    public void tearDown() {
        System.clearProperty(aParam);
        System.clearProperty(aParamWithDot);
    }

}
