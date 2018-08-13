/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.elasticsearch.seqgen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy("org.nuxeo.ecm.platform.uidgen.core")
@Deploy("org.nuxeo.elasticsearch.seqgen")
@Deploy("org.nuxeo.elasticsearch.core")
@Deploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.seqgen:elasticsearch-seqgen-index-test-contrib.xml")
public class TestSequenceGeneratorWithElasticSearch {

    @Inject
    protected UIDGeneratorService uidGeneratorService;

    @Test
    public void testIncrement() throws Exception {
        UIDSequencer seq = uidGeneratorService.getSequencer();
        assertNotNull(seq);
        assertTrue(seq.getClass().isAssignableFrom(ESUIDSequencer.class));

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
        seq.getNext("mySequence");
        seq.getNext("mySequence");

        assertTrue(seq.getNext("mySequence") > 1);
        // initSequence will work only for greater value
        seq.initSequence("mySequence", 1_000_000L);
        assertEquals(1_000_001L, seq.getNextLong("mySequence"));
        assertEquals(1_000_002L, seq.getNextLong("mySequence"));
        seq.initSequence("another", 3_147_483_647L);
        assertTrue("Sequence should be a long",seq.getNextLong("another") > 3_147_483_647L);

    }

    @Test
    @Ignore("NXP-20582: timeout waiting termination")
    public void testConcurrency() throws Exception {
        final String seqName = "mt";
        int nbCalls = 5000;

        final UIDSequencer seq = uidGeneratorService.getSequencer();
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 5, 500L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(nbCalls + 1));

        for (int i = 0; i < nbCalls; i++) {
            tpe.submit(new Runnable() {
                @Override
                public void run() {
                    seq.getNext(seqName);
                }
            });
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
        int size = 1000;
        seq.initSequence(key, 0L);
        List<Long> block = seq.getNextBlock(key, size);
        assertNotNull(block);
        assertEquals(size, block.size());
        assertTrue(block.get(0) < block.get(1));
        assertTrue(block.get(size - 2) < block.get(size - 1));
        assertTrue(block.get(size - 1) < seq.getNextLong(key));
    }
}
