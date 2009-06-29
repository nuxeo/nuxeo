package org.nuxeo.ecm.platform.importer.filter;

import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.runtime.api.Framework;

public class EventServiceConfiguratorFilter implements ImporterFilter {

    protected Boolean blockSyncPostCommitProcessing;
    protected Boolean blockAsyncProcessing;
    protected Boolean blockMimeTypeDetection;
    protected boolean blockNotifications = true;
    protected Boolean bulkMode;
    protected EventServiceAdmin eventAdmin=null;

    protected static final String NOTIF_LISTENER = "notificationListener";
    protected static final String MIME_LISTENER = "mimetypeIconUpdater";

    public EventServiceConfiguratorFilter(Boolean blockSyncPostCommitProcessing,Boolean blockAsyncProcessing,Boolean blockMimeTypeDetection, Boolean bulkMode) {
        this.blockAsyncProcessing=blockAsyncProcessing;
        this.blockSyncPostCommitProcessing = blockSyncPostCommitProcessing;
        this.blockMimeTypeDetection = blockMimeTypeDetection;
        this.bulkMode = bulkMode;
    }

    public void handleBeforeImport() {
        eventAdmin = Framework.getLocalService(EventServiceAdmin.class);

        if (eventAdmin!=null) {
            if (true == bulkMode) {
                eventAdmin.setBulkModeEnabled(true);
            }
            if (true == blockMimeTypeDetection) {
                eventAdmin.setListenerEnabledFlag(MIME_LISTENER, false);
            }
            if (true == blockNotifications) {
                eventAdmin.setListenerEnabledFlag(NOTIF_LISTENER, false);
            }
            if (true == blockAsyncProcessing) {
                eventAdmin.setBlockAsyncHandlers(true);
            } else {
                eventAdmin.setBlockAsyncHandlers(false);
            }
            if (true == blockSyncPostCommitProcessing) {
                eventAdmin.setBlockSyncPostCommitHandlers(true);
            } else {
                eventAdmin.setBlockSyncPostCommitHandlers(false);
            }
        }
    }

    public void handleAfterImport(Exception e) {
        if (eventAdmin!=null) {
            eventAdmin.setBulkModeEnabled(false);
            eventAdmin.setBlockAsyncHandlers(false);
            eventAdmin.setBlockSyncPostCommitHandlers(false);
            eventAdmin.setListenerEnabledFlag(NOTIF_LISTENER, true);
            eventAdmin.setListenerEnabledFlag(MIME_LISTENER, true);
        }
        eventAdmin=null;
    }

    public boolean getBlockNotifications() {
        return blockNotifications;
    }

    public void setBlockNotifications(boolean blockNotifications) {
        this.blockNotifications = blockNotifications;
    }
}
