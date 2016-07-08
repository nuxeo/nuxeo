/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.automation.scripting.internals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Before;
import org.junit.Test;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

public class TestMarshalingHelper {

    private ScriptEngine engine;

    @Before
    public void before() {
        engine = new ScriptEngineManager().getEngineByName("nashorn");
    }

    @Test
    public void testUnwrapList() throws ScriptException, NoSuchMethodException {
        engine.eval("function test() { return ['science', 'society']; }");
        ScriptObjectMirror object = (ScriptObjectMirror) ((Invocable) engine).invokeFunction("test");
        List<Object> list = MarshalingHelper.unwrapList(object);
        assertEquals("science", list.get(0));
        assertEquals("society", list.get(1));
    }

    @Test
    public void testUnwrapListError() throws ScriptException, NoSuchMethodException {
        engine.eval("function test() { return new Date(); };");
        ScriptObjectMirror object = (ScriptObjectMirror) ((Invocable) engine).invokeFunction("test");
        try {
            MarshalingHelper.unwrapList(object);
            fail("unwrapList should throw an exception.");
        } catch (IllegalArgumentException e) {
            assertEquals("JavaScript input is not an Array!", e.getMessage());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUnwrapMap() throws ScriptException, NoSuchMethodException {
        engine.eval("function test() { return {'key1':'value', 'key2': {'subKey': 'subValue'}}; }");
        ScriptObjectMirror object = (ScriptObjectMirror) ((Invocable) engine).invokeFunction("test");
        Map<String, Object> map = MarshalingHelper.unwrapMap(object);
        assertEquals("value", map.get("key1"));
        assertNotNull(map.get("key2"));
        assertEquals("subValue", ((Map<String, Object>) map.get("key2")).get("subKey"));
    }

    @Test
    public void testUnwrapMapError() throws ScriptException, NoSuchMethodException {
        engine.eval("function test() { return []; };");
        ScriptObjectMirror object = (ScriptObjectMirror) ((Invocable) engine).invokeFunction("test");
        try {
            MarshalingHelper.unwrapMap(object);
            fail("unwrapMap should throw an exception.");
        } catch (IllegalArgumentException e) {
            assertEquals("JavaScript input is not an Object!", e.getMessage());
        }
    }

    @Test
    public void testUnwrapDate() throws ScriptException, NoSuchMethodException {
        engine.eval("function test() { return new Date(0); }");
        ScriptObjectMirror object = (ScriptObjectMirror) ((Invocable) engine).invokeFunction("test");
        Calendar cal = MarshalingHelper.unwrapDate(object);
        Calendar expectedCal = Calendar.getInstance();
        expectedCal.setTimeInMillis(0);
        assertEquals(expectedCal, cal);
    }

    @Test
    public void testUnwrapDateError() throws ScriptException, NoSuchMethodException {
        engine.eval("function test() { return []; };");
        ScriptObjectMirror object = (ScriptObjectMirror) ((Invocable) engine).invokeFunction("test");
        try {
            MarshalingHelper.unwrapDate(object);
            fail("unwrapDate should throw an exception.");
        } catch (IllegalArgumentException e) {
            assertEquals("JavaScript input is not a Date!", e.getMessage());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUnwrapWithList() throws ScriptException, NoSuchMethodException {
        engine.eval("function test() { return ['science']; }");
        ScriptObjectMirror object = (ScriptObjectMirror) ((Invocable) engine).invokeFunction("test");
        Object obj = MarshalingHelper.unwrap(object);
        assertTrue(obj instanceof List);
        assertEquals("science", ((List<Object>) obj).get(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUnwrapWithMap() throws ScriptException, NoSuchMethodException {
        engine.eval("function test() { return {'key1':'value'}; }");
        ScriptObjectMirror object = (ScriptObjectMirror) ((Invocable) engine).invokeFunction("test");
        Object obj = MarshalingHelper.unwrap(object);
        assertTrue(obj instanceof Map);
        // Needed by DocumentScriptingWrapper#setPropertyValue
        assertTrue(obj instanceof Serializable);
        assertEquals("value", ((Map<String, Object>) obj).get("key1"));
    }

    @Test
    public void testUnwrapWithDate() throws ScriptException, NoSuchMethodException {
        engine.eval("function test() { return new Date(0); }");
        ScriptObjectMirror object = (ScriptObjectMirror) ((Invocable) engine).invokeFunction("test");
        Object obj = MarshalingHelper.unwrap(object);
        assertTrue(obj instanceof Calendar);
        Calendar expectedCal = Calendar.getInstance();
        expectedCal.setTimeInMillis(0);
        assertEquals(expectedCal, obj);
    }

    @Test
    public void testUnwrapError() throws ScriptException, NoSuchMethodException {
        engine.eval("function test() { return RegExp(); };");
        ScriptObjectMirror object = (ScriptObjectMirror) ((Invocable) engine).invokeFunction("test");
        try {
            MarshalingHelper.unwrap(object);
            fail("unwrap should throw an exception.");
        } catch (UnsupportedOperationException e) {
            assertEquals("RegExp is not supported!", e.getMessage());
        }
    }

}
