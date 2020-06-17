/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.platform.el;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.el.ExpressionFactoryImpl;
import org.junit.Test;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;

public class TestExpressionEvaluator {

    public static class SampleBean {

        private final String sampleValue = "value";

        public String getSampleValue() {
            return sampleValue;
        }

    }

    private static SampleBean createSampleBean() {
        return new SampleBean();
    }

    private static SampleBean[] createSampleArray() {
        return new SampleBean[] { createSampleBean() };
    }

    private static List<SampleBean> createSampleList() {
        return Arrays.asList(createSampleBean());
    }

    private final ExpressionEvaluator evaluatorUnderTest = new ExpressionEvaluator(new ExpressionFactoryImpl());

    private final ExpressionContext context = new ExpressionContext();

    private static Map<String, SampleBean> createSampleMap() {
        Map<String, SampleBean> sampleMap = new HashMap<>();
        sampleMap.put("key", createSampleBean());
        return sampleMap;
    }

    @Test
    public void testProperty() {
        SampleBean sampleBean = createSampleBean();
        evaluatorUnderTest.bindValue(context, "bean", sampleBean);
        Object value = evaluatorUnderTest.evaluateExpression(context, "${bean.sampleValue}", String.class);
        assertNotNull(value);
        assertTrue(value instanceof String);
        String stringValue = (String) value;
        assertEquals(sampleBean.getSampleValue(), stringValue);
    }

    @Test
    public void testMap() {
        Map<String, SampleBean> sampleMap = createSampleMap();
        evaluatorUnderTest.bindValue(context, "map", sampleMap);
        Object value = evaluatorUnderTest.evaluateExpression(context, "${map.key.sampleValue}", String.class);
        assertNotNull(value);
    }

    @Test
    public void testArray() {
        SampleBean[] sampleArray = createSampleArray();
        evaluatorUnderTest.bindValue(context, "array", sampleArray);
        Object value = evaluatorUnderTest.evaluateExpression(context, "${array[0].sampleValue}", String.class);
        assertNotNull(value);
    }

    @Test
    public void testList() {
        List<SampleBean> sampleList = createSampleList();
        evaluatorUnderTest.bindValue(context, "list", sampleList);
        Object value = evaluatorUnderTest.evaluateExpression(context, "${list[0].sampleValue}", String.class);
        assertNotNull(value);
        value = evaluatorUnderTest.evaluateExpression(context, "${list.get(0).sampleValue}", String.class);
        assertNotNull(value);
    }

    /**
     * NXP-28918
     */
    @Test
    public void testArrayContainsFunction() {
        Boolean res = evaluatorUnderTest.evaluateExpression(context, "${nx:arrayContains(array, 'foo')}",
                Boolean.class);
        assertFalse(res);

        String[] arr = new String[] { "bar" };
        evaluatorUnderTest.bindValue(context, "array", arr);
        res = evaluatorUnderTest.evaluateExpression(context, "${nx:arrayContains(array, 'foo')}", Boolean.class);
        assertFalse(res);

        res = evaluatorUnderTest.evaluateExpression(context, "${nx:arrayContains(array, 'bar')}", Boolean.class);
        assertTrue(res);
    }

    /**
     * NXP-28918
     */
    @Test
    public void testArrayContainsAllFunction() {
        Boolean res = evaluatorUnderTest.evaluateExpression(context, "${nx:arrayContainsAll(array, 'foo')}",
                Boolean.class);
        assertFalse(res);

        String[] arr = new String[] { "foo", "bar" };
        evaluatorUnderTest.bindValue(context, "array", arr);
        res = evaluatorUnderTest.evaluateExpression(context, "${nx:arrayContainsAll(array, 'foo', 'foobar')}",
                Boolean.class);
        assertFalse(res);

        res = evaluatorUnderTest.evaluateExpression(context, "${nx:arrayContainsAll(array, 'bar', 'foo')}",
                Boolean.class);
        assertTrue(res);

        res = evaluatorUnderTest.evaluateExpression(context, "${nx:arrayContainsAll(array, 'bar')}", Boolean.class);
        assertTrue(res);
    }

    /**
     * NXP-28918
     */
    @Test
    public void testArrayContainsAnyFunction() {
        Boolean res = evaluatorUnderTest.evaluateExpression(context, "${nx:arrayContainsAny(array, 'foo')}",
                Boolean.class);
        assertFalse(res);

        String[] arr = new String[] { "foo", "bar" };
        evaluatorUnderTest.bindValue(context, "array", arr);
        res = evaluatorUnderTest.evaluateExpression(context, "${nx:arrayContainsAny(array, 'barfoo', 'foobar')}",
                Boolean.class);
        assertFalse(res);

        res = evaluatorUnderTest.evaluateExpression(context, "${nx:arrayContainsAny(array, 'bar', 'foo')}",
                Boolean.class);
        assertTrue(res);

        res = evaluatorUnderTest.evaluateExpression(context, "${nx:arrayContainsAny(array, 'bar')}", Boolean.class);
        assertTrue(res);
    }

    /**
     * NXP-28918
     */
    @Test
    public void testArrayContainsNoneFunction() {
        Boolean res = evaluatorUnderTest.evaluateExpression(context, "${nx:arrayContainsNone(array, 'foo')}",
                Boolean.class);
        assertFalse(res);

        String[] arr = new String[] { "foo", "bar" };
        evaluatorUnderTest.bindValue(context, "array", arr);
        res = evaluatorUnderTest.evaluateExpression(context, "${nx:arrayContainsNone(array, 'foo', 'foobar')}",
                Boolean.class);
        assertFalse(res);

        res = evaluatorUnderTest.evaluateExpression(context, "${nx:arrayContainsNone(array, 'barfoo', 'foobar')}",
                Boolean.class);
        assertTrue(res);

        res = evaluatorUnderTest.evaluateExpression(context, "${nx:arrayContainsNone(array, 'foobar')}", Boolean.class);
        assertTrue(res);
    }

}
