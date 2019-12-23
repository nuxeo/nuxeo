/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.ecm.core.storage.status;

import java.util.Map;

import org.nuxeo.ecm.blob.s3.S3BlobProvider;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryManager;
import org.nuxeo.ecm.core.storage.sql.S3BinaryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.api.Probe;
import org.nuxeo.runtime.management.api.ProbeStatus;

/**
 * Probe to check the status of the S3BinaryManager. Returns success if Nuxeo can access the bucket, failure otherwise
 *
 * @since 9.3
 */
public class S3BinaryManagerStatusProbe implements Probe {

    @Override
    public ProbeStatus run() {
        Map<String, BlobProvider> providers = Framework.getService(BlobManager.class).getBlobProviders();
        boolean found = false;
        for (Map.Entry<String, BlobProvider> en : providers.entrySet()) {
            String id = en.getKey();
            BlobProvider blobProvider = en.getValue();
            BinaryManager bm = blobProvider.getBinaryManager();
            if (bm instanceof S3BinaryManager) {
                if (!((S3BinaryManager) bm).canAccessBucket()) {
                    return ProbeStatus.newFailure("S3BinaryManager cannot access the configured bucket: " + id);
                }
                found = true;
            } else if (bm instanceof S3BlobProvider) {
                if (!((S3BlobProvider) bm).canAccessBucket()) {
                    return ProbeStatus.newFailure("S3BinaryManager cannot access the configured bucket: " + id);
                }
                found = true;
            }
        }
        if (found) {
            return ProbeStatus.newSuccess("S3BinaryManager can access the configured buckets");
        } else {
            return ProbeStatus.newSuccess("No S3BinaryManager bucket configured");
        }
    }

}
