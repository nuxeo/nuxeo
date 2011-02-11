package org.nuxeo.opensocial.container.client.event.priv.presenter;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class ShowCodeEvent extends GwtEvent<ShowCodeEventHandler> {
    public static Type<ShowCodeEventHandler> TYPE = new Type<ShowCodeEventHandler>();

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<ShowCodeEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ShowCodeEventHandler handler) {
        handler.onShowCode(this);
    }
}
