package org.nuxeo.opensocial.container.client.event.publ;

import com.google.gwt.event.shared.EventHandler;

/**
 * @author Stéphane Fourrier
 */
public interface UpdateWebContentEventHandler extends EventHandler {
    void onUpdateWebContent(UpdateWebContentEvent event);
}
