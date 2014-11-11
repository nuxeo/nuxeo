/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.operation;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;

public class TestModificationSet {

    ModificationSet set;

    @Before
    public void setUp() throws Exception {
        set = new ModificationSet();
    }

    protected Modification getModification(DocumentRef ref) {
        for (Modification m : set) {
            if (m.ref.equals(ref)) {
                return m;
            }
        }
        return null;
    }

    @Test
    public void test() {
        set.add(new IdRef("a"), Modification.CREATE);
        set.add(new IdRef("b"), Modification.REMOVE);
        set.add(new IdRef("c"), Modification.CONTENT);
        set.add(new IdRef("e"), Modification.SECURITY);
        set.add(new IdRef("d"), Modification.STATE);

        assertTrue(getModification(new IdRef("a")).isCreate());
        assertTrue(getModification(new IdRef("b")).isRemove());
        assertTrue(getModification(new IdRef("c")).isUpdateModification());
        assertTrue(getModification(new IdRef("e")).isSecurityUpdate());
        assertTrue(getModification(new IdRef("d")).isStateUpdate());

        assertFalse(getModification(new IdRef("a")).isRemove());
        assertFalse(getModification(new IdRef("b")).isCreate());

        Iterator<Modification> it = set.iterator();
        assertEquals("a [8]", it.next().toString());
        assertEquals("b [16]", it.next().toString());
        assertEquals("c [64]", it.next().toString());
        assertEquals("e [128]", it.next().toString());
        assertEquals("d [256]", it.next().toString());
        assertFalse(it.hasNext());

        set.add(new IdRef("c"), Modification.STATE | Modification.SECURITY);
        Iterator<Modification> it2 = set.iterator();
        assertEquals("a [8]", it2.next().toString());
        assertEquals("b [16]", it2.next().toString());
        assertEquals("c [64]", it2.next().toString());
        assertEquals("e [128]", it2.next().toString());
        assertEquals("d [256]", it2.next().toString());
        assertEquals("c [384]", it2.next().toString());
        assertFalse(it2.hasNext());

        set.remove(2);
        Iterator<Modification> it3 = set.iterator();
        assertEquals("a [8]", it3.next().toString());
        assertEquals("b [16]", it3.next().toString());
        assertEquals("e [128]", it3.next().toString());
        assertEquals("d [256]", it3.next().toString());
        assertEquals("c [384]", it3.next().toString());
        assertFalse(it3.hasNext());
    }

}
