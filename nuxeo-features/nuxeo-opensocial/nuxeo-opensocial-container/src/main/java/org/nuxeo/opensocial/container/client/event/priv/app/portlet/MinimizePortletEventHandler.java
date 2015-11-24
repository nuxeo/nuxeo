package org.nuxeo.opensocial.container.client.event.priv.app.portlet;

import com.google.gwt.event.shared.EventHandler;

/**
 * @author Stéphane Fourrier
 */
public interface MinimizePortletEventHandler extends EventHandler {
    void onMinimizeWebContent(MinimizePortletEvent event);
}
