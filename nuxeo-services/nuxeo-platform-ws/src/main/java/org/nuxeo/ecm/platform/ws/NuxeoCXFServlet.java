package org.nuxeo.ecm.platform.ws;

import javax.servlet.ServletConfig;

import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.7.3
 */
public class NuxeoCXFServlet extends CXFNonSpringServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void loadBus(ServletConfig sc) {
        setBus(BusFactory.getDefaultBus());
    }

}
