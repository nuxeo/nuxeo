package org.nuxeo.opensocial.container.client.event.priv.model;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class WebContentIdChangedEvent extends
        GwtEvent<WebContentIdChangedEventHandler> {
    public static Type<WebContentIdChangedEventHandler> TYPE = new Type<WebContentIdChangedEventHandler>();

    private String oldWebContentId;

    private String newWebContentId;

    public WebContentIdChangedEvent(String oldWebContentId,
            String newWebContentId) {
        this.oldWebContentId = oldWebContentId;
        this.newWebContentId = newWebContentId;
    }

    public String getOldWebContentId() {
        return oldWebContentId;
    }

    public String getNewWebContentId() {
        return newWebContentId;
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<WebContentIdChangedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(WebContentIdChangedEventHandler handler) {
        handler.onWebContentIdChange(this);
    }
}
