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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.bulk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.S3SetBlobLengthAction.ACTION_NAME;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;

import java.time.Duration;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.blob.s3.S3BlobProviderFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.computation.BulkScrollerComputation;
import org.nuxeo.ecm.core.bulk.message.BulkCommand.Builder;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, S3BlobProviderFeature.class })
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-bulk-contrib.xml")
@RepositoryConfig(init = BlobDocumentSetRepositoryInit.class)
public class TestSetBlobLengthAction {

    private static final Logger log = LogManager.getLogger(BulkScrollerComputation.class);

    @Inject
    public BulkService service;

    @Inject
    public CoreSession session;

    @Inject
    public TransactionalFeature txFeature;

    @Test
    public void testSetBlobLength() throws Exception {
        // Note that content length is updated only on S3BinaryManager
        String nxql = "SELECT * from File";
        dumpDocs("BEFORE", nxql);
        String commandId = service.submit(new Builder(ACTION_NAME, nxql).repository(session.getRepositoryName())
                                                                        .user("Administrator")
                                                                        .param("force", true)
                                                                        .param("xpath", "content")
                                                                        .build());
        assertTrue("Bulk action didn't finish", service.await(Duration.ofSeconds(60)));
        BulkStatus status = service.getStatus(commandId);
        log.info(status);
        assertNotNull(status);
        assertEquals(COMPLETED, status.getState());
        txFeature.nextTransaction();
        dumpDocs("AFTER", nxql);
    }

    protected void dumpDocs(String title, String nxql) {
        log.info("---------- " + title);
        for (DocumentModel child : session.query(nxql)) {
            Long length = null;
            Blob blob = ((Blob) child.getPropertyValue("file:content"));
            if (blob != null) {
                length = blob.getLength();
            }
            log.info("{}: content/length: {}", child.getName(), length);
        }
    }
}
