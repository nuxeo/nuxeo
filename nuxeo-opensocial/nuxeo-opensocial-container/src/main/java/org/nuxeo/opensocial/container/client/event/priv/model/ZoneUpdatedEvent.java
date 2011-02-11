package org.nuxeo.opensocial.container.client.event.priv.model;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class ZoneUpdatedEvent extends GwtEvent<ZoneUpdatedEventHandler> {
    public static Type<ZoneUpdatedEventHandler> TYPE = new Type<ZoneUpdatedEventHandler>();

    private final int id;

    public ZoneUpdatedEvent(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<ZoneUpdatedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ZoneUpdatedEventHandler handler) {
        handler.onUpdateRow(this);
    }
}
