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
 * $Id: TestListDiff.java 20563 2007-06-15 15:52:27Z ogrisel $
 */

package org.nuxeo.ecm.core.api;

import junit.framework.TestCase;

// Nothing much to test actually.
public class TestListDiff extends TestCase {
    public void test() {
        ListDiff list = new ListDiff();

        assertFalse(list.isDirty());
        assertEquals("ListDiff { [] }", list.toString());

        list.add(0);
        assertTrue(list.isDirty());
        assertEquals("ListDiff { [Entry {0, ADD, 0}] }", list.toString());

        list.remove(5);
        assertTrue(list.isDirty());
        assertEquals(
                "ListDiff { [Entry {0, ADD, 0}, Entry {5, REMOVE, null}] }",
                list.toString());

        list.reset();
        assertFalse(list.isDirty());
        assertEquals("ListDiff { [] }", list.toString());
    }
}
