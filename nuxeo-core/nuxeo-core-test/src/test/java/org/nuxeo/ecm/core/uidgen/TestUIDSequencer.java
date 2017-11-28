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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.core.uidgen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @since 7.4
 */
public class TestUIDSequencer extends NXRuntimeTestCase {

    protected UIDGeneratorService service;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.core", "OSGI-INF/test-uidgenerator-contrib.xml");
        service = Framework.getService(UIDGeneratorService.class);
    }

    @Test
    public void testSequencer() {

        UIDSequencer seq = service.getSequencer("dummySequencer");

        // Test UIDSequencer#getNext
        assertEquals(1, seq.getNext("mySequence"));
        assertEquals(2L, seq.getNextLong("mySequence"));
        assertEquals(1, seq.getNext("mySequence2"));

        // Test UIDSequencer#initSequence
        seq.initSequence("mySequence", 1);
        assertTrue(seq.getNext("mySequence") > 1);
        seq.initSequence("mySequence", 10L);
        assertTrue(seq.getNext("mySequence") > 10);
        assertTrue(seq.getNextLong("mySequence") > 10);
    }

}
