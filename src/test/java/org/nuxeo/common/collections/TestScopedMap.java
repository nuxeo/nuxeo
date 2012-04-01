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
 * $Id: TestScopedMap.java 19046 2007-05-21 13:03:50Z sfermigier $
 */

package org.nuxeo.common.collections;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class TestScopedMap {

    @Test
    public void testGetScopedValueWithScope() {
        ScopedMap map = new ScopedMap();
        map.put("default/foo", "bar");
        assertEquals("bar", map.getScopedValue(ScopeType.DEFAULT, "foo"));
    }

    @Test
    public void testGetScopedValue() {
        ScopedMap map = new ScopedMap();
        map.put("default/foo", "bar");
        assertEquals("bar", map.getScopedValue("foo"));
    }

    @Test
    public void testPutScopedValueWithScope() {
        ScopedMap map = new ScopedMap();
        map.putScopedValue(ScopeType.REQUEST, "foo", "bar");
        assertEquals("bar", map.get("request/foo"));
    }

    @Test
    public void testPutScopedValue() {
        ScopedMap map = new ScopedMap();
        map.putScopedValue("foo", "bar");
        assertEquals("bar", map.get("default/foo"));
    }

    @Test
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
