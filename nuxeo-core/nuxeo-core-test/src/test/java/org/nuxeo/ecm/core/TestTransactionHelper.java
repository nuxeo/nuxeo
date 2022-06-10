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

package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, TransactionalFeature.class })
public class TestTransactionHelper {

    @Test
    public void testTransactionTtl() throws InterruptedException {
        TransactionHelper.commitOrRollbackTransaction();
        int ttl = TransactionHelper.getTransactionTimeToLive();
        // outside of a transaction
        assertEquals(-1, ttl);
        int timeout = 2;
        TransactionHelper.startTransaction(timeout);
        ttl = TransactionHelper.getTransactionTimeToLive();
        // alive
        assertTrue("Got " + ttl, ttl > 0);
        // timeout
        Thread.sleep((timeout + 2) * 1000);
        assertTrue(TransactionHelper.isTransactionTimedOut());
        ttl = TransactionHelper.getTransactionTimeToLive();
        assertEquals(0, ttl);
        try {
            TransactionHelper.commitOrRollbackTransaction();
        } catch (TransactionRuntimeException e) {
            // tx timeout is expected
            TransactionHelper.startTransaction();
        }
    }

   @Test
    public void testCheckTransaction() throws InterruptedException {
        TransactionHelper.commitOrRollbackTransaction();
        int timeout = 1;
        TransactionHelper.startTransaction(timeout);
        Thread.sleep((timeout + 2) * 1000);
        try {
            TransactionHelper.checkTransactionTimeout();
            fail("Expecting exception");
        } catch (TransactionRuntimeException e) {
            // tx timeout is expected
            assertTrue(e.getMessage(), e.getMessage().contains("Transaction has timed out"));
            assertTrue(e.getMessage(), e.getMessage().contains("duration 1s"));
        } finally {
            TransactionHelper.setTransactionRollbackOnly();
        }
    }
}
