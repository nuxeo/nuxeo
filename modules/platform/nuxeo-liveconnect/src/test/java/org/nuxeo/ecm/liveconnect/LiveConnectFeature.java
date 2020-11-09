/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.liveconnect;

import java.util.UUID;

import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * @since 11.4
 */
@Features({ CoreFeature.class, DirectoryFeature.class })
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.core.mimetype")
@Deploy("org.nuxeo.ecm.platform.oauth")
@Deploy("org.nuxeo.ecm.default.config")
@Deploy("org.nuxeo.ecm.platform.query.api:OSGI-INF/pageprovider-framework.xml")
@Deploy("org.nuxeo.ecm.liveconnect")
@Deploy("org.nuxeo.ecm.liveconnect.test:OSGI-INF/test-box-config.xml")
@Deploy("org.nuxeo.ecm.liveconnect.test:OSGI-INF/test-googledrive-config.xml")
public class LiveConnectFeature implements RunnerFeature {

    // same as in test XML contrib
    public static final String SERVICE_BOX_ID = "box";

    // same as in test XML contrib
    public static final String SERVICE_CORE_ID = "core";

    // same as in test XML contrib
    public static final String SERVICE_GOOGLE_DRIVE_ID = "googledrive";

    public static final String USER_ID = "tester@example.com";

    public static BlobInfo createBlobInfo(String serviceId, String fileId) {
        return createBlobInfo(serviceId, fileId, UUID.randomUUID().toString());
    }

    public static BlobInfo createBlobInfo(String serviceId, String fileId, String digest) {
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = serviceId + ':' + USER_ID + ':' + fileId;
        blobInfo.digest = digest;
        return blobInfo;
    }

    public static BlobInfo createBlobInfo(String serviceId, String fileId, String digest, String revisionId) {
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = serviceId + ':' + USER_ID + ':' + fileId + ':' + revisionId;
        blobInfo.digest = digest;
        return blobInfo;
    }

    public static SimpleManagedBlob createBlob(String serviceId, String fileId) {
        return new SimpleManagedBlob(createBlobInfo(serviceId, fileId));
    }

    public static SimpleManagedBlob createBlob(String serviceId, String fileId, String digest) {
        return new SimpleManagedBlob(createBlobInfo(serviceId, fileId, digest));
    }

    public static SimpleManagedBlob createBlob(String serviceId, String fileId, String digest, String revisionId) {
        return new SimpleManagedBlob(createBlobInfo(serviceId, fileId, digest, revisionId));
    }
}
