package org.nuxeo.ecm.core.convert.plugins.tests;


public class TestWordConverter extends BaseConverterTest {

    // Word POI tests fails in surefire

    public void testWordConverter() throws Exception {
        doTestTextConverter("application/msword", "word2text", "hello.doc");
    }

    public void testAnyToTextConverter() throws Exception {
        doTestAny2TextConverter("application/msword", "any2text", "hello.doc");
    }
}
