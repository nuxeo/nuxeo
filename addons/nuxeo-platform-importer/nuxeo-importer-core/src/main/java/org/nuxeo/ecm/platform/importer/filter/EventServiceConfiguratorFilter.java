/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 */
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

    // @since 8.3
    protected boolean blockIndexing = false;

    protected boolean bulkMode = false;

    protected static final String NOTIF_LISTENER = "notificationListener";

    protected static final String MIME_LISTENER = "mimetypeIconUpdater";

    protected static final String INDEXING_LISTENER = "elasticSearchInlineListener";

    public EventServiceConfiguratorFilter(Boolean blockSyncPostCommitProcessing, Boolean blockAsyncProcessing,
                                          Boolean blockMimeTypeDetection, Boolean blockIndexing, Boolean bulkMode) {
        if (blockAsyncProcessing != null) {
            this.blockAsyncProcessing = blockAsyncProcessing;
        }
        if (blockSyncPostCommitProcessing != null) {
            this.blockSyncPostCommitProcessing = blockSyncPostCommitProcessing;
        }
        if (blockMimeTypeDetection != null) {
            this.blockMimeTypeDetection = blockMimeTypeDetection;
        }
        if (blockIndexing != null) {
            this.blockIndexing = blockIndexing;
        }
        if (bulkMode != null) {
            this.bulkMode = bulkMode;
        }
    }

    public void handleBeforeImport() {
        EventServiceAdmin eventAdmin = Framework.getService(EventServiceAdmin.class);
        if (eventAdmin == null) {
            log.error("EventServiceAdmin service was not found ... Possible that the import process will not proceed ok");
            return;
        }
        eventAdmin.setBulkModeEnabled(bulkMode);
        eventAdmin.setBlockAsyncHandlers(blockAsyncProcessing);
        eventAdmin.setBlockSyncPostCommitHandlers(blockSyncPostCommitProcessing);
        if (blockMimeTypeDetection) {
            eventAdmin.setListenerEnabledFlag(MIME_LISTENER, false);
        }
        if (blockNotifications) {
            eventAdmin.setListenerEnabledFlag(NOTIF_LISTENER, false);
        }
        if (blockIndexing) {
            eventAdmin.setListenerEnabledFlag(INDEXING_LISTENER, false);
        }
    }

    public void handleAfterImport(Exception e) {
        EventServiceAdmin eventAdmin = Framework.getService(EventServiceAdmin.class);
        if (eventAdmin != null) {
            log.info("Restoring default event listeners and bulk mode");
            eventAdmin.setBulkModeEnabled(false);
            eventAdmin.setBlockAsyncHandlers(false);
            eventAdmin.setBlockSyncPostCommitHandlers(false);
            eventAdmin.setListenerEnabledFlag(NOTIF_LISTENER, true);
            eventAdmin.setListenerEnabledFlag(MIME_LISTENER, true);
            eventAdmin.setListenerEnabledFlag(INDEXING_LISTENER, true);
        }
    }

    public boolean getBlockNotifications() {
        return blockNotifications;
    }

    public void setBlockNotifications(boolean blockNotifications) {
        this.blockNotifications = blockNotifications;
    }

    public String toString() {
        return String.format(
                "blockSyncPostCommitProcessing set %b, blockAsyncProcessing set %b, blockMimeTypeDetection set %b, blockNotifications set %b, blockIndexing set %b, bulkMode set %b",
                blockSyncPostCommitProcessing, blockAsyncProcessing, blockMimeTypeDetection, blockNotifications, blockIndexing, bulkMode);
    }
}
