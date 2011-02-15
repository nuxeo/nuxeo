package org.nuxeo.opensocial.container.client.gadgets.facets;

import org.nuxeo.opensocial.container.client.event.priv.app.portlet.ClosePortletEvent;
import org.nuxeo.opensocial.container.client.gadgets.facets.api.Facet;

/**
 * @author Stéphane Fourrier
 */
public class IsClosable extends Facet {
    public IsClosable() {
        super("facet-close", new ClosePortletEvent());
    }
}
