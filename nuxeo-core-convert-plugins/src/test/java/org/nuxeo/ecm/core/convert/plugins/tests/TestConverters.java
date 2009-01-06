package org.nuxeo.ecm.core.convert.plugins.tests;



public class TestConverters extends BaseConverterTest {


    public void testHTMLConverter() throws Exception {
        doTestTextConverter("text/html", "html2text", "hello.html");
    }

    public void testXMLConverter() throws Exception {
        doTestTextConverter("text/xml", "xml2text", "hello.xml");
    }

    public void testXlConverter() throws Exception {
        doTestTextConverter("application/vnd.ms-excel", "xl2text", "hello.xls");
    }

    public void testOOWriterConverter() throws Exception {
        doTestTextConverter("application/vnd.sun.xml.writer", "oo2text", "hello.sxw");
        doTestTextConverter("application/vnd.oasis.opendocument.text", "oo2text", "hello.odt");
    }

    public void testOOCalcConverter() throws Exception {
        doTestTextConverter("application/vnd.sun.xml.calc", "oo2text", "hello.sxc");
        doTestTextConverter("application/vnd.oasis.opendocument.spreadsheet", "oo2text", "hello.ods");
    }

    public void testOOPrezConverter() throws Exception {
        doTestTextConverter("application/vnd.sun.xml.impress", "oo2text", "hello.sxi");
        doTestTextConverter("application/vnd.oasis.opendocument.presentation", "oo2text", "hello.odp");
    }

    public void testPDFConverter() throws Exception {
        doTestTextConverter("application/pdf", "pdf2text", "hello.pdf");
    }

    public void testAnyToTextConverter() throws Exception {
        doTestAny2TextConverter("text/html", "any2text", "hello.html");
        doTestAny2TextConverter("text/xml", "any2text", "hello.xml");
        doTestAny2TextConverter("application/vnd.ms-excel", "any2text", "hello.xls");
        doTestAny2TextConverter("application/vnd.sun.xml.writer", "any2text", "hello.sxw");
        doTestAny2TextConverter("application/vnd.oasis.opendocument.text", "any2text", "hello.odt");
        doTestAny2TextConverter("application/vnd.sun.xml.calc", "any2text", "hello.sxc");
        doTestAny2TextConverter("application/vnd.oasis.opendocument.spreadsheet", "any2text", "hello.ods");
        doTestAny2TextConverter("application/vnd.sun.xml.impress", "any2text", "hello.sxi");
        doTestAny2TextConverter("application/vnd.oasis.opendocument.presentation", "any2text", "hello.odp");
        doTestAny2TextConverter("application/pdf", "any2text", "hello.pdf");
    }
}
