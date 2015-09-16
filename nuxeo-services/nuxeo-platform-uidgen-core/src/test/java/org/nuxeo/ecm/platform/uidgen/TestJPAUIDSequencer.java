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
 *     Antoine Taillefer
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.uidgen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.core.persistence", //
        "org.nuxeo.ecm.platform.uidgen.core", //
})
@LocalDeploy("org.nuxeo.ecm.platform.uidgen.core.tests:OSGI-INF/uidgenerator-test-contrib.xml")
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
        seq.initSequence("mySequence", 10);
        assertTrue(seq.getNext("mySequence") > 10);
    }

}
