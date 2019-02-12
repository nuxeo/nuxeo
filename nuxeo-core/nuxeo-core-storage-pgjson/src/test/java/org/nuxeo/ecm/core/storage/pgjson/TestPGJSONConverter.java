/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.pgjson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Test;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.schema.types.ComplexTypeImpl;
import org.nuxeo.ecm.core.schema.types.ListTypeImpl;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.pgjson.PGJSONRepository.TypesMap;

public class TestPGJSONConverter {

    protected static final String DATE_ISO = "2019-01-02T15:45:42.123Z";

    protected static final String DATE_MILLIS = "1546443942123";

    @Test
    public void testStringToJson() {
        PGJSONConverter converter = new PGJSONConverter(null);
        assertEquals("\"\"", converter.valueToJson(""));
        assertEquals("\"\\\\\"", converter.valueToJson("\\"));
        assertEquals("\"\\\"\"", converter.valueToJson("\""));
        assertEquals("\"AB\"", converter.valueToJson("AB"));
        assertEquals("\"A\\\\B\"", converter.valueToJson("A\\B"));
        assertEquals("\"A\\\"B\"", converter.valueToJson("A\"B"));
        assertEquals("\" !~ \"", converter.valueToJson(" !~ "));
        // control char escapes
        assertEquals("\"\\b\\f\\n\\r\\t\"", converter.valueToJson("\b\f\n\r\t"));
        // non-ascii
        assertEquals("\"\\u0001\"", converter.valueToJson("\u0001"));
        assertEquals("\"\\u00E9\"", converter.valueToJson("\u00e9"));
        assertEquals("\"\\u1234\"", converter.valueToJson("\u1234"));
        assertEquals("\"\\uFEDC\"", converter.valueToJson("\ufedc"));
        // cannot convert null char
        try {
            converter.valueToJson("abc\0def");
            fail("sould fail to convert null char");
        } catch (NuxeoException e) {
            assertEquals("Invalid null character at pos: 3", e.getMessage());
        }
    }

    @Test
    public void testLongToJson() {
        PGJSONConverter converter = new PGJSONConverter(null);
        assertEquals("0", converter.valueToJson(Long.valueOf(0)));
        assertEquals("123456789", converter.valueToJson(Long.valueOf(123456789)));
        assertEquals("-123", converter.valueToJson(Long.valueOf(-123)));
    }

    @Test
    public void testDoubleToJson() {
        PGJSONConverter converter = new PGJSONConverter(null);
        assertEquals("0.0", converter.valueToJson(Double.valueOf(0)));
        assertEquals("1.234", converter.valueToJson(Double.valueOf(1.234)));
    }

    @Test
    public void testBooleanToJson() {
        PGJSONConverter converter = new PGJSONConverter(null);
        assertEquals("false", converter.valueToJson(Boolean.FALSE));
        assertEquals("true", converter.valueToJson(Boolean.TRUE));
    }

    @Test
    public void testCalendarToJson() {
        PGJSONConverter converter = new PGJSONConverter(null);
        Calendar cal = GregorianCalendar.from(ZonedDateTime.parse(DATE_ISO));
        assertEquals(DATE_MILLIS, converter.valueToJson(cal));
    }

    @Test
    public void testListToJson() {
        PGJSONConverter converter = new PGJSONConverter(null);
        assertEquals("[]", converter.valueToJson(Arrays.asList()));
        assertEquals("[\"foo\"]", converter.valueToJson(Arrays.asList("foo")));
        assertEquals("[\"foo\",\"bar\"]", converter.valueToJson(Arrays.asList("foo", "bar")));
        assertEquals("[\"foo\",123]", converter.valueToJson(Arrays.asList("foo", Long.valueOf(123))));
        // nulls are skipped
        assertEquals("[\"foo\",\"bar\"]", converter.valueToJson(Arrays.asList("foo", null, "bar")));
    }

    @Test
    public void testArrayToJson() {
        PGJSONConverter converter = new PGJSONConverter(null);
        assertEquals("[]", converter.valueToJson(new String[0]));
        assertEquals("[\"foo\"]", converter.valueToJson(new String[] { "foo" }));
        assertEquals("[\"foo\",\"bar\"]", converter.valueToJson(new String[] { "foo", "bar" }));
        assertEquals("[\"foo\",123]", converter.valueToJson(new Object[] { "foo", Long.valueOf(123) }));
        // nulls are skipped
        assertEquals("[\"foo\",\"bar\"]", converter.valueToJson(new String[] { "foo", null, "bar" }));
    }

