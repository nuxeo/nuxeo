package org.nuxeo.opensocial.container.client.event.priv.service;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class WebContentsLoadedEvent extends
        GwtEvent<WebContentsLoadedEventHandler> {
    public static Type<WebContentsLoadedEventHandler> TYPE = new Type<WebContentsLoadedEventHandler>();

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<WebContentsLoadedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(WebContentsLoadedEventHandler handler) {
        handler.onWebContentsLoaded(this);
    }
}
