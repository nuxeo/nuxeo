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
