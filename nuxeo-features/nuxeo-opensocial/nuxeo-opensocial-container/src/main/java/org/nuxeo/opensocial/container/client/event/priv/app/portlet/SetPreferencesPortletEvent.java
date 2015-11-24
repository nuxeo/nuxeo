package org.nuxeo.opensocial.container.client.event.priv.app.portlet;

import org.nuxeo.opensocial.container.client.ui.api.HasId;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class SetPreferencesPortletEvent extends
        GwtEvent<SetPreferencesPortletEventHandler> implements HasId {
    public static Type<SetPreferencesPortletEventHandler> TYPE = new Type<SetPreferencesPortletEventHandler>();

    private String webContentId;

    public SetPreferencesPortletEvent() {
    }

    public SetPreferencesPortletEvent(String webContentId) {
        this.webContentId = webContentId;
    }

    public String getId() {
        return webContentId;
    }

    public void setId(String id) {
        this.webContentId = id;
    }

    @Override
    public Type<SetPreferencesPortletEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(SetPreferencesPortletEventHandler handler) {
        handler.onSetPreferences(this);
    }
}
