package org.nuxeo.opensocial.container.client.event.priv.app.portlet;

import org.nuxeo.opensocial.container.client.ui.api.HasId;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class MaximizePortletEvent extends GwtEvent<MaximizePortletEventHandler>
        implements HasId {
    public static Type<MaximizePortletEventHandler> TYPE = new Type<MaximizePortletEventHandler>();

    private String webContentId;

    public MaximizePortletEvent() {
    }

    public MaximizePortletEvent(String webContentId) {
        this.webContentId = webContentId;
    }

    public String getId() {
        return webContentId;
    }

    public void setId(String id) {
        this.webContentId = id;
    }

    @Override
    public Type<MaximizePortletEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(MaximizePortletEventHandler handler) {
        handler.onMaximizeWebContent(this);
    }
}
