package org.nuxeo.platform.el;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.ecm.platform.el.ExpressionEvaluator;

import de.odysseus.el.ExpressionFactoryImpl;

public class TestExpressionEvaluator extends TestCase {

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
    
    private ExpressionEvaluator evaluatorUnderTest = new ExpressionEvaluator(
            new ExpressionFactoryImpl());
    
    private ExpressionContext context = new ExpressionContext();
    
    private static Map<String, SampleBean> createSampleMap() {
        Map<String, SampleBean> sampleMap = new HashMap<String, SampleBean>();
        sampleMap.put("key", createSampleBean());
        return sampleMap;
    }


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
    
    public void testMap() {
        Map<String,SampleBean> sampleMap = createSampleMap();
        evaluatorUnderTest.bindValue(context, "map", sampleMap);
        Object value = evaluatorUnderTest.evaluateExpression(context,
                "${map.key.sampleValue}", String.class);
        assertNotNull(value);
    }
    
    public void testArray() {
        SampleBean[] sampleArray = createSampleArray();
        evaluatorUnderTest.bindValue(context, "array", sampleArray);
        Object value = evaluatorUnderTest.evaluateExpression(context,
                "${array[0].sampleValue}", String.class);
        assertNotNull(value);
    }

}
