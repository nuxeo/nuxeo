package org.nuxeo.opensocial.container.client.event.priv.model;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class ZoneDeletedEvent extends GwtEvent<ZoneRowDeletedEventHandler> {
    public static Type<ZoneRowDeletedEventHandler> TYPE = new Type<ZoneRowDeletedEventHandler>();

    private final int id;

    public ZoneDeletedEvent(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<ZoneRowDeletedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ZoneRowDeletedEventHandler handler) {
        handler.onRowDeleted(this);
    }
}
