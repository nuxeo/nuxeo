package org.nuxeo.opensocial.container.client.event.priv.model;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class ZoneAddedEvent extends GwtEvent<ZoneAddedEventHandler> {
    public static Type<ZoneAddedEventHandler> TYPE = new Type<ZoneAddedEventHandler>();

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<ZoneAddedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ZoneAddedEventHandler handler) {
        handler.onAddRow(this);
    }
}
