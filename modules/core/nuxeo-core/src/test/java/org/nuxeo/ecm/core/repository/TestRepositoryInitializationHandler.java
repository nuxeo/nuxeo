/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;

public class TestRepositoryInitializationHandler {

    protected static class TestHandler extends RepositoryInitializationHandler {
        @Override
        public void doInitializeRepository(CoreSession session) {
        }
    }

    @After
    public void tearDown() throws Exception {
        Field field = RepositoryInitializationHandler.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }

    protected RepositoryInitializationHandler getTail() {
        return RepositoryInitializationHandler.getInstance();
    }

    @Test
    public void testUninstallSameOrder() throws Exception {
        RepositoryInitializationHandler a = new TestHandler();
        RepositoryInitializationHandler b = new TestHandler();
        assertNull(getTail());
        a.install();
        assertEquals(a, getTail());
        b.install();
        assertEquals(b, getTail());
        assertNull(a.previous);
        assertEquals(b, a.next);
        assertEquals(a, b.previous);
        assertNull(b.next);

        // uninstall a then b
        a.uninstall();
        assertEquals(b, getTail());
        assertNull(b.previous);
        assertNull(b.next);
        b.uninstall();
        assertNull(getTail());
    }

    @Test
    public void testUninstallInverseOrder() throws Exception {
        RepositoryInitializationHandler a = new TestHandler();
        RepositoryInitializationHandler b = new TestHandler();
        assertNull(getTail());
        a.install();
        assertEquals(a, getTail());
        b.install();
        assertEquals(b, getTail());
        assertNull(a.previous);
        assertEquals(b, a.next);
        assertEquals(a, b.previous);
        assertNull(b.next);

        // uninstall b then a
        b.uninstall();
        assertEquals(a, getTail());
        assertNull(a.previous);
        assertNull(a.next);
        a.uninstall();
        assertNull(getTail());
    }

}
