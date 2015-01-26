/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.Assert;

/**
 * Makes chained assertion on json.
 *
 * @since 7.2
 */
public class JsonAssert {

    private JsonNode jsonNode;

    private JsonAssert(String json) throws IOException {
        jsonNode = JsonFactoryProvider.get().createJsonParser(json).readValueAsTree();
    }

    private JsonAssert(JsonNode jsonNode) {
        this.jsonNode = jsonNode;
    }

    /**
     * Create an Json assertion on the given json string.
     *
     * @param json The json on which you want to make assertion.
     */
    public static JsonAssert on(String json) throws IOException {
        return new JsonAssert(json);
    }

    /**
     * Gets the underlying {@link JsonNode} of the current {@link JsonAssert}.
     *
     * @return A {@link JsonNode}
     * @since 7.2
     */
    public JsonNode getNode() {
        return jsonNode;
    }

    /**
     * Prints the current json content.
     */
    @Override
    public String toString() {
        return jsonNode.toString();
    }

    /**
     * Gets a json assertion on the element at the given index. Works on array.
     *
     * @param index The index in the array.
     * @return A json assertion on the element of the array.
     * @since 7.2
     */
    public JsonAssert get(int index) throws IOException {
        return get("[" + index + "]");
    }

    /**
     * Gets a json assertion on the element of which the path in the current {@link JsonAssert} is the given json path.
     *
     * @param jsonPath The json path: property or [index] or property.subProperty[index] or
     *            [index].property[index][index]
     * @return A json assertion on the element targeted by the given path.
     * @since 7.2
     */
    public JsonAssert get(String jsonPath) throws IOException {
        // tokenize the path
        String SEPARATOR = "<SEP>";
        String pattern = "[\\[\\]\\.]";
        Iterator<String> tokens = Arrays.asList(
                jsonPath.replaceAll("(" + pattern + ")", SEPARATOR + "$1" + SEPARATOR).split(SEPARATOR)).iterator();
        JsonNode jn = jsonNode;
        String read = "";
        // iterates on tokens and navigate i json nodes
        while (tokens.hasNext()) { // ends when all token are read
            String token = tokens.next();
            if (StringUtils.isBlank(token)) {
                continue;
            }
            switch (token) {
            case ".": // simple separator, ignore it
                read += token;
                break;
            case "[": // an index in a rray is expected, checks its an array an navigate in the array element
                read += token;
                if (!tokens.hasNext()) {
                    throw new IOException("Invalid json parameter value : [ must be followed by a index and by ] :"
                            + read);
                }
                // get the index
                Integer index = null;
                try {
                    index = Integer.valueOf(tokens.next());
                    read += index;
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid json parameter value : [ must be followed by a index and by ] :"
                            + read);
                }
                if (index < 0) {
                    throw new IOException("Invalid json parameter value : [ must be followed by a index and by ] :"
                            + read);
                }
                if (!tokens.hasNext() || !"]".equals(tokens.next())) {
                    throw new IOException("Invalid json parameter value : [ must be followed by a index and by ] :"
                            + read);
                }
                read += "]";
                // checks its an array
                if (!jn.isArray()) {
                    throw new IOException(read + " is not a array");
                }
                // checks the index exists
                if (!jn.has(index)) {
                    return null;
                }
                // navigate
                jn = jn.get(index);
                break;
            default: // a property, navigate in the property if the current its an abject
                // checks its an array
                if (!jn.isObject()) {
                    throw new IOException(read + " is not an object");
                }
                // checks the property exists
                if (!jn.has(token)) {
                    return null;
                }
                read += token;
                // navigates
                jn = jn.get(token);
                break;
            }
        }
        // returns the new assertion
        return new JsonAssert(jn);
    }

    /**
     * Checks if the current element is an array which contains an element at the given index (starting 0).
     *
     * @param index The index to check.
     * @return A json assertion for the element at the given index.
     * @since 7.2
     */
    public JsonAssert has(int index) throws IOException {
        JsonAssert jsonAssert = get(index);
        Assert.assertNotNull("no field " + index, jsonAssert);
        return jsonAssert;
    }

    /**
     * Checks if the current element is an array which does not contains an element at the given index (starting 0).
     *
     * @param index The index to check.
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert hasNot(int index) throws IOException {
        Assert.assertNull("field is present", get(index));
        return this;
    }

    /**
     * Checks if the current element has an element at in the given json path. see {@link #get(String)}
     *
     * @param index The index to check.
     * @return A json assertion for the element at the given json path.
     * @since 7.2
     */
    public JsonAssert has(String path) throws IOException {
        JsonAssert jsonAssert = get(path);
        Assert.assertNotNull("no field " + path, jsonAssert);
        return jsonAssert;
    }

    /**
     * Checks if the current element has an element at in the given json path. see {@link #get(String)}
     *
     * @param index The index to check.
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert hasNot(String path) throws IOException {
        Assert.assertNull("field is present", get(path));
        return this;
    }

    /**
     * Checks the current element is null
     *
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isNull() {
        Assert.assertTrue("value is not null", jsonNode.isNull());
        return this;
    }

    /**
     * Checks the current is not null.
     *
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert notNull() {
        Assert.assertFalse("value is null", jsonNode.isNull());
        return this;
    }

    /**
     * Checks the current is a text.
     *
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isText() {
        notNull();
        Assert.assertTrue("not a text value", jsonNode.isTextual());
        return this;
    }

    /**
     * @return The current text has the expected value.
     * @param expected The expected value
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isEquals(String expected) {
        isText();
        String value = jsonNode.getTextValue();
        Assert.assertEquals(equalsMsg(expected, value), expected, value);
        return this;
    }

    /**
     * Checks the current text has not the given value.
     *
     * @param expected The not expected value.
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert notEquals(String expected) {
        isText();
        Assert.assertNotEquals(notEqualsMsg(expected), expected, jsonNode.getTextValue());
        return this;
    }

    /**
     * Checks the current is a boolean.
     *
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isBool() {
        notNull();
        Assert.assertTrue("not a boolean value", jsonNode.isBoolean());
        return this;
    }

    /**
     * Checks the current boolean is true.
     *
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isTrue() {
        isBool();
        Assert.assertTrue("is not true", jsonNode.getBooleanValue());
        return this;
    }

    /**
     * Checks the current boolean is false.
     *
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isFalse() {
        isBool();
        Assert.assertFalse("is not false", jsonNode.getBooleanValue());
        return this;
    }

    /**
     * Checks the current boolean has the expected value.
     *
     * @param expected The expected value
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isEquals(boolean expected) {
        isBool();
        boolean value = jsonNode.getBooleanValue();
        Assert.assertEquals(equalsMsg(expected, value), expected, value);
        return this;
    }

    /**
     * Checks the current boolean has not the given value.
     *
     * @param expected The not expected value
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isNotEquals(boolean expected) {
        isBool();
        boolean value = jsonNode.getBooleanValue();
        Assert.assertNotEquals(notEqualsMsg(expected), expected, value);
        return this;
    }

    /**
     * Checks the current is an integer.
     *
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isInt() {
        notNull();
        Assert.assertTrue("not an int", jsonNode.isIntegralNumber());
        return this;
    }

    /**
     * Checks the current integer has the expected value.
     *
     * @param expected The expected value
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isEquals(int expected) {
        isInt();
        int value = jsonNode.getIntValue();
        Assert.assertEquals(equalsMsg(expected, value), expected, value);
        return this;
    }

    /**
     * Checks the current integer has not the given value.
     *
     * @param expected The not expected value
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert notEquals(int expected) {
        isInt();
        Assert.assertNotEquals(notEqualsMsg(expected), expected, jsonNode.getLongValue());
        return this;
    }

    /**
     * Checks the current integer has the expected value.
     *
     * @param expected The expected value
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isEquals(long expected) {
        isInt();
        long value = jsonNode.getLongValue();
        Assert.assertEquals(equalsMsg(expected, value), expected, value);
        return this;
    }

    /**
     * Checks the current integer has not the given value.
     *
     * @param expected The not expected value
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert notEquals(long expected) {
        isInt();
        Assert.assertNotEquals(notEqualsMsg(expected), expected, jsonNode.getLongValue());
        return this;
    }

    /**
     * Checks the current is an floating point number.
     *
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isDouble() {
        notNull();
        Assert.assertTrue("not a double", jsonNode.isFloatingPointNumber());
        return this;
    }

    /**
     * Checks the current floating point number has the expected value.
     *
     * @param expected The expected value
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isEquals(double expected, double delta) {
        isDouble();
        double value = jsonNode.getDoubleValue();
        Assert.assertEquals(equalsMsg(expected, value + " +- " + delta), expected, value, delta);
        return this;
    }

    /**
     * Checks the current floating point number has not the given value.
     *
     * @param expected The not expected value
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert notEquals(double expected, double delta) {
        isDouble();
        Assert.assertNotEquals(notEqualsMsg(expected + " +- " + delta), jsonNode.getDoubleValue(), delta);
        return this;
    }

    /**
     * Checks the current floating point number has the expected value.
     *
     * @param expected The expected value
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isEquals(float expected, float delta) {
        isDouble();
        double value = jsonNode.getDoubleValue();
        Assert.assertEquals(equalsMsg(expected, value + " +- " + delta), expected, value, delta);
        return this;
    }

    /**
     * Checks the current floating point number has not the given value.
     *
     * @param expected The not expected value
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert notEquals(float expected, float delta) {
        isDouble();
        Assert.assertNotEquals(notEqualsMsg(expected + " +- " + delta), jsonNode.getDoubleValue(), delta);
        return this;
    }

    /**
     * Checks the current floating point number has the expected value.
     *
     * @param expected The expected value
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isEquals(BigDecimal expected) {
        isDouble();
        BigDecimal value = jsonNode.getDecimalValue();
        Assert.assertEquals(equalsMsg(expected, value), expected, value);
        return this;
    }

    /**
     * Checks the current floating point number has not the given value.
     *
     * @param expected The not expected value
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert notEquals(BigDecimal expected) {
        isDouble();
        Assert.assertNotEquals(notEqualsMsg(expected), jsonNode.getDecimalValue());
        return this;
    }

    /**
     * Checks the current is a binary.
     *
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isBinary() {
        notNull();
        Assert.assertTrue("not a binary", jsonNode.isBinary());
        return this;
    }

    /**
     * Checks the current binary has the expected value.
     *
     * @param expected The expected value
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isEquals(byte[] expected) throws IOException {
        isBinary();
        byte[] value = jsonNode.getBinaryValue();
        Assert.assertEquals(equalsMsg(expected, value), expected, value);
        return this;
    }

    /**
     * Checks the current binary has not the given value.
     *
     * @param expected The not expected value
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert notEquals(byte[] expected) throws IOException {
        isBinary();
        Assert.assertNotEquals(notEqualsMsg(expected), jsonNode.getBinaryValue());
        return this;
    }

    /**
     * Checks the current is an object.
     *
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isObject() {
        notNull();
        Assert.assertTrue("is not an object", jsonNode.isObject());
        return this;
    }

    /**
     * Checks the current is not an object.
     *
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert notObject() {
        notNull();
        Assert.assertTrue("is an object", jsonNode.isObject());
        return this;
    }

    /**
     * Checks the current object has the given number of properties.
     *
     * @param count the expected number of properties.
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert properties(int count) {
        isObject();
        int found = 0;
        // iterates to count properties
        Iterator<JsonNode> it = jsonNode.getElements();
        while (it.hasNext()) {
            found++;
            it.next();
        }
        Assert.assertEquals("Expected " + count + " children but found " + found, count, found);
        return this;
    }

    /**
     * Checks the current is an array.
     *
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert isArray() {
        notNull();
        Assert.assertTrue("is not an array", jsonNode.isArray());
        return this;
    }

    /**
     * Checks the current is not an array.
     *
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert notArray() {
        notNull();
        Assert.assertTrue("is an array", jsonNode.isArray());
        return this;
    }

    /**
     * Checks the current array has the given number of element.
     *
     * @param count the expected number of element.
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert length(int length) {
        isArray();
        if (length == 0) {
            Assert.assertFalse("has more than 0 element : " + jsonNode.toString(), jsonNode.has(0));
            return this;
        }
        Assert.assertTrue("has less than " + length + " elements", jsonNode.has(length - 1));
        Assert.assertFalse("has more than " + length + " elements", jsonNode.has(length));
        return this;
    }

    /**
     * Checks the current array contains exactly the given json as string.
     *
     * @param expecteds A set of json string.
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert contains(String... expecteds) {
        length(expecteds.length);
        JsonNode jn = null;
        Iterator<JsonNode> it = jsonNode.getElements();
        Map<String, Integer> expectedMap = new HashMap<String, Integer>();
        for (String value : expecteds) {
            Integer count = expectedMap.get(value);
            if (count == null) {
                count = 0;
            }
            count++;
            expectedMap.put(value, count);
        }
        Map<String, Integer> foundMap = new HashMap<String, Integer>();
        List<String> founds = new ArrayList<String>();
        while (it.hasNext()) {
            jn = it.next();
            String value = jn.isNull() ? null : jn.getValueAsText();
            founds.add(value);
            Integer count = foundMap.get(value);
            if (count == null) {
                count = 0;
            }
            count++;
            foundMap.put(value, count);
        }
        Assert.assertEquals("some value were not found or not expected: found=" + founds, expectedMap, foundMap);
        return this;
    }

    /**
     * Checks whether the node targeted by the given path contains all the given json strings. The path could be like
     * 'property' or 'property.subProperty'. If the path contains arrays, each sub elements would be checked.
     * <p>
     * Example:
     *
     * <pre>
     * JsonAssert json = JsonAssert.on("{ "element": [ { "name": "name1" }, { "name": "name1" }, { "name": "name2" } ] }");
     * json.containsAll("element.name", "name1", "name1", "name2"); // works
     * json.containsAll("element.name", "name2", "name1", "name1"); // works
     * json.containsAll("element.name", "name1", "name2", "name1"); // works
     * json.containsAll("element.name", "toto"); // fail
     * json.containsAll("element.name"); // fail
     * json.containsAll("element.name", "name1", "name2"); // fail, even if there's just name1 and name2, it checks the length too.
     * json.containsAll("element.name", "name1", "name2", "name2"); // fail, name1 was found just one time, name2 was found 2 times
     * </pre>
     *
     * </p>
     *
     * @param path The targeted path.
     * @param values All the expected values.
     * @return The current json assertion for chaining.
     * @since 7.2
     */
    public JsonAssert childrenContains(String path, String... values) throws IOException {
        List<String> founds = getAll(path, jsonNode);
        Assert.assertEquals("found more or less element thant expected : found=" + founds, values.length, founds.size());
        Map<String, Integer> expectedMap = new HashMap<String, Integer>();
        for (String value : values) {
            Integer count = expectedMap.get(value);
            if (count == null) {
                count = 0;
            }
            count++;
            expectedMap.put(value, count);
        }
        Map<String, Integer> foundMap = new HashMap<String, Integer>();
        for (String value : founds) {
            Integer count = foundMap.get(value);
            if (count == null) {
                count = 0;
            }
            count++;
            foundMap.put(value, count);
        }
        Assert.assertEquals("some value were not found or not expected: found=" + founds, expectedMap, foundMap);
        return this;
    }

    /**
     * utility for {@link #childrenContains(String, String...)}
     */
    private List<String> getAll(String path, JsonNode node) throws IOException {
        List<String> result = new ArrayList<String>();
        if (!node.isArray()) {
            int index = path.indexOf('.');
            if (index < 0) {
                JsonNode el = node.get(path);
                if (el.isNull()) {
                    result.add(null);
                } else {
                    result.add(el.getValueAsText());
                }
            } else {
                String token = path.substring(0, index);
                JsonNode el = node.get(token);
                String rest = path.substring(index + 1);
                result.addAll(getAll(rest, el));
            }
        } else {
            Iterator<JsonNode> it = node.getElements();
            while (it.hasNext()) {
                result.addAll(getAll(path, it.next()));
            }
        }
        return result;
    }

    private String equalsMsg(Object expected, Object value) {
        return "expected : " + expected + " but was " + value;
    }

    private String notEqualsMsg(Object expected) {
        return "is equals to expected : " + expected;
    }

    /**
     * @deprecated do not confound with isEquals - to check equality, use toString() to compare json
     */
    @Override
    @Deprecated
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

}
