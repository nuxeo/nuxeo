/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: TestListDiff.java 20563 2007-06-15 15:52:27Z ogrisel $
 */

package org.nuxeo.ecm.core.api;

import org.junit.Test;
import static org.junit.Assert.*;

// Nothing much to test actually.
public class TestListDiff {

    @Test
    public void test() {
        ListDiff list = new ListDiff();

        assertFalse(list.isDirty());
        assertEquals("ListDiff { [] }", list.toString());

        list.add(0);
        assertTrue(list.isDirty());
        assertEquals("ListDiff { [Entry {0, ADD, 0}] }", list.toString());

        list.remove(5);
        assertTrue(list.isDirty());
        assertEquals("ListDiff { [Entry {0, ADD, 0}, Entry {5, REMOVE, null}] }", list.toString());

        list.reset();
        assertFalse(list.isDirty());
        assertEquals("ListDiff { [] }", list.toString());
    }
}
