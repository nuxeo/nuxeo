package org.nuxeo.opensocial.container.client.event.priv.model;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class ContainerSizeChangedEvent extends
        GwtEvent<ContainerSizeChangedEventHandler> {
    public static Type<ContainerSizeChangedEventHandler> TYPE = new Type<ContainerSizeChangedEventHandler>();

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<ContainerSizeChangedEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ContainerSizeChangedEventHandler handler) {
        handler.onChangeContainerSize(this);
    }
}
