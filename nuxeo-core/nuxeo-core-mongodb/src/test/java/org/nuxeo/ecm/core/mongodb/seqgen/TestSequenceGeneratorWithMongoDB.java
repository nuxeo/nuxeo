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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.mongodb.seqgen.MongoDBUIDSequencer;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.runtime.mongodb.MongoDBFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, MongoDBFeature.class })
@LocalDeploy({ "org.nuxeo.ecm.core.mongodb.test:OSGI-INF/mongodb-seqgen-test-contrib.xml" })
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

        seq.getNext("mySequence");
        seq.getNext("mySequence");

        seq.initSequence("mySequence", 1);
        assertTrue(seq.getNext("mySequence") > 1);
        seq.initSequence("mySequence", 10);
        assertTrue(seq.getNext("mySequence") > 10);
        assertTrue(seq.getNextLong("mySequence") > 10);
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

}
