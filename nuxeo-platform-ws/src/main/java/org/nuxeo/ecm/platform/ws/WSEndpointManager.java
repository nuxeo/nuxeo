package org.nuxeo.ecm.platform.ws;

import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.platform.api.ws.WSEndpointDescriptor;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.7.2
 */
public interface WSEndpointManager {
    Collection<WSEndpointDescriptor> getEndpoints();
}
