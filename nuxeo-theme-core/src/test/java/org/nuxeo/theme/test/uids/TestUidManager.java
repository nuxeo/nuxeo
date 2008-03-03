/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.test.uids;

import junit.framework.TestCase;

import org.nuxeo.theme.test.IdentifiableObject;
import org.nuxeo.theme.uids.Identifiable;
import org.nuxeo.theme.uids.UidManager;

public class TestUidManager extends TestCase {

    private UidManager uidManager;

    @Override
    public void setUp() {
        uidManager = new UidManager();
    }

    @Override
    public void tearDown() {
        uidManager.clear();
    }

    public void testRegisterUnregister() {
        Identifiable ob1 = new IdentifiableObject();
        Integer id1 = uidManager.register(ob1);
        assertEquals(id1, ob1.getUid());
        assertEquals(ob1, uidManager.getObjectByUid(id1));
        uidManager.unregister(ob1);
        assertNull(ob1.getUid());
    }

    public void testClear() {
        Identifiable ob1 = new IdentifiableObject();
        Integer id1 = uidManager.register(ob1);
        assertEquals(id1, ob1.getUid());
        assertEquals(ob1, uidManager.getObjectByUid(id1));
        uidManager.clear();
        assertNull(uidManager.getObjectByUid(id1));
    }

    public void testSequence() {
        Integer[] uids1 = new Integer[10];
        Integer[] uids2 = new Integer[10];

        uidManager.clear();
        for (int i = 0; i < 10; i++) {
            Identifiable ob = new IdentifiableObject();
            uidManager.register(ob);
            uids1[i] = ob.getUid();
        }

        uidManager.clear();
        for (int i = 0; i < 10; i++) {
            Identifiable ob = new IdentifiableObject();
            uidManager.register(ob);
            uids2[i] = ob.getUid();
        }

        for (int i = 0; i < 10; i++) {
            assertEquals(uids1[i], uids2[i]);
        }

    }

}
