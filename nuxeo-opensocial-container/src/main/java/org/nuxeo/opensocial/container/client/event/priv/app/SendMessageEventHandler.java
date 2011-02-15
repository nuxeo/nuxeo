package org.nuxeo.opensocial.container.client.event.priv.app;

import com.google.gwt.event.shared.EventHandler;

/**
 * @author Stéphane Fourrier
 */
public interface SendMessageEventHandler extends EventHandler {
    void onMessageSent(SendMessageEvent event);
}
