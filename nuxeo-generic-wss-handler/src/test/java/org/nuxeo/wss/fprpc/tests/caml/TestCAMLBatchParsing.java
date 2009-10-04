package org.nuxeo.wss.fprpc.tests.caml;

import java.io.InputStream;
import java.util.List;

import junit.framework.TestCase;

import org.nuxeo.wss.fprpc.CAMLHandler;
import org.nuxeo.wss.fprpc.FPRPCCall;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

public class TestCAMLBatchParsing extends TestCase {

    public void testParsing() throws Exception {

        InputStream xmlStream = this.getClass().getClassLoader().getResourceAsStream("SimpleCAMLBatch.xml");
        XMLReader reader = CAMLHandler.getXMLReader();
        CAMLHandler handler = (CAMLHandler) reader.getContentHandler();
        reader.parse(new InputSource(xmlStream));

        List<FPRPCCall> calls = handler.getParsedCalls();
        assertNotNull(calls);
        assertEquals(2, calls.size());

        assertEquals("0,NewList", calls.get(0).getId());
        assertEquals("NewList", calls.get(0).getMethodName());
        assertEquals("Meeting Topics", calls.get(0).getParameters().get("Title"));
        assertEquals("100", calls.get(0).getParameters().get("ListTemplate"));
        assertEquals(2, calls.get(0).getParameters().size());

        assertEquals("1,NewList", calls.get(1).getId());
        assertEquals("NewList", calls.get(1).getMethodName());
        assertEquals("Volunteers", calls.get(1).getParameters().get("Title"));
        assertEquals("100", calls.get(1).getParameters().get("ListTemplate"));
        assertEquals(2, calls.get(1).getParameters().size());
    }

}
