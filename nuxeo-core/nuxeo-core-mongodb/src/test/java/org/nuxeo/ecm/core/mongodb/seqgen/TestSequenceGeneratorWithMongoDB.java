/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiry
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.mongodb.seqgen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.runtime.mongodb.MongoDBFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(MongoDBFeature.class)
@Deploy("org.nuxeo.ecm.core:OSGI-INF/uidgenerator-service.xml")
@Deploy("org.nuxeo.ecm.core.mongodb.test:OSGI-INF/mongodb-seqgen-test-contrib.xml")
public class TestSequenceGeneratorWithMongoDB {

    @Inject
    protected UIDGeneratorService uidGeneratorService;

    @Test
    public void testIncrement() throws Exception {
        UIDSequencer seq = uidGeneratorService.getSequencer();
        assertNotNull(seq);
        assertTrue(seq.getClass().isAssignableFrom(MongoDBUIDSequencer.class));

        assertEquals(1, seq.getNext("myseq"));
        assertEquals(2, seq.getNext("myseq"));
        assertEquals(3L, seq.getNextLong("myseq"));
        assertEquals(1, seq.getNext("myseq2"));
        assertEquals(4, seq.getNext("myseq"));
        assertEquals(2, seq.getNext("myseq2"));
    }

    @Test
    public void testInitSequence() {
        UIDSequencer seq = uidGeneratorService.getSequencer();

        seq.getNext("autoSequence");
        seq.getNext("autoSequence");
        assertTrue(seq.getNext("autoSequence") > 2);

        seq.initSequence("mySequence", 1);
        assertTrue(seq.getNext("mySequence") > 1);
        assertTrue(seq.getNextLong("mySequence") < 10L);
        seq.initSequence("mySequence", 10L);
        assertTrue("Sequence should skip ahead to 10", seq.getNextLong("mySequence") > 10L);
        assertTrue(seq.getNextLong("mySequence") > 10L);

        try {
            seq.initSequence("mySequence", 5L);
            fail(); // should never get here.
        } catch (NuxeoException ne) {
            assertEquals("Failed to update the sequence 'mySequence' with value 5", ne.getMessage());
        }

        seq.initSequence("another", 500L);

        try {
            seq.initSequence("another", 500L);
            fail(); // should never get here.
        } catch (NuxeoException ne) {
            assertEquals("Failed to update the sequence 'another' with value 500", ne.getMessage());
        }

        assertTrue("Sequence should be greater than 500",seq.getNextLong("another") > 500L);

        try {
            seq.initSequence("another", 499L);
            fail(); // should never get here.
        } catch (NuxeoException ne) {
            assertEquals("Failed to update the sequence 'another' with value 499", ne.getMessage());
        }

        seq.initSequence("another", 9999L);
        assertTrue("Sequence should be at 10000",seq.getNextLong("another") >= 10000L);

        seq.initSequence("another", 3_147_483_647L);
        assertTrue("Sequence should be a long",seq.getNextLong("another") > 3_147_483_647L);
    }

    @SuppressWarnings("boxing")
    @Test
    public void testConcurrency() throws Exception {
        String seqName = "mt";
        int nbCalls = 10000;

        UIDSequencer seq = uidGeneratorService.getSequencer();

        ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 5, 500L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(nbCalls + 1));

        for (int i = 0; i < nbCalls; i++) {
            tpe.submit(() -> seq.getNext(seqName));
        }

        tpe.shutdown();
        boolean finish = tpe.awaitTermination(20, TimeUnit.SECONDS);
        assertTrue("timeout", finish);

        assertEquals(nbCalls + 1, seq.getNext(seqName));
    }

    @Test
    public void testBlockOfSequences() {
        UIDSequencer seq = uidGeneratorService.getSequencer();
        String key = "blockKey";
        int size = 100;
        seq.initSequence(key, 0L);
        List<Long> block = seq.getNextBlock(key, size);
        assertNotNull(block);
        assertEquals(size, block.size());
        assertTrue(block.get(0) < block.get(1));
        assertTrue(block.get(size - 2) < block.get(size - 1));
        assertTrue(block.get(size - 1) < seq.getNextLong(key));

        block = seq.getNextBlock(key, size);
        assertEquals(size, block.size());
        assertTrue(block.get(0) > size);
    }

}
