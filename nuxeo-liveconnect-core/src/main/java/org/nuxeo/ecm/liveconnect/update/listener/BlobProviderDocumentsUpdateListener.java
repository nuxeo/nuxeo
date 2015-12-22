/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.liveconnect.update.BatchUpdateBlobProvider;
import org.nuxeo.runtime.api.Framework;

public class BlobProviderDocumentsUpdateListener implements PostCommitEventListener {

    public static final String BLOB_PROVIDER_DOCUMENT_UPDATE_EVENT = "blobProviderDocumentUpdateEvent";

    @Override
    public void handleEvent(EventBundle events) {

        for (Event each : events) {
            onEvent(each);
        }

    }

    protected void onEvent(Event event) {
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
