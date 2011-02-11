package org.nuxeo.opensocial.container.client.event.priv.app.portlet;

import org.nuxeo.opensocial.container.client.ui.api.HasId;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class MinimizePortletEvent extends GwtEvent<MinimizePortletEventHandler>
        implements HasId {
    public static Type<MinimizePortletEventHandler> TYPE = new Type<MinimizePortletEventHandler>();

    private String webContentId;

    public MinimizePortletEvent() {
    }

    public MinimizePortletEvent(String webContentId) {
        this.webContentId = webContentId;
    }

    public String getId() {
        return webContentId;
    }

    public void setId(String id) {
        this.webContentId = id;
    }

    @Override
    public Type<MinimizePortletEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(MinimizePortletEventHandler handler) {
        handler.onMinimizeWebContent(this);
    }
}
