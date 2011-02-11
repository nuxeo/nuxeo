package org.nuxeo.opensocial.container.client.event.priv.model;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class FooterSelectedEvent extends GwtEvent<FooterSelectedEventHandler> {
    public static Type<FooterSelectedEventHandler> TYPE = new Type<FooterSelectedEventHandler>();

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<FooterSelectedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(FooterSelectedEventHandler handler) {
        handler.onSelectFooter(this);
    }
}
