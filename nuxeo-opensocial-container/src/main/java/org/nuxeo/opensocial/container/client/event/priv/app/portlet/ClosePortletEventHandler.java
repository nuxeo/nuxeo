package org.nuxeo.opensocial.container.client.event.priv.app.portlet;

import com.google.gwt.event.shared.EventHandler;

/**
 * @author Stéphane Fourrier
 */
public interface ClosePortletEventHandler extends EventHandler {
    void onCloseWebContent(ClosePortletEvent event);
}
