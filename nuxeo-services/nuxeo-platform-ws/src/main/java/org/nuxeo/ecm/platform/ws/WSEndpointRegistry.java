package org.nuxeo.ecm.platform.ws;

import java.util.Collection;
import org.nuxeo.ecm.platform.api.ws.WSEndpointDescriptor;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Simple WSEndpoints registry
 *
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.7.2
 */
public class WSEndpointRegistry extends
        SimpleContributionRegistry<WSEndpointDescriptor> {

    @Override
    public String getContributionId(WSEndpointDescriptor contrib) {
        return contrib.name;
    }

    public Collection<WSEndpointDescriptor> getContributions() {
        return currentContribs.values();
    }
}
