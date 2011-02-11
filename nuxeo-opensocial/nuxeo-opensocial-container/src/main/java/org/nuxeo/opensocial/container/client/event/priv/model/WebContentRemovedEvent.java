package org.nuxeo.opensocial.container.client.event.priv.model;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class WebContentRemovedEvent extends
        GwtEvent<WebContentRemovedEventHandler> {
    public static Type<WebContentRemovedEventHandler> TYPE = new Type<WebContentRemovedEventHandler>();

    String WebContentId;

    public WebContentRemovedEvent(String WebContentId) {
        this.WebContentId = WebContentId;
    }

    public String getWebContentId() {
        return WebContentId;
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<WebContentRemovedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(WebContentRemovedEventHandler handler) {
        handler.onWebContentRemoved(this);
    }
}
