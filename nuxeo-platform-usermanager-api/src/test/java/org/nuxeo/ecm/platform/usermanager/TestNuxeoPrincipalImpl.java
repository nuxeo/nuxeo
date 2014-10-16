/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Florent Guillaume
 *
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

    @SuppressWarnings({"SimplifiableJUnitAssertion", "ObjectEqualsNull"})
    @Test
    public void testEquals() throws ClientException {
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
