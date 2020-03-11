/*
 * (C) Copyright 2014-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.redis.contribs.RedisUIDSequencer;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, RedisFeature.class })
@Deploy("org.nuxeo.ecm.core.redis.tests:test-uidsequencer-contrib.xml")
public class TestRedisUIDSequencer {

    @Inject
    protected UIDGeneratorService service;

    @Test
    public void testRedisUIDSequencer() throws Exception {
        UIDSequencer sequencer = service.getSequencer("redisSequencer");
        assertNotNull(sequencer);
        assertTrue(sequencer instanceof RedisUIDSequencer);
        sequencer.init(); // not correctly done in tests TODO fix this

        assertEquals(1L, sequencer.getNextLong("A"));
        assertEquals(2L, sequencer.getNextLong("A"));
        assertEquals(1, sequencer.getNext("B"));
        assertEquals(3L, sequencer.getNextLong("A"));
        assertEquals(2, sequencer.getNext("B"));

        sequencer.initSequence("A", 100000L);
        assertEquals(100001L, sequencer.getNextLong("A"));
    }

    @Test
    public void testBlockOfSequences() {
        UIDSequencer seq = service.getSequencer();
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
