/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.runtime;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @since 5.7
 */
public class TestFrameworkProperties extends NXRuntimeTestCase {

    @Test
    public void testExpandVars() {
        // do not define prop
        assertEquals("<myProp>myValue</myProp>",
                runtime.expandVars("<myProp>myValue</myProp>"));
        assertEquals("<myProp>${myParam}</myProp>",
                runtime.expandVars("<myProp>${myParam}</myProp>"));
        assertEquals(
                "<myProp>myDefaultValue</myProp>",
                runtime.expandVars("<myProp>${myParam:=myDefaultValue}</myProp>"));

        // define system prop
        System.setProperty("myParam", "mySystemValue");
        assertEquals("<myProp>myValue</myProp>",
                runtime.expandVars("<myProp>myValue</myProp>"));
        assertEquals("<myProp>mySystemValue</myProp>",
                runtime.expandVars("<myProp>${myParam}</myProp>"));
        assertEquals(
                "<myProp>mySystemValue</myProp>",
                runtime.expandVars("<myProp>${myParam:=myDefaultValue}</myProp>"));

        // define prop
        runtime.getProperties().setProperty("myParam", "myValue");
        assertEquals("<myProp>myValue</myProp>",
                runtime.expandVars("<myProp>myValue</myProp>"));
        assertEquals("<myProp>myValue</myProp>",
                runtime.expandVars("<myProp>${myParam}</myProp>"));
        assertEquals(
                "<myProp>myValue</myProp>",
                runtime.expandVars("<myProp>${myParam:=myDefaultValue}</myProp>"));
    }

    @Test
    public void testExpandVarsWithSpecialChars() {
        // do not define prop
        assertEquals("<myProp>${\\my.strange/value}</myProp>",
                runtime.expandVars("<myProp>${\\my.strange/value}</myProp>"));
        assertEquals("<myProp>${my.param}</myProp>",
                runtime.expandVars("<myProp>${my.param}</myProp>"));
        assertEquals(
                "<myProp>myDefaultValue</myProp>",
                runtime.expandVars("<myProp>${my.param:=myDefaultValue}</myProp>"));

        // define system prop
        System.setProperty("my.param", "mySystemValue");
        assertEquals("<myProp>${\\my.strange/value}</myProp>",
                runtime.expandVars("<myProp>${\\my.strange/value}</myProp>"));
        assertEquals("<myProp>mySystemValue</myProp>",
                runtime.expandVars("<myProp>${my.param}</myProp>"));
        assertEquals(
                "<myProp>mySystemValue</myProp>",
                runtime.expandVars("<myProp>${my.param:=myDefaultValue}</myProp>"));

        // define prop
        runtime.getProperties().setProperty("my.param", "${\\my.strange/value}");
        assertEquals("<myProp>${\\my.strange/value}</myProp>",
                runtime.expandVars("<myProp>${\\my.strange/value}</myProp>"));
        assertEquals("<myProp>${\\my.strange/value}</myProp>",
                runtime.expandVars("<myProp>${my.param}</myProp>"));
        assertEquals(
                "<myProp>${\\my.strange/value}</myProp>",
                runtime.expandVars("<myProp>${my.param:=myDefaultValue}</myProp>"));

    }

    @Test
    public void testIsBooleanPropertyTrueFalse() throws Exception {
        assertNull(runtime.getProperties().getProperty("foo-true"));
        assertNull(runtime.getProperties().getProperty("foo-false"));
        assertNull(System.getProperty("foo-true"));
        assertNull(System.getProperty("foo-false"));
        assertFalse(Framework.isBooleanPropertyTrue("foo-true"));
        assertFalse(Framework.isBooleanPropertyTrue("foo-false"));
        assertFalse(Framework.isBooleanPropertyFalse("foo-false"));
        assertFalse(Framework.isBooleanPropertyFalse("foo-true"));
        setRuntimeProp("foo-true", "true");
        setRuntimeProp("foo-false", "false");
        assertTrue(Framework.isBooleanPropertyTrue("foo-true"));
        assertFalse(Framework.isBooleanPropertyTrue("foo-false"));
        assertFalse(Framework.isBooleanPropertyFalse("foo-true"));
        assertTrue(Framework.isBooleanPropertyFalse("foo-false"));
        setRuntimeProp("foo-true", "false");
        setRuntimeProp("foo-false", "true");
        assertFalse(Framework.isBooleanPropertyTrue("foo-true"));
        assertTrue(Framework.isBooleanPropertyTrue("foo-false"));
        assertTrue(Framework.isBooleanPropertyFalse("foo-true"));
        assertFalse(Framework.isBooleanPropertyFalse("foo-false"));
    }

    @Test
    public void testIsDevModeSet() throws Exception {
        assertTrue(Framework.isInitialized());
        // check compat
        assertEquals("org.nuxeo.dev", Framework.NUXEO_DEV_SYSTEM_PROP);
        // make sure runtime prop is not set
        assertNull(runtime.getProperties().getProperty(
                Framework.NUXEO_DEV_SYSTEM_PROP));
        setRuntimeProp(Framework.NUXEO_DEV_SYSTEM_PROP, "true");
        assertTrue(Framework.isDevModeSet());
        setRuntimeProp(Framework.NUXEO_DEV_SYSTEM_PROP, "");
        assertFalse(Framework.isDevModeSet());
        setRuntimeProp(Framework.NUXEO_DEV_SYSTEM_PROP, "false");
        assertFalse(Framework.isDevModeSet());
    }

    protected void setRuntimeProp(String name, String value) {
        runtime.getProperties().setProperty(name, value);
    }

}
