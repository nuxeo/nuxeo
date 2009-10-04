package org.nuxeo.wss.fprpc.tests.xmlconfig;

import java.util.List;

import org.nuxeo.wss.servlet.config.FilterBindingConfig;
import org.nuxeo.wss.servlet.config.XmlConfigHandler;

import junit.framework.TestCase;

public class TestXmlConfig extends TestCase {


    public void testParsing() throws Exception {

        List<FilterBindingConfig> bindings = XmlConfigHandler.getConfigEntries();
        assertTrue(bindings.size()>0);

        FilterBindingConfig binding = bindings.get(0);
        assertEquals("(.*)/_vti_inf.html.*",binding.getUrl());
        assertEquals("GET",binding.getRequestType());
        assertEquals("VtiHandler",binding.getTargetService());
        assertEquals(null,binding.getRedirectURL());



    }
}
