package org.nuxeo.opensocial.container.client.event.priv.model;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class SideBarChangedEvent extends GwtEvent<SideBarChangedEventHandler> {
    public static Type<SideBarChangedEventHandler> TYPE = new Type<SideBarChangedEventHandler>();

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<SideBarChangedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(SideBarChangedEventHandler handler) {
        handler.onChangeSideBarPosition(this);
    }
}
