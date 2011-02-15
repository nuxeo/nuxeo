package org.nuxeo.opensocial.container.client.event.priv.model;

import com.google.gwt.event.shared.EventHandler;

/**
 * @author Stéphane Fourrier
 */
public interface FooterSelectedEventHandler extends EventHandler {
    void onSelectFooter(FooterSelectedEvent event);
}
