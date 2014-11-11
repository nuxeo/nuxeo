/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: TestScopedMap.java 19046 2007-05-21 13:03:50Z sfermigier $
 */

package org.nuxeo.common.collections;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
public class TestScopedMap extends TestCase {

    public void testGetScopedValueWithScope() {
        ScopedMap map = new ScopedMap();
        map.put("default/foo", "bar");
        assertEquals("bar", map.getScopedValue(ScopeType.DEFAULT, "foo"));
    }

    public void testGetScopedValue() {
        ScopedMap map = new ScopedMap();
        map.put("default/foo", "bar");
        assertEquals("bar", map.getScopedValue("foo"));
    }

    public void testPutScopedValueWithScope() {
        ScopedMap map = new ScopedMap();
        map.putScopedValue(ScopeType.REQUEST, "foo", "bar");
        assertEquals("bar", map.get("request/foo"));
    }

    public void testPutScopedValue() {
        ScopedMap map = new ScopedMap();
        map.putScopedValue("foo", "bar");
        assertEquals("bar", map.get("default/foo"));
    }

    public void testClearScope() {
        ScopedMap map = new ScopedMap();
        map.putScopedValue(ScopeType.REQUEST, "foo1", "bar1");
        assertEquals("bar1", map.get("request/foo1"));
        map.putScopedValue(ScopeType.REQUEST, "foo2", "bar2");
        assertEquals("bar2", map.get("request/foo2"));
        map.putScopedValue(ScopeType.DEFAULT, "foo3", "bar3");
        assertEquals("bar3", map.get("default/foo3"));
        assertEquals(3, map.size());

        map.clearScope(ScopeType.REQUEST);

        assertEquals(1, map.size());
        assertEquals("bar3", map.get("default/foo3"));
    }

}
