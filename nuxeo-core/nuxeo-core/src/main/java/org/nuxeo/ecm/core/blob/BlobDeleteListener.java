/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.runtime.api.Framework;

/**
 * Called periodically by a scheduler to delete the blobs marked for deletion, if enough time has elapsed.
 *
 * @since 2021.9
 */
public class BlobDeleteListener implements PostCommitEventListener {

    @Override
    public void handleEvent(EventBundle events) {
        BlobManager blobManager = Framework.getService(BlobManager.class);
        if (blobManager != null) {
            blobManager.deleteBlobsMarkedForDeletion();
        }
    }

}
