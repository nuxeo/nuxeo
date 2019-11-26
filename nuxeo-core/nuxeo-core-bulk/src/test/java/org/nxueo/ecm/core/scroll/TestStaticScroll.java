/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nxueo.ecm.core.scroll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.scroll.StaticScroll;
import org.nuxeo.ecm.core.scroll.StaticScrollRequest;

public class TestStaticScroll {

    protected Scroll getScroll(String ids, int scrollSize) {
        ScrollRequest request = StaticScrollRequest.builder(ids).scrollSize(scrollSize).build();
        Scroll scroll = new StaticScroll();
        scroll.init(request, Collections.emptyMap());
        return scroll;
    }

    @Test
    public void testNormal() {
        String ids = "first,2,3,4,5,6,7,8,9,last";
        int scrollSize = 4;
        Scroll scroll = getScroll(ids, scrollSize);

        assertNotNull(scroll);
        assertTrue(scroll.fetch());

        assertEquals(scrollSize, scroll.getIds().size());
        assertEquals(Arrays.asList("first", "2", "3", "4"), scroll.getIds());
        // calling getIds multiple times
        assertEquals(Arrays.asList("first", "2", "3", "4"), scroll.getIds());

        assertTrue(scroll.fetch());
        assertEquals(Arrays.asList("5", "6", "7", "8"), scroll.getIds());

        assertTrue(scroll.fetch());
        assertEquals(Arrays.asList("9", "last"), scroll.getIds());

        assertFalse(scroll.fetch());
        assertEquals(Collections.emptyList(), scroll.getIds());

        assertFalse(scroll.fetch());
        assertEquals(Collections.emptyList(), scroll.getIds());
    }

    @Test
    public void testFetchMustBeCalledFirst() {
        String ids = "first,2,3,4,5,6,7,8,9,last";
        int scrollSize = 4;
        Scroll scroll = getScroll(ids, scrollSize);
        try {
            scroll.getIds();
            fail("fetch must be called first");
        } catch (IllegalStateException e) {
            // expected
        }
        assertTrue(scroll.fetch());
        scroll.getIds();
    }

    @Test
    public void testSingle() {
        String ids = "first";
        int scrollSize = 4;
        Scroll scroll = getScroll(ids, scrollSize);

        assertTrue(scroll.fetch());
        assertEquals(Arrays.asList("first"), scroll.getIds());
        assertFalse(scroll.fetch());

        scroll = getScroll(ids, 1);
        assertTrue(scroll.fetch());
        assertEquals(Arrays.asList("first"), scroll.getIds());
        assertFalse(scroll.fetch());
    }

    @Test
    public void testInvalidStaticScrollRequest() {
        try {
            getScroll(null, 4);
            fail("expecting identifier cannot be null");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            getScroll("foo,bar", 0);
            fail("expecting scroll size > 0");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testBlankIdentifier() {
        Scroll scroll = getScroll("", 4);
        assertFalse(scroll.toString(), scroll.fetch());
        assertEquals(Collections.emptyList(), scroll.getIds());

        scroll = getScroll(",,", 4);
        assertFalse(scroll.toString(), scroll.fetch());
        assertEquals(Collections.emptyList(), scroll.getIds());

        scroll = getScroll(",,  foo   ,  bar   ", 1);
        assertTrue(scroll.fetch());
        assertEquals(Arrays.asList("foo"), scroll.getIds());

        assertTrue(scroll.fetch());
        assertEquals(Arrays.asList("bar"), scroll.getIds());

        assertFalse(scroll.fetch());
    }
}
