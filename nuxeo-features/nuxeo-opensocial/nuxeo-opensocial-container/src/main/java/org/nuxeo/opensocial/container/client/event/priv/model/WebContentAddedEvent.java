package org.nuxeo.opensocial.container.client.event.priv.model;

import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class WebContentAddedEvent extends GwtEvent<WebContentAddedEventHandler> {
    public static Type<WebContentAddedEventHandler> TYPE = new Type<WebContentAddedEventHandler>();

    private final WebContentData abstractData;

    public WebContentAddedEvent(WebContentData webContentData) {
        this.abstractData = webContentData;
    }

    public WebContentData getAbstractData() {
        return abstractData;
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<WebContentAddedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(WebContentAddedEventHandler handler) {
        handler.onAddWebContent(this);
    }
}
