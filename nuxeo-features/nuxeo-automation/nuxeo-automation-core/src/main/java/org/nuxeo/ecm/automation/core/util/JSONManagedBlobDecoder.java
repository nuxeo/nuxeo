/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.automation.core.util;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Blob decoder resolving a blob from a given blob provider and a blob key.
 * <p>
 * Format is:
 *
 * <pre>
 * {
 *     "providerId": "PROVIDER_ID", <-- the blob provider id
 *     "key": "BLOB_KEY" <-- the blob key
 * }
 * </pre>
 *
 * @since 10.2
 */
public class JSONManagedBlobDecoder implements JSONBlobDecoder {

    @Override
    public Blob getBlobFromJSON(ObjectNode jsonObject) {

        if (!(jsonObject.has("providerId") && jsonObject.has("key"))) {
            return null;
        }

        String providerId = jsonObject.get("providerId").textValue();
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(providerId);
        if (blobProvider == null) {
            return null;
        }

        if (!blobProvider.hasCreateFromKeyPermission()) {
            throw new NuxeoException("The current user does not have the rights to fetch the blob",
                    HttpServletResponse.SC_UNAUTHORIZED);
        }

        try {
            BlobInfo blobInfo = new BlobInfo();
            blobInfo.key = providerId + ":" + jsonObject.get("key").textValue();
            return blobProvider.readBlob(blobInfo);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

}