    @Test
    public void testStateToJson() {
        PGJSONConverter converter = new PGJSONConverter(null);
        StateDiff state = new StateDiff(); // to keep null values
        assertEquals("{}", converter.valueToJson(state));
        state.put("foo", "bar");
        assertEquals("{\"foo\":\"bar\"}", converter.valueToJson(state));
        state.put("gee", "moo");
        // State keeps order for a small number of keys so this is ok
        assertEquals("{\"foo\":\"bar\",\"gee\":\"moo\"}", converter.valueToJson(state));
        state.put("foo", null);
        assertEquals("{\"gee\":\"moo\"}", converter.valueToJson(state));

        // nested
        State state2 = new State();
        state2.put("a", Long.valueOf(123));
        state2.put("b", (Serializable) Arrays.asList((Serializable) Boolean.TRUE, Double.valueOf(3.14)));
        state.put("bar", state2);
        assertEquals("{\"gee\":\"moo\",\"bar\":{\"a\":123,\"b\":[true,3.14]}}", converter.valueToJson(state));
    }

    @Test
    public void testJsonToState() {
        TypesMap types = new TypesMap();
        types.put("int", LongType.INSTANCE);
        types.put("double", DoubleType.INSTANCE);
        types.put("double2", DoubleType.INSTANCE);
        types.put("doubleint", DoubleType.INSTANCE);
        types.put("date", DateType.INSTANCE);
        ComplexTypeImpl ct = new ComplexTypeImpl(null, null, null);
        ct.addField("foo", LongType.INSTANCE, null, 0, null);
        types.put("objint", new TypesMap("objint", ct, "foo", LongType.INSTANCE));
        ListTypeImpl lt = new ListTypeImpl(null, null, LongType.INSTANCE);
        types.put("arrayint", new TypesMap("arrayint", lt, "0", LongType.INSTANCE));
        PGJSONConverter converter = new PGJSONConverter(types);

        String json = "{\"bar\":   \t     \"bar\"    \r\n, " //
                + "\"esc\": \"\\\"\\\\\\/\\b\\f\\n\\r\\t\\u00E9\\u1234\\ufedc\", " //
                + "\"true\": true, " //
                + "\"false\": false, " //
                + "\"null\": null, " //
                + "\"int\": 123, " //
                + "\"double\": 456.789, " //
                + "\"double2\": -1.23E45, " //
                + "\"doubleint\": 121212, " //
                + "\"date\": " + DATE_MILLIS + ", " //
                + "\"objempty\": {}, " //
                + "\"obj\": {\"foo\": \"bar\"}, " //
                + "\"objint\": {\"foo\": 123}, " //
                + "\"listempty\": [], " //
                + "\"array\": [\"foo\", \"bar\"], " //
                + "\"arrayint\": [-123], " //
                + "\"list\": [{\"a\": true}]" //
                + "}";
        Object state = converter.jsonToValue(json);
        State expected = new State();
        expected.put("bar", "bar");
        expected.put("esc", "\"\\/\b\f\n\r\t\u00e9\u1234\ufedc");
        expected.put("true", Boolean.TRUE);
        expected.put("false", Boolean.FALSE);
        // null not visible in a State
        expected.put("int", Long.valueOf("123"));
        expected.put("double", Double.valueOf("456.789"));
        expected.put("double2", Double.valueOf("-1.23E45"));
        expected.put("doubleint", Double.valueOf("121212"));
        expected.put("date", GregorianCalendar.from(ZonedDateTime.parse(DATE_ISO)));
        // empty object turned into null so not visible in State
        State obj = new State();
        obj.put("foo", "bar");
        expected.put("obj", obj);
        State objint = new State();
        objint.put("foo", Long.valueOf(123));
        expected.put("objint", objint);
        // empty array turned into null so not visible in State
        expected.put("array", new String[] { "foo", "bar" });
        expected.put("arrayint", new Long[] { Long.valueOf("-123") });
        State listst = new State();
        listst.put("a", Boolean.TRUE);
        expected.put("list", (Serializable) Arrays.asList(listst));
        assertEquals(expected, state);
    }

    @Test
    public void testJsonParsingLong() {
        assertLong("0");
        assertLong("-0");
        assertLong("1");
        assertLong("-1");
        assertLong("1234567890");
        assertLong("-1234567890");
        assertLong("1234567890123456789");
        assertLong("-1234567890123456789");
        try {
            assertLong("12345678901234567890");
            fail("should fail to parse as a long");
        } catch (NuxeoException e) {
            assertEquals("Got Double instead of Long for path: foo", e.getMessage());
        }
    }

    protected static void assertLong(String value) {
        TypesMap types = new TypesMap();
        types.put("foo", LongType.INSTANCE);
        PGJSONConverter converter = new PGJSONConverter(types);
        Object state = converter.jsonToValue("{\"foo\":" + value + "}");
        State expected = new State();
        expected.put("foo", Long.valueOf(value));
        assertEquals(expected, state);
    }

