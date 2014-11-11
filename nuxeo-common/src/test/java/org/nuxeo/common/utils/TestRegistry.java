/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.common.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


public class TestRegistry {

    private Object object;

    private Registry<Object> registry;

    @Before
    public void setUp() {
        object = new Object();
        registry = new Registry<Object>("My Registry");
    }

    @After
    public void tearDown() {
        registry.clear();
    }

    @Test
    public void testRegistry() {
        assertEquals("My Registry", registry.getName());
    }

    @Test
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

    @Test
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
