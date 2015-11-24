package org.nuxeo.opensocial.container.client.event.priv.model;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class WebContentMovedEvent extends GwtEvent<WebContentMovedEventHandler> {
    public static Type<WebContentMovedEventHandler> TYPE = new Type<WebContentMovedEventHandler>();

    private final String fromUnitName;

    private final int fromWebContentPosition;

    private final String toUnitName;

    private final int toWebContentPosition;

    public WebContentMovedEvent(String fromUnitName,
            int fromWebContentPosition, String toUnitName,
            int toWebContentPosition) {
        this.fromUnitName = fromUnitName;
        this.fromWebContentPosition = fromWebContentPosition;
        this.toUnitName = toUnitName;
        this.toWebContentPosition = toWebContentPosition;
    }

    public String getFromUnitName() {
        return fromUnitName;
    }

    public int getFromWebContentPosition() {
        return fromWebContentPosition;
    }

    public String getToUnitName() {
        return toUnitName;
    }

    public int getToWebContentPosition() {
        return toWebContentPosition;
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<WebContentMovedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(WebContentMovedEventHandler handler) {
        handler.onWebContentHasMoved(this);
    }
}
