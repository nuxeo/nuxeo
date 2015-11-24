package org.nuxeo.opensocial.container.client.gadgets.facets;

import org.nuxeo.opensocial.container.client.event.priv.app.portlet.SetPreferencesPortletEvent;
import org.nuxeo.opensocial.container.client.gadgets.facets.api.Facet;

/**
 * @author Stéphane Fourrier
 */
public class IsConfigurable extends Facet {
    public IsConfigurable() {
        super("facet-configure", new SetPreferencesPortletEvent());
    }
}
