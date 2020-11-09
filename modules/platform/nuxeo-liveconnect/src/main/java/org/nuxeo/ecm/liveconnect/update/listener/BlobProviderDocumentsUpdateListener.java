/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */
package org.nuxeo.ecm.liveconnect.update.listener;

import java.util.Map;

import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.liveconnect.update.BatchUpdateBlobProvider;
import org.nuxeo.runtime.api.Framework;

public class BlobProviderDocumentsUpdateListener implements EventListener {

    public static final String BLOB_PROVIDER_DOCUMENT_UPDATE_EVENT = "blobProviderDocumentUpdateEvent";

    @Override
    public void handleEvent(Event event) {
        Map<String, BlobProvider> blobProviders = Framework.getService(BlobManager.class).getBlobProviders();
        if (BLOB_PROVIDER_DOCUMENT_UPDATE_EVENT.equals(event.getName())) {
            // Trigger update for all providers
            for (BlobProvider blobProvider : blobProviders.values()) {
                if (blobProvider instanceof BatchUpdateBlobProvider) {
                    ((BatchUpdateBlobProvider) blobProvider).processDocumentsUpdate();
                }
            }
        } else {
            // Trigger update for a given provider (we assume the event name is the name of the provider)
            ((BatchUpdateBlobProvider) blobProviders.get(event.getName())).processDocumentsUpdate();
        }
    }

}
