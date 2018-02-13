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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.liveconnect.core;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.UUID;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogFeature;

@Ignore
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class, LogFeature.class, LogCaptureFeature.class })
@Deploy({ "org.nuxeo.ecm.core.cache",
        "org.nuxeo.ecm.core.mimetype",
        "org.nuxeo.ecm.platform.oauth",
        "org.nuxeo.ecm.default.config",
        "org.nuxeo.ecm.liveconnect.core" })
@Deploy({
        "org.nuxeo.ecm.platform.query.api:OSGI-INF/pageprovider-framework.xml",
        "org.nuxeo.ecm.liveconnect.core:OSGI-INF/liveconnect-workmanager-contrib.xml",
        "org.nuxeo.ecm.liveconnect.core:OSGI-INF/test-core-cache-config.xml",
        "org.nuxeo.ecm.liveconnect.core:OSGI-INF/test-core-config.xml",
        "org.nuxeo.ecm.liveconnect.core:OSGI-INF/test-core-pageprovider-contrib.xml"
})
public class LiveConnectTestCase {

    // same as in test XML contrib
    public static final String SERVICE_ID = "core";

    public static final String USERID = "tester@example.com";

    public static final String FILE_1_ID = "5000948880";

    public static final int FILE_1_SIZE = 629644;

    public static final String FILE_1_NAME = "tigers.jpeg";

    public static final String FILE_1_DIGEST = UUID.randomUUID().toString();

    public static final byte[] FILE_1_BYTES = "picture of a tiger".getBytes(UTF_8);

    public static final String INVALID_FILE_ID = "invalid-file-id";

    protected SimpleManagedBlob createBlob(String fileId) {
        return createBlob(fileId, UUID.randomUUID().toString());
    }

    protected SimpleManagedBlob createBlob(String fileId, String digest) {
        return new SimpleManagedBlob(createBlobInfo(fileId, digest));
    }

    protected SimpleManagedBlob createBlob(String fileId, String digest, String revisionId) {
        return new SimpleManagedBlob(createBlobInfo(fileId, digest, revisionId));
    }

    protected BlobInfo createBlobInfo(String fileId) {
        return createBlobInfo(fileId, UUID.randomUUID().toString());
    }

    protected BlobInfo createBlobInfo(String fileId, String digest) {
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = SERVICE_ID + ':' + USERID + ':' + fileId;
        blobInfo.digest = digest;
        return blobInfo;
    }

    protected BlobInfo createBlobInfo(String fileId, String digest, String revisionId) {
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = SERVICE_ID + ':' + USERID + ':' + fileId + ':' + revisionId;
        blobInfo.digest = digest;
        return blobInfo;
    }

}
