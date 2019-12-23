/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.junit.Test;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalConfig;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@Features(TransactionalFeature.class)
@TransactionalConfig(autoStart = false)
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-blob-provider-s3-record.xml")
public class TestS3BlobStoreRecord extends TestS3BlobStoreAbstract {

    @Test
    public void testFlags() {
        assertTrue(bp.isTransactional());
        assertTrue(bp.isRecordMode());
        assertFalse(bs.getKeyStrategy().useDeDuplication());
    }

    @Test
    public void testCRUDInTransaction() {
        TransactionHelper.runInTransaction(() -> {
            try {
                testCRUD();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

}
