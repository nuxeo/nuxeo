package org.nuxeo.opensocial.container.client.event.publ;

import java.util.List;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class UpdateWebContentEvent extends
        GwtEvent<UpdateWebContentEventHandler> {
    public static Type<UpdateWebContentEventHandler> TYPE = new Type<UpdateWebContentEventHandler>();

    private String webContentId;

    private List<String> files;

    public UpdateWebContentEvent(String webContentId) {
        this.webContentId = webContentId;
    }

    public UpdateWebContentEvent(String webContentId, List<String> files) {
        this(webContentId);
        this.files = files;
    }

    public String getWebContentId() {
        return webContentId;
    }

    public List<String> getFiles() {
        return files;
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
