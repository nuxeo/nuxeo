package org.nuxeo.opensocial.container.client.event.publ;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class UpdateWebContentEvent extends
        GwtEvent<UpdateWebContentEventHandler> {
    public static Type<UpdateWebContentEventHandler> TYPE = new Type<UpdateWebContentEventHandler>();

    private String webContentId;

    public UpdateWebContentEvent(String webContentId) {
        this.webContentId = webContentId;
    }

    public String getWebContentId() {
        return webContentId;
    }

    @Override
    protected void dispatch(UpdateWebContentEventHandler handler) {
        handler.onUpdateWebContent(this);
    }

    @Override
    public Type<UpdateWebContentEventHandler> getAssociatedType() {
        return TYPE;
    }
}
