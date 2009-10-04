package org.nuxeo.wss.fprpc.tests.xmlconfig;

import org.nuxeo.wss.servlet.config.FilterBindingConfig;
import org.nuxeo.wss.servlet.config.FilterBindingResolver;

import junit.framework.TestCase;

public class TestBindings extends TestCase {


    public void testBindings() throws Exception {

        FilterBindingConfig config = FilterBindingResolver.getBinding("/_vti_inf.html");
        assertNotNull(config);
        assertEquals("VtiHandler", config.getTargetService());
        assertEquals("", config.getSiteName());

        config = FilterBindingResolver.getBinding("/_vti_inf.html?XXX=YYYY");
        assertNotNull(config);
        assertEquals("VtiHandler", config.getTargetService());
        assertEquals("", config.getSiteName());

        config = FilterBindingResolver.getBinding("/server/_vti_inf.html?UUUU=VVVV");
        assertNotNull(config);
        assertEquals("VtiHandler", config.getTargetService());
        assertEquals("/server", config.getSiteName());

    }
}
