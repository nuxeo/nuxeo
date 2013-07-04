package org.nuxeo.ecm.platform.ws;

import javax.servlet.ServletConfig;
import javax.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.nuxeo.ecm.platform.api.ws.WSEndpointDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.7.2
 */
public class NuxeoCXFServlet extends CXFNonSpringServlet {

    @Override
    protected void loadBus(ServletConfig sc) {
        super.loadBus(sc);

        Bus bus = getBus();
        BusFactory.setDefaultBus(bus);

        WSEndpointManager endpointManager = Framework.getLocalService(WSEndpointManager.class);
        for (WSEndpointDescriptor desc : endpointManager.getEndpoints()) {
            Endpoint.publish(desc.address, desc.getImplementorInstance());
        }
    }
}