    @Test
    public void testJsonParsingDouble() {
        assertDouble("0");
        assertDouble("1");
        assertDouble("-1");
        assertDouble("1234567890");
        assertDouble("-1234567890");
        assertDouble("1234567890123456789");
        assertDouble("-1234567890123456789");
        assertDouble("1234567890123456789012");
        assertDouble("-1234567890123456789012");
        assertDouble("0.1234");
        assertDouble("-0.1234");
        assertDouble("12.34");
        assertDouble("-12.34");
        assertDouble("12.34e56");
        assertDouble("-12.34e56");
        assertDouble("12.34E+56");
        assertDouble("12.34E-56");
    }

    protected static void assertDouble(String value) {
        TypesMap types = new TypesMap();
        types.put("foo", DoubleType.INSTANCE);
        PGJSONConverter converter = new PGJSONConverter(types);
        Object state = converter.jsonToValue("{\"foo\":" + value + "}");
        State expected = new State();
        expected.put("foo", Double.valueOf(value));
        assertEquals(expected, state);
    }

    @Test
    public void testJsonParsingErrors() {
        assertError(" ", "Unexpected ' ' at pos: 1");
        assertError("#", "Unexpected '#' at pos: 1");
        assertError("{", "Expected '\"' instead of EOF at pos: 1");
        assertError("{,", "Expected '\"' instead of ',' at pos: 2");
        assertError("{\u0000}", "Expected '\"' instead of EOF at pos: 2"); // \u0000 is EOF
        assertError("{} ", "Expected EOF instead of ' ' at pos: 3");
        assertError("{}#", "Expected EOF instead of '#' at pos: 3");
        assertError("{#", "Expected '\"' instead of '#' at pos: 2");
        // string key
        assertError("{\"", "Unexpected EOF in string at pos: 2");
        assertError("{\"foo", "Unexpected EOF in string at pos: 5");
        // escape
        assertError("{\"foo\\", "Unexpected EOF in string escape at pos: 6");
        assertError("{\"foo\\X", "Invalid escape 'X' at pos: 7");
        assertError("{\"foo\\u", "Unexpected EOF in string hex escape at pos: 7");
        assertError("{\"foo\\u1", "Unexpected EOF in string hex escape at pos: 8");
        assertError("{\"foo\\u12", "Unexpected EOF in string hex escape at pos: 9");
        assertError("{\"foo\\u123", "Unexpected EOF in string hex escape at pos: 10");
        assertError("{\"foo\\uX", "Invalid hex escape 'X' at pos: 8");
        // colon
        assertError("{\"foo\"", "Expected ':' instead of EOF at pos: 6");
        assertError("{\"foo\":", "Expected value instead of EOF at pos: 7");
        // unknown value
        assertError("{\"foo\":X", "Unexpected 'X' at pos: 8");
        // string value
        assertError("{\"foo\":\"", "Unexpected EOF in string at pos: 8");
        assertError("{\"foo\":\"foo", "Unexpected EOF in string at pos: 11");
        assertError("{\"foo\":\"foo\"", "Expected ',' or '}' instead of EOF at pos: 12");
        // true
        assertError("{\"foo\":t", "Expected 'r' instead of EOF at pos: 8");
        assertError("{\"foo\":tr", "Expected 'u' instead of EOF at pos: 9");
        assertError("{\"foo\":tru", "Expected 'e' instead of EOF at pos: 10");
        assertError("{\"foo\":true", "Expected ',' or '}' instead of EOF at pos: 11");
        assertError("{\"foo\":tX", "Expected 'r' instead of 'X' at pos: 9");
        assertError("{\"foo\":trX", "Expected 'u' instead of 'X' at pos: 10");
        assertError("{\"foo\":truX", "Expected 'e' instead of 'X' at pos: 11");
        assertError("{\"foo\":trueX", "Expected ',' or '}' instead of 'X' at pos: 12");
        assertError("{\"foo\":true X", "Expected ',' or '}' instead of 'X' at pos: 13");
        // false
        assertError("{\"foo\":f", "Expected 'a' instead of EOF at pos: 8");
        assertError("{\"foo\":fa", "Expected 'l' instead of EOF at pos: 9");
        assertError("{\"foo\":fal", "Expected 's' instead of EOF at pos: 10");
        assertError("{\"foo\":fals", "Expected 'e' instead of EOF at pos: 11");
        assertError("{\"foo\":false", "Expected ',' or '}' instead of EOF at pos: 12");
        assertError("{\"foo\":fX", "Expected 'a' instead of 'X' at pos: 9");
        assertError("{\"foo\":faX", "Expected 'l' instead of 'X' at pos: 10");
        assertError("{\"foo\":falX", "Expected 's' instead of 'X' at pos: 11");
        assertError("{\"foo\":falsX", "Expected 'e' instead of 'X' at pos: 12");
        assertError("{\"foo\":falseX", "Expected ',' or '}' instead of 'X' at pos: 13");
        assertError("{\"foo\":false X", "Expected ',' or '}' instead of 'X' at pos: 14");
        // null
        assertError("{\"foo\":n", "Expected 'u' instead of EOF at pos: 8");
        assertError("{\"foo\":nu", "Expected 'l' instead of EOF at pos: 9");
        assertError("{\"foo\":nul", "Expected 'l' instead of EOF at pos: 10");
        assertError("{\"foo\":null", "Expected ',' or '}' instead of EOF at pos: 11");
        assertError("{\"foo\":nX", "Expected 'u' instead of 'X' at pos: 9");
        assertError("{\"foo\":nuX", "Expected 'l' instead of 'X' at pos: 10");
        assertError("{\"foo\":nulX", "Expected 'l' instead of 'X' at pos: 11");
        assertError("{\"foo\":nullX", "Expected ',' or '}' instead of 'X' at pos: 12");
        assertError("{\"foo\":null X", "Expected ',' or '}' instead of 'X' at pos: 13");
        // long
        assertError("{\"foo\":0", "Expected ',' or '}' instead of EOF at pos: 8");
        assertError("{\"foo\":0X", "Expected ',' or '}' instead of 'X' at pos: 9");
        assertError("{\"foo\":01", "Expected ',' or '}' instead of '1' at pos: 9");
        assertError("{\"foo\":1", "Expected ',' or '}' instead of EOF at pos: 8");
        assertError("{\"foo\":1X", "Expected ',' or '}' instead of 'X' at pos: 9");
        assertError("{\"foo\":123", "Expected ',' or '}' instead of EOF at pos: 10");
        assertError("{\"foo\":123X", "Expected ',' or '}' instead of 'X' at pos: 11");
        assertError("{\"foo\":-0", "Expected ',' or '}' instead of EOF at pos: 9");
        assertError("{\"foo\":-01", "Expected ',' or '}' instead of '1' at pos: 10");
        assertError("{\"foo\":-1", "Expected ',' or '}' instead of EOF at pos: 9");
        assertError("{\"foo\":-123", "Expected ',' or '}' instead of EOF at pos: 11");
        // double
        assertError("{\"foo\":123.", "Expected digit between '0' and '9' instead of EOF at pos: 11");
        assertError("{\"foo\":123.X", "Expected digit between '0' and '9' instead of 'X' at pos: 12");
        assertError("{\"foo\":123.456", "Expected ',' or '}' instead of EOF at pos: 14", true);
        assertError("{\"foo\":123.456X", "Expected ',' or '}' instead of 'X' at pos: 15", true);
        assertError("{\"foo\":123.456e", "Expected digit between '0' and '9' instead of EOF at pos: 15");
        assertError("{\"foo\":123.456e+", "Expected digit between '0' and '9' instead of EOF at pos: 16");
        assertError("{\"foo\":123.456e+X", "Expected digit between '0' and '9' instead of 'X' at pos: 17");
        assertError("{\"foo\":123.456e+1X", "Expected ',' or '}' instead of 'X' at pos: 18", true);
        assertError("{\"foo\":123e", "Expected digit between '0' and '9' instead of EOF at pos: 11");
        assertError("{\"foo\":123e+", "Expected digit between '0' and '9' instead of EOF at pos: 12");
        assertError("{\"foo\":123e+X", "Expected digit between '0' and '9' instead of 'X' at pos: 13");
        assertError("{\"foo\":123e+1X", "Expected ',' or '}' instead of 'X' at pos: 14", true);
        // array
        assertError("{\"foo\":[", "Expected value instead of EOF at pos: 8");
        assertError("{\"foo\":[,", "Unexpected ',' at pos: 9");
    }

    protected static void assertError(String badJson, String message) {
        assertError(badJson, message, false);
    }

    protected static void assertError(String badJson, String message, boolean dbl) {
        TypesMap types = new TypesMap();
        types.put("foo", dbl ? DoubleType.INSTANCE : LongType.INSTANCE);
        PGJSONConverter converter = new PGJSONConverter(types);
        try {
            converter.jsonToValue(badJson);
            fail("Parsing should fail for: " + badJson);
        } catch (NuxeoException e) {
            assertEquals(message, e.getMessage());
        }
    }

}
