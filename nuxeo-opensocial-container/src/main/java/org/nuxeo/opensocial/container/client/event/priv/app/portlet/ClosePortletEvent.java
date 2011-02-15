package org.nuxeo.opensocial.container.client.event.priv.app.portlet;

import org.nuxeo.opensocial.container.client.ui.api.HasId;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class ClosePortletEvent extends GwtEvent<ClosePortletEventHandler>
        implements HasId {
    public static Type<ClosePortletEventHandler> TYPE = new Type<ClosePortletEventHandler>();

    private String webContentId;

    public ClosePortletEvent() {
    }

    public ClosePortletEvent(String webContentId) {
        this.webContentId = webContentId;
    }

    public String getId() {
        return webContentId;
    }

    public void setId(String id) {
        this.webContentId = id;
    }

    @Override
    public Type<ClosePortletEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ClosePortletEventHandler handler) {
        handler.onCloseWebContent(this);
    }
}
