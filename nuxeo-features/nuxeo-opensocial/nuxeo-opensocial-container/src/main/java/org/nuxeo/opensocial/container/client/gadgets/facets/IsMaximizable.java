package org.nuxeo.opensocial.container.client.gadgets.facets;

import org.nuxeo.opensocial.container.client.event.priv.app.portlet.MaximizePortletEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.MinimizePortletEvent;
import org.nuxeo.opensocial.container.client.gadgets.facets.api.Facet;

/**
 * @author Stéphane Fourrier
 */
public class IsMaximizable extends Facet {
    public IsMaximizable() {
        super("facet-maximize", new MaximizePortletEvent(), "facet-minimize",
                new MinimizePortletEvent());
    }
}
