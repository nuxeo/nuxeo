package org.nuxeo.ecm.platform.importer.filter;

import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.runtime.api.Framework;

public class EventServiceConfiguratorFilter implements ImporterFilter {

    protected Boolean blockSyncPostCommitProcessing;
    protected Boolean blockAsyncProcessing;
    protected Boolean blockMimeTypeDetection;
    protected Boolean bulkMode;
    protected EventServiceAdmin eventAdmin=null;

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
                eventAdmin.setListenerEnabledFlag("mimetypeIconUpdater", false);
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
            eventAdmin.setListenerEnabledFlag("mimetypeIconUpdater", true);
        }
        eventAdmin=null;
    }
}
