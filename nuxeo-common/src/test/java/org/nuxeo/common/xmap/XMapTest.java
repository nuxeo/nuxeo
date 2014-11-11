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
 * $Id$
 */

package org.nuxeo.common.xmap;

import java.net.URL;

import junit.framework.TestCase;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class XMapTest extends TestCase {

    public void testMapping() throws Exception {
        XMap xmap = new XMap();
        xmap.register(Author.class);

        URL url = Thread.currentThread().getContextClassLoader()
            .getResource("test-xmap.xml");

        Author author = (Author) xmap.load(url);

        assertEquals("First test 22", author.title);
        assertEquals("bla bla", author.description);
        assertEquals(author, author.name.owner);
        assertEquals("my first name", author.name.firstName);
        assertEquals("my last name", author.name.lastName);
        assertEquals("The content", author.content.trim());
        assertEquals("author", author.nameType);
        assertEquals(32, author.age);
        assertEquals("test1", author.getId());
        assertEquals(3, author.items.size());
        assertEquals(3, author.itemIds.size());
        assertEquals(2, author.friends.size());
        assertEquals("Item 1", author.items.get(0));
        assertEquals("Item 2", author.items.get(1));
        assertEquals("Item 3", author.items.get(2));
        assertEquals("item1", author.itemIds.get(0));
        assertEquals("item2", author.itemIds.get(1));
        assertEquals("item3", author.itemIds.get(2));
        assertEquals("friend1_fn", author.friends.get(0).firstName);
        assertEquals("friend1_ln", author.friends.get(0).lastName);
        assertEquals("friend2_fn", author.friends.get(1).firstName);
        assertEquals("friend2_ln", author.friends.get(1).lastName);

        assertEquals("Test <b>content</b>", author.testContent.trim());
        String t = author.testContent2.getFirstChild().getTextContent().trim();
        assertEquals("Test", t);

        assertFalse(author.content.equals(author.content.trim()));

        assertNull(author.testNullByDefaultForList);
        assertNull(author.testNullByDefaultForMap);
    }

}
