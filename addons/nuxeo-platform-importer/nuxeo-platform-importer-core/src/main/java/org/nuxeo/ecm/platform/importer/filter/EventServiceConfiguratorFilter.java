package org.nuxeo.ecm.platform.importer.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.runtime.api.Framework;

public class EventServiceConfiguratorFilter implements ImporterFilter {

    private static final Log log = LogFactory.getLog(EventServiceConfiguratorFilter.class);

    protected boolean blockSyncPostCommitProcessing = false;

    protected boolean blockAsyncProcessing = false;

    protected boolean blockMimeTypeDetection = false;

    protected boolean blockNotifications = true;

    protected boolean bulkMode = false;

    protected EventServiceAdmin eventAdmin = null;

    protected static final String NOTIF_LISTENER = "notificationListener";

    protected static final String MIME_LISTENER = "mimetypeIconUpdater";

    public EventServiceConfiguratorFilter(
            Boolean blockSyncPostCommitProcessing,
            Boolean blockAsyncProcessing, Boolean blockMimeTypeDetection,
            Boolean bulkMode) {
        if (blockAsyncProcessing != null) {
            this.blockAsyncProcessing = blockAsyncProcessing;
        }
        if (blockSyncPostCommitProcessing != null) {
            this.blockSyncPostCommitProcessing = blockSyncPostCommitProcessing;
        }
        if (blockMimeTypeDetection != null) {
            this.blockMimeTypeDetection = blockMimeTypeDetection;
        }
        if (bulkMode != null) {
            this.bulkMode = bulkMode;
        }
    }

    public void handleBeforeImport() {
        eventAdmin = Framework.getLocalService(EventServiceAdmin.class);

        if (eventAdmin != null) {
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
        } else {
            log.warn("EventServiceAdmin service was not found ... Possible that the import process will not proceed ok");
        }
    }

    public void handleAfterImport(Exception e) {
        if (eventAdmin != null) {
            eventAdmin.setBulkModeEnabled(false);
            eventAdmin.setBlockAsyncHandlers(false);
            eventAdmin.setBlockSyncPostCommitHandlers(false);
            eventAdmin.setListenerEnabledFlag(NOTIF_LISTENER, true);
            eventAdmin.setListenerEnabledFlag(MIME_LISTENER, true);
        }
        eventAdmin = null;
    }

    public boolean getBlockNotifications() {
        return blockNotifications;
    }

    public void setBlockNotifications(boolean blockNotifications) {
        this.blockNotifications = blockNotifications;
    }

    public String toString() {
        return String.format(
                "blockSyncPostCommitProcessing set %b, blockAsyncProcessing set %b, blockMimeTypeDetection set %b and blockNotifications set %b",
                blockSyncPostCommitProcessing, blockAsyncProcessing,
                blockMimeTypeDetection, blockNotifications);
    }
}
