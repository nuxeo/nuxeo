/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
        deployContrib("org.nuxeo.ecm.core", "OSGI-INF/uidgenerator-seqgen-test-contrib.xml");
        service = Framework.getService(UIDGeneratorService.class);
    }

    @Test
    public void testSequencer() {

        UIDSequencer seq = service.getSequencer("dummySequencer");

        // Test UIDSequencer#getNext
        assertEquals(1, seq.getNext("mySequence"));
        assertEquals(2, seq.getNext("mySequence"));
        assertEquals(1, seq.getNext("mySequence2"));

        // Test UIDSequencer#initSequence
        seq.initSequence("mySequence", 1);
        assertTrue(seq.getNext("mySequence") > 1);
        seq.initSequence("mySequence", 10);
        assertTrue(seq.getNext("mySequence") > 10);
    }

}
