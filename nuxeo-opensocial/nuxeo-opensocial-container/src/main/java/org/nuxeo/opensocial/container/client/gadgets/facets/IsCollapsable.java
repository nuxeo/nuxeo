package org.nuxeo.opensocial.container.client.gadgets.facets;

import org.nuxeo.opensocial.container.client.event.priv.app.portlet.CollapsePortletEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.UncollapsePortletEvent;
import org.nuxeo.opensocial.container.client.gadgets.facets.api.Facet;

/**
 * @author Stéphane Fourrier
 */
public class IsCollapsable extends Facet {
    public IsCollapsable() {
        super("facet-collapse", new CollapsePortletEvent(), "facet-collapsed",
                new UncollapsePortletEvent());
    }
}
