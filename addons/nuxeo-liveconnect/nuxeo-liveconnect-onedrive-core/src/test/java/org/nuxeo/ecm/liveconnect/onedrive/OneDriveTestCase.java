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
package org.nuxeo.ecm.liveconnect.onedrive;

import java.util.UUID;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@Ignore
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.core.cache", "org.nuxeo.ecm.core.mimetype", "org.nuxeo.ecm.platform.oauth",
        "org.nuxeo.ecm.directory", "org.nuxeo.ecm.directory.sql", "org.nuxeo.ecm.default.config" })
@LocalDeploy({ "org.nuxeo.ecm.platform.query.api:OSGI-INF/pageprovider-framework.xml",
        "org.nuxeo.ecm.liveconnect.onedrive:OSGI-INF/cache-config.xml",
        "org.nuxeo.ecm.liveconnect:OSGI-INF/liveconnect-workmanager-contrib.xml",
        "org.nuxeo.ecm.liveconnect.onedrive:OSGI-INF/onedrive-pageprovider-contrib.xml" })
public class OneDriveTestCase {

    // same as in test XML contrib
    protected static final String SERVICE_ID = "onedrive";

    protected static final String USERID = "tester@example.com";

    protected static final String FILE_1_ID = "5000948880";

    protected SimpleManagedBlob createBlob() {
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = SERVICE_ID + ':' + USERID + ':' + FILE_1_ID;
        blobInfo.digest = UUID.randomUUID().toString();
        return new SimpleManagedBlob(blobInfo);
    }

    protected SimpleManagedBlob createBlob(String revision) {
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = SERVICE_ID + ':' + USERID + ':' + FILE_1_ID + ':' + revision;
        blobInfo.digest = UUID.randomUUID().toString();
        return new SimpleManagedBlob(blobInfo);
    }

}
