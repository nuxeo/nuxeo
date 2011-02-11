package org.nuxeo.opensocial.container.client.event.priv.app.portlet;

import org.nuxeo.opensocial.container.client.ui.api.HasId;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class UncollapsePortletEvent extends
        GwtEvent<UncollapsePortletEventHandler> implements HasId {
    public static Type<UncollapsePortletEventHandler> TYPE = new Type<UncollapsePortletEventHandler>();

    private String webContentId;

    public UncollapsePortletEvent() {
    }

    public UncollapsePortletEvent(String webContentId) {
        this.webContentId = webContentId;
    }

    public String getId() {
        return webContentId;
    }

    public void setId(String id) {
        this.webContentId = id;
    }

    @Override
    public Type<UncollapsePortletEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(UncollapsePortletEventHandler handler) {
        handler.onUncollapsePortlet(this);
    }
}
