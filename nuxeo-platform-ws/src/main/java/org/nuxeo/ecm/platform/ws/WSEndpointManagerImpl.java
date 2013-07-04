package org.nuxeo.ecm.platform.ws;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.ws.WSEndpointDescriptor;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.7
 */
public class WSEndpointManagerImpl extends DefaultComponent implements
        WSEndpointManager {

    public static final String ENDPOINT_EP = "endpoint";

    private static final Log log = LogFactory.getLog(WSEndpointManagerImpl.class);

    protected WSEndpointRegistry regitry = new WSEndpointRegistry();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (ENDPOINT_EP.equals(extensionPoint)) {
            regitry.addContribution((WSEndpointDescriptor) contribution);
        } else {
            log.info("Unknown extension point: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (ENDPOINT_EP.equals(extensionPoint)) {
            regitry.removeContribution((WSEndpointDescriptor) contribution);
        }
    }

    @Override
    public Collection<WSEndpointDescriptor> getEndpoints() {
        return regitry.getContributions();
    }
}
