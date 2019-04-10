/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.importer.stream.consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.pattern.Message;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerFactory;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPolicy;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPool;
import org.nuxeo.runtime.api.Framework;

/**
 * Consumer Pool that block Nuxeo listeners during import.
 *
 * @since 9.1
 */
public class DocumentConsumerPool<M extends Message> extends ConsumerPool<M> {
    private static final Log log = LogFactory.getLog(DocumentConsumerPool.class);

    protected static final String NOTIF_LISTENER = "notificationListener";

    protected static final String MIME_LISTENER = "mimetypeIconUpdater";

    protected static final String INDEXING_LISTENER = "elasticSearchInlineListener";

    protected static final String DUBLICORE_LISTENER = "dclistener";

    protected static final String TPL_LISTENER = "templateCreator";

    protected static final String BINARY_LISTENER = "binaryMetadataSyncListener";

    protected static final String UID_LISTENER = "uidlistener";

    protected static final String VIDEO_LISTENER = "videoChangedListener";

    protected static final String PICTURE_LISTENER = "pictureViewsGenerationListener";

    protected static final String BLOB_LISTENER = "checkBlobUpdate";

    protected boolean blockAsync;

    protected final DocumentConsumerPolicy policy;

    protected boolean blockPostCommit;

    protected boolean bulkMode;

    protected boolean listenerIndexingEnabled;

    protected boolean listenerNotifEnabled;

    protected boolean listenerMimeEnabled;

    protected boolean listenerDublincoreEnabled;

    protected boolean listenerTplEnabled;

    protected boolean listenerBinaryEnabled;

    protected boolean listenerUidEnabled;

    protected boolean listenerVideoEnabled;

    protected boolean listenerPictureEnabled;

    protected boolean listenerBlobEnabled;

    public DocumentConsumerPool(String logName, LogManager manager, ConsumerFactory<M> factory,
            ConsumerPolicy consumerPolicy) {
        super(logName, manager, factory, consumerPolicy);
        EventServiceAdmin eventAdmin = Framework.getService(EventServiceAdmin.class);
        policy = (DocumentConsumerPolicy) consumerPolicy;
        if (eventAdmin == null) {
            log.info("Can not apply document policy there is no event service available");
            return;
        }
        if (policy.blockAsyncListeners()) {
            blockAsync = eventAdmin.isBlockAsyncHandlers();
            eventAdmin.setBlockAsyncHandlers(true);
            log.debug("Block asynchronous listeners");
        }
        if (policy.blockPostCommitListeners()) {
            blockPostCommit = eventAdmin.isBlockSyncPostCommitHandlers();
            eventAdmin.setBlockSyncPostCommitHandlers(true);
            log.debug("Block post commit listeners");
        }
        if (policy.bulkMode()) {
            bulkMode = eventAdmin.isBulkModeEnabled();
            eventAdmin.setBulkModeEnabled(true);
            log.debug("Enable bulk mode");
        }
        if (policy.blockIndexing()) {
            listenerIndexingEnabled = disableSyncListener(eventAdmin, INDEXING_LISTENER);
            log.debug("Block ES indexing");
        }
        if (policy.blockDefaultSyncListeners()) {
            listenerNotifEnabled = disableSyncListener(eventAdmin, NOTIF_LISTENER);
            listenerMimeEnabled = disableSyncListener(eventAdmin, MIME_LISTENER);
            listenerDublincoreEnabled = disableSyncListener(eventAdmin, DUBLICORE_LISTENER);
            listenerTplEnabled = disableSyncListener(eventAdmin, TPL_LISTENER);
            listenerBinaryEnabled = disableSyncListener(eventAdmin, BINARY_LISTENER);
            listenerUidEnabled = disableSyncListener(eventAdmin, UID_LISTENER);
            listenerVideoEnabled = disableSyncListener(eventAdmin, VIDEO_LISTENER);
            listenerPictureEnabled = disableSyncListener(eventAdmin, PICTURE_LISTENER);
            listenerBlobEnabled = disableSyncListener(eventAdmin, BLOB_LISTENER);
            log.debug("Block some default synchronous listener");
        }
    }

    protected boolean disableSyncListener(EventServiceAdmin eventAdmin, String name) {
        EventListenerDescriptor desc = eventAdmin.getListenerList().getDescriptor(name);
        if (desc != null && desc.isEnabled()) {
            eventAdmin.setListenerEnabledFlag(name, false);
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        super.close();

        EventServiceAdmin eventAdmin = Framework.getService(EventServiceAdmin.class);
        if (eventAdmin == null) {
            return;
        }
        if (policy.blockAsyncListeners()) {
            eventAdmin.setBlockAsyncHandlers(blockAsync);
            log.debug("Restore asynchronous listeners blocking state: " + blockAsync);
        }
        if (policy.blockPostCommitListeners()) {
            eventAdmin.setBlockSyncPostCommitHandlers(blockPostCommit);
            log.debug("Restore post commit listeners blocking state: " + blockPostCommit);
        }
        if (policy.bulkMode()) {
            eventAdmin.setBulkModeEnabled(bulkMode);
            log.debug("Restore bulk mode: " + bulkMode);
        }
        if (policy.blockIndexing() && listenerIndexingEnabled) {
            eventAdmin.setListenerEnabledFlag(INDEXING_LISTENER, true);
            log.debug("Unblock ES indexing");
        }
        if (policy.blockDefaultSyncListeners()) {
            if (listenerNotifEnabled) {
                eventAdmin.setListenerEnabledFlag(NOTIF_LISTENER, true);
            }
            if (listenerMimeEnabled) {
                eventAdmin.setListenerEnabledFlag(MIME_LISTENER, true);
            }
            if (listenerDublincoreEnabled) {
                eventAdmin.setListenerEnabledFlag(DUBLICORE_LISTENER, true);
            }
            if (listenerTplEnabled) {
                eventAdmin.setListenerEnabledFlag(TPL_LISTENER, true);
            }
            if (listenerBinaryEnabled) {
                eventAdmin.setListenerEnabledFlag(BINARY_LISTENER, true);
            }
            if (listenerUidEnabled) {
                eventAdmin.setListenerEnabledFlag(UID_LISTENER, true);
            }
            if (listenerVideoEnabled) {
                eventAdmin.setListenerEnabledFlag(VIDEO_LISTENER, true);
            }
            if (listenerPictureEnabled) {
                eventAdmin.setListenerEnabledFlag(PICTURE_LISTENER, true);
            }
            if (listenerBlobEnabled) {
                eventAdmin.setListenerEnabledFlag(BLOB_LISTENER, true);
            }
            log.debug("Unblock some default synchronous listener");
        }
    }

}
