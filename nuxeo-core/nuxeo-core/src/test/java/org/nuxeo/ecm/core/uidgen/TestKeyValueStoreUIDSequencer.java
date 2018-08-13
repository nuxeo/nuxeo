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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.uidgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * This test uses the default (in-memory) key/value store.
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.runtime.kv")
@Deploy("org.nuxeo.ecm.core:OSGI-INF/uidgenerator-service.xml")
@Deploy("org.nuxeo.ecm.core:OSGI-INF/uidgenerator-keyvalue-config.xml")
@Deploy("org.nuxeo.ecm.core.tests:OSGI-INF/test-keyvaluestore-uidseq.xml")
public class TestKeyValueStoreUIDSequencer {

    @Inject
    protected UIDGeneratorService service;

    @Test
    public void testRegistration() {
        UIDSequencer seq = service.getSequencer();
        assertNotNull(seq);
        assertTrue(seq.getClass().getName(), seq instanceof KeyValueStoreUIDSequencer);
    }

    @Test
    public void testSequencer() {
        UIDSequencer seq = service.getSequencer();
        String key;

        key = "foo";
        seq.initSequence(key, 0L);
        seq.getNextLong(key);
        seq.getNextLong(key);
        assertThat(seq.getNextLong(key)).isGreaterThan(2);

        key = "bar";
        seq.initSequence(key, 1L);
        assertThat(seq.getNextLong(key)).isGreaterThan(1);
        assertThat(seq.getNextLong(key)).isLessThan(10);
        seq.initSequence(key, 10L);
        assertThat(seq.getNextLong(key)).isGreaterThan(10);
        assertThat(seq.getNextLong(key)).isGreaterThan(10);
        // we can go backward when initializing a sequence
        seq.initSequence(key, 5L);
        assertEquals(6, seq.getNextLong(key));

        key = "baz";
        seq.initSequence(key, 499L);
        assertEquals(500, seq.getNextLong(key));
        seq.initSequence(key, 9999L);
        assertEquals(10000, seq.getNextLong(key));
        seq.initSequence(key, (long) Integer.MAX_VALUE);
        assertEquals(2_147_483_648L, seq.getNextLong(key)); // first long not representable as int
    }

    @Test
    public void testInterleave() {
        UIDSequencer seq = service.getSequencer();
        assertNotNull(seq);
        assertTrue(seq.getClass().getName(), seq instanceof KeyValueStoreUIDSequencer);

        String key1 = "foo";
        String key2 = "bar";
        seq.initSequence(key1, 0L);
        seq.initSequence(key2, 0L);
        assertEquals(1, seq.getNextLong(key1));
        assertEquals(2, seq.getNextLong(key1));
        assertEquals(3, seq.getNextLong(key1));
        assertEquals(1, seq.getNextLong(key2));
        assertEquals(4, seq.getNextLong(key1));
        assertEquals(2, seq.getNextLong(key2));
    }

    @Test
    public void testNoCollisionsBetweenSequencers() {
        UIDSequencer seq1 = service.getSequencer("uidgen");
        UIDSequencer seq2 = service.getSequencer("myseq");

        String key = "foo";
        seq1.initSequence(key, 100L);
        seq2.initSequence(key, 555L);
        assertEquals(101, seq1.getNextLong(key));
        assertEquals(556, seq2.getNextLong(key));
    }

    @SuppressWarnings("boxing")
    @Test
    public void testConcurrency() throws Exception {
        UIDSequencer seq = service.getSequencer();
        int n = 10000;
        int poolSize = 5;

        String key = "mt";
        seq.initSequence(key, 0L);
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(poolSize, poolSize, 500L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(n + 1));
        for (int i = 0; i < n; i++) {
            tpe.submit(() -> seq.getNextLong(key));
        }
        tpe.shutdown();
        boolean finish = tpe.awaitTermination(20, TimeUnit.SECONDS);
        assertTrue("timeout", finish);
        assertEquals(n + 1, seq.getNextLong(key));
    }

    @Test
    public void testBlockOfSequences() {
        UIDSequencer seq = service.getSequencer();
        String key = "block";
        int size = 10;
        seq.initSequence(key, 0L);
        List<Long> block = seq.getNextBlock(key, size);
        assertNotNull(block);
        assertEquals(size, block.size());
        assertTrue(block.get(0) < block.get(1));
        assertTrue(block.get(size - 2) < block.get(size - 1));
        assertTrue(block.get(size - 1) < seq.getNextLong(key));
    }
}
