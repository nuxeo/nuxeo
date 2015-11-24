package org.nuxeo.opensocial.container.client.event.priv.app;

import com.google.gwt.event.shared.EventHandler;

/**
 * @author Stéphane Fourrier
 */
public interface HideMessageEventHandler extends EventHandler {
    void onMessageHidden(HideMessageEvent event);
}
