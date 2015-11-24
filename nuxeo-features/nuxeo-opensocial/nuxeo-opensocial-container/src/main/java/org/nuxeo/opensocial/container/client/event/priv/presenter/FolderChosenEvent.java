package org.nuxeo.opensocial.container.client.event.priv.presenter;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Stéphane Fourrier
 */
public class FolderChosenEvent extends GwtEvent<FolderChosenEventHandler> {
    public static Type<FolderChosenEventHandler> TYPE = new Type<FolderChosenEventHandler>();

    private String folderId;

    private String folderName;

    public FolderChosenEvent(String folderId, String folderName) {
        this.folderId = folderId;
        this.folderName = folderName;
    }

    @Override
    public com.google.gwt.event.shared.GwtEvent.Type<FolderChosenEventHandler> getAssociatedType() {
        return TYPE;
    }

    public String getFolderId() {
        return folderId;
    }

    public String getFolderName() {
        return folderName;
    }

    @Override
    protected void dispatch(FolderChosenEventHandler handler) {
        handler.onFolderChosen(this);
    }
}
