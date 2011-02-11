package org.nuxeo.opensocial.container.client.event.priv.service;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class LayoutLoadedEvent extends GwtEvent<LayoutLoadedEventHandler> {
    public static Type<LayoutLoadedEventHandler> TYPE = new Type<LayoutLoadedEventHandler>();

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<LayoutLoadedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(LayoutLoadedEventHandler handler) {
        handler.onLayoutLoaded(this);
    }
}
