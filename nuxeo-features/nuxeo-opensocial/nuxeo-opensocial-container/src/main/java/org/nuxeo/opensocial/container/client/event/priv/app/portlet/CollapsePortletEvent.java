package org.nuxeo.opensocial.container.client.event.priv.app.portlet;

import org.nuxeo.opensocial.container.client.ui.api.HasId;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class CollapsePortletEvent extends GwtEvent<CollapsePortletEventHandler>
        implements HasId {
    public static Type<CollapsePortletEventHandler> TYPE = new Type<CollapsePortletEventHandler>();

    private String webContentId;

    public CollapsePortletEvent() {
    }

    public CollapsePortletEvent(String webContentId) {
        this.webContentId = webContentId;
    }

    public String getId() {
        return webContentId;
    }

    public void setId(String id) {
        this.webContentId = id;
    }

    @Override
    public Type<CollapsePortletEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(CollapsePortletEventHandler handler) {
        handler.onCollapsePortlet(this);
    }
}
