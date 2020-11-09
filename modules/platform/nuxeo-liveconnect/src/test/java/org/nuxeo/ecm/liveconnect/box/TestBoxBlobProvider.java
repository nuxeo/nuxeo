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
package org.nuxeo.ecm.liveconnect.box;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.spy;
import static org.nuxeo.ecm.liveconnect.LiveConnectFeature.SERVICE_BOX_ID;
import static org.nuxeo.ecm.liveconnect.LiveConnectFeature.createBlob;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.liveconnect.LiveConnectFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(LiveConnectFeature.class)
public class TestBoxBlobProvider {

    protected static final String FILE_1_ID = "5000948880";

    protected static final int FILE_1_SIZE = 629644;

    protected static final String FILE_1_NAME = "tigers.jpeg";

    @Inject
    private BlobManager blobManager;

    private BoxBlobProvider blobProvider;

    @Before
    public void before() {
        blobProvider = spy((BoxBlobProvider) blobManager.getBlobProvider(SERVICE_BOX_ID));
        assertNotNull(blobProvider);
    }

    @Test
    public void testFreezeVersionWithBlobWithRevision() throws Exception {
        SimpleManagedBlob blob = createBlob(SERVICE_BOX_ID, FILE_1_ID, "digest", "revision");

        ManagedBlob newBlob = blobProvider.freezeVersion(blob, null);
        assertNull(newBlob);
    }

    @Test
    public void testFreezeVersion() throws Exception {
        SimpleManagedBlob blob = createBlob(SERVICE_BOX_ID, FILE_1_ID);

        ManagedBlob newBlob = blobProvider.freezeVersion(blob, null);
        assertNotNull(newBlob);
        assertEquals(blob.getKey() + ":26261748416", newBlob.getKey());
    }

}
