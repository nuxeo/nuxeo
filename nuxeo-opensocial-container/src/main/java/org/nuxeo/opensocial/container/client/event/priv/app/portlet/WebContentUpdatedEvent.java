package org.nuxeo.opensocial.container.client.event.priv.app.portlet;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class WebContentUpdatedEvent extends
        GwtEvent<WebContentUpdatedEventHandler> {
    public static Type<WebContentUpdatedEventHandler> TYPE = new Type<WebContentUpdatedEventHandler>();

    private String webContentId;

    public WebContentUpdatedEvent() {
    }

    public WebContentUpdatedEvent(String webContentId) {
        this.webContentId = webContentId;
    }

    public String getWebContentId() {
        return webContentId;
    }

    @Override
    public Type<WebContentUpdatedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(WebContentUpdatedEventHandler handler) {
        handler.onWebContentUpdated(this);
    }
}
