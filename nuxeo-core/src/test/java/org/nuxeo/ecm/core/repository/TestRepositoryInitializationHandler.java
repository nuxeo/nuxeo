/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
