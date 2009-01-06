package org.nuxeo.ecm.core.convert.plugins.tests;

public class TestPPTConverter extends BaseConverterTest {

    // PPT POI tests fails in surefire

    public void testPptConverter() throws Exception {
        doTestTextConverter("application/vnd.ms-powerpoint", "ppt2text",
                "hello.ppt");
    }


    public void testAnyToTextConverter() throws Exception {
        doTestAny2TextConverter("application/vnd.ms-powerpoint",
                    "any2text", "hello.ppt");
    }

}
