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
 *     Antoine Taillefer
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.uidgen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.core.persistence")
@Deploy("org.nuxeo.ecm.platform.uidgen.core")
@Deploy("org.nuxeo.ecm.platform.uidgen.core.tests:OSGI-INF/uidgenerator-test-contrib.xml")
public class TestJPAUIDSequencer {

    @Inject
    protected UIDGeneratorService service;

    @Test
    public void testSequencer() {

        UIDSequencer seq = service.getSequencer("hibernateSequencer");

        // Test UIDSequencer#getNext
        assertEquals(1, seq.getNext("mySequence"));
        assertEquals(2, seq.getNext("mySequence"));
        assertEquals(1, seq.getNext("mySequence2"));

        // Test UIDSequencer#initSequence
        seq.initSequence("mySequence", 1);
        assertTrue(seq.getNext("mySequence") > 1);
        seq.initSequence("mySequence", 10L);
        assertTrue(seq.getNext("mySequence") > 10);
    }

}
