/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id: TestNuxeoPrincipalImpl.java 28443 2008-01-02 18:16:28Z sfermigier $
 */

package org.nuxeo.ecm.platform.usermanager;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Florent Guillaume
 */
public class TestNuxeoPrincipalImpl extends NXRuntimeTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.directory.types.contrib");
    }

    @Test
    public void testEquals() {
        NuxeoPrincipalImpl a = new NuxeoPrincipalImpl("foo");
        NuxeoPrincipalImpl b = new NuxeoPrincipalImpl("foo");
        NuxeoPrincipalImpl c = new NuxeoPrincipalImpl("bar");
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertFalse(a.equals(c));
        assertFalse(c.equals(a));
        assertFalse(a.equals(null));
        assertFalse(c.equals(null));
    }

    @Test
    public void testHasCode() throws Exception {
        NuxeoPrincipalImpl a = new NuxeoPrincipalImpl("foo");
        NuxeoPrincipalImpl b = new NuxeoPrincipalImpl("foo");
        assertEquals(a.hashCode(), b.hashCode());
        // we don't assert that hash codes are different for principals with
        // different names, as that doesn't have to be true
    }

}
