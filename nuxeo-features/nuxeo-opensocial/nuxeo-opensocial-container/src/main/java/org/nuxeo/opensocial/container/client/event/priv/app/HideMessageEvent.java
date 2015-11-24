package org.nuxeo.opensocial.container.client.event.priv.app;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class HideMessageEvent extends GwtEvent<HideMessageEventHandler> {
    public static Type<HideMessageEventHandler> TYPE = new Type<HideMessageEventHandler>();

    public HideMessageEvent() {
    }

    @Override
    public Type<HideMessageEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(HideMessageEventHandler handler) {
        handler.onMessageHidden(this);
    }
}
