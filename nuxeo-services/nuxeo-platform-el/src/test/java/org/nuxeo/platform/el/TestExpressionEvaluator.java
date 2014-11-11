/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.platform.el;

import static org.junit.Assert.assertEquals;
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

    private final ExpressionEvaluator evaluatorUnderTest = new ExpressionEvaluator(
            new ExpressionFactoryImpl());

    private final ExpressionContext context = new ExpressionContext();

    private static Map<String, SampleBean> createSampleMap() {
        Map<String, SampleBean> sampleMap = new HashMap<String, SampleBean>();
        sampleMap.put("key", createSampleBean());
        return sampleMap;
    }

    @Test
    public void testProperty() {
        SampleBean sampleBean = createSampleBean();
        evaluatorUnderTest.bindValue(context, "bean", sampleBean);
        Object value = evaluatorUnderTest.evaluateExpression(context,
                "${bean.sampleValue}", String.class);
        assertNotNull(value);
        assertTrue(value instanceof String);
        String stringValue = (String) value;
        assertEquals(sampleBean.getSampleValue(), stringValue);
    }

    @Test
    public void testMap() {
        Map<String, SampleBean> sampleMap = createSampleMap();
        evaluatorUnderTest.bindValue(context, "map", sampleMap);
        Object value = evaluatorUnderTest.evaluateExpression(context,
                "${map.key.sampleValue}", String.class);
        assertNotNull(value);
    }

    @Test
    public void testArray() {
        SampleBean[] sampleArray = createSampleArray();
        evaluatorUnderTest.bindValue(context, "array", sampleArray);
        Object value = evaluatorUnderTest.evaluateExpression(context,
                "${array[0].sampleValue}", String.class);
        assertNotNull(value);
    }

    @Test
    public void testList() {
        List<SampleBean> sampleList = createSampleList();
        evaluatorUnderTest.bindValue(context, "list", sampleList);
        Object value = evaluatorUnderTest.evaluateExpression(context,
                "${list[0].sampleValue}", String.class);
        assertNotNull(value);
        value = evaluatorUnderTest.evaluateExpression(context,
                "${list.get(0).sampleValue}", String.class);
        assertNotNull(value);
    }

}
