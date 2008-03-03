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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.common.utils;

import junit.framework.TestCase;


public class TestRegistry extends TestCase {

    private Object object;

    private Registry<Object> registry;

    @Override
    public void setUp() {
        object = new Object();
        registry = new Registry("My Registry");
    }

    @Override
    public void tearDown() {
        registry.clear();
    }

    public void testRegistry() {
        assertEquals("My Registry", registry.getName());
    }

    public void testRegisterUnregister() {
        assertEquals(0, registry.size());

        // register
        registry.register("foo", object);

        assertEquals(1, registry.size());

        assertEquals(object, registry.getObjectByName("foo"));
        assertTrue(registry.isRegistered(object));
        assertTrue(registry.isRegistered("foo"));

        registry.unregister("foo");

        assertEquals(0, registry.size());
    }

    public void testRegistrationBehaviors() {
        assertEquals(0, registry.size());

        // register
        registry.register("foo", object);

        assertEquals(1, registry.size());

        assertTrue(registry.isRegistered(object));
        assertTrue(registry.isRegistered("foo"));

        // register again
        registry.register("foo", object);

        assertEquals(1, registry.size());
    }

}
