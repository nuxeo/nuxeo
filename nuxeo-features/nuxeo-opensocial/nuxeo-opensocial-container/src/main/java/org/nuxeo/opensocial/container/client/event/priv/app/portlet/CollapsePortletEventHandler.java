package org.nuxeo.opensocial.container.client.event.priv.app.portlet;

import com.google.gwt.event.shared.EventHandler;

/**
 * @author Stéphane Fourrier
 */
public interface CollapsePortletEventHandler extends EventHandler {
    void onCollapsePortlet(CollapsePortletEvent event);
}
