package org.nuxeo.opensocial.container.client.event.priv.model;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class HeaderSelectedEvent extends GwtEvent<HeaderSelectedEventHandler> {
    public static Type<HeaderSelectedEventHandler> TYPE = new Type<HeaderSelectedEventHandler>();

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<HeaderSelectedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(HeaderSelectedEventHandler handler) {
        handler.onSelectHeader(this);
    }
}
