/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Gabriel Barata <gbarata@nuxeo.com>
 */
package org.nuxeo.ecm.automation.jaxrs.io.operations;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;

/**
 * Resolves a blob passed by key as input of an operation.
 *
 * @since 8.4
 */
public class BlobInputResolver implements InputResolver<Blob> {

    @Override
    public String getType() {
        return "blob";
    }

    @Override
    public Blob getInput(String content) {
        return blobFromKey(content);
    }

    public static Blob blobFromKey(String key) {
        try {
            int colon = key.indexOf(':');
            if (colon < 0) {
                throw new NuxeoException("Invalid blob key format: " + key);
            }
            String providerId = key.substring(0, colon);
            BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(providerId);
            BlobInfo blobInfo = new BlobInfo();
            blobInfo.key = key;
            if (blobProvider.performsExternalAccessControl(blobInfo)) {
                return blobProvider.readBlob(blobInfo);
            } else {
                throw new NuxeoException("BlobProvider " + providerId + " cannot perform access control for key: " +
                    key);
            }
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }
}
