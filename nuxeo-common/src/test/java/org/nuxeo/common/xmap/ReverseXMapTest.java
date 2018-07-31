/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.common.xmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.junit.Test;
import org.nuxeo.common.xmap.Author.Gender;

public class ReverseXMapTest {

    @Test
    public void testReverse() throws Exception {
        XMap xmap = new XMap();
        xmap.register(Author.class);
        URL url = Thread.currentThread().getContextClassLoader().getResource("test-xmap.xml");
        Author author = (Author) xmap.load(url);
        try {
            xmap.toXML("");
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("java.lang.String is NOT registred in xmap"));
        }

        // save the object
        // System.out.println(xmap.toXML(author));
        File file = File.createTempFile("xmap", "xml", new File(System.getProperty("java.io.tmpdir")));
        xmap.toXML(author, file);

        // load map from new created file
        xmap = new XMap();
        xmap.register(Author.class);
        author = (Author) xmap.load(file.toURI().toURL());
        file.delete();

        assertEquals("First test 22", author.title);
        assertEquals("bla bla", author.description);
        assertEquals(author, author.name.owner);
        assertEquals("my first name", author.name.firstName);
        assertEquals("my last name", author.name.lastName);
        assertEquals("The content", author.content.trim());
        assertEquals("author", author.nameType);
        assertEquals(Gender.MALE, author.gender);
        assertEquals(32, author.age);
        assertEquals("test1", author.getId());
        assertEquals("friend1_fn", author.friends.get(0).firstName);
        assertEquals("friend1_ln", author.friends.get(0).lastName);
        assertEquals("friend2_fn", author.friends.get(1).firstName);
        assertEquals("friend2_ln", author.friends.get(1).lastName);

        // assertEquals("Test <b>content</b>", author.testContent.trim());
        String t = author.testContent2.getFirstChild().getTextContent().trim();
        assertEquals("Test", t);

        assertNotEquals(author.content, author.content.trim());

        assertNull(author.testNullByDefaultForList);
        assertNull(author.testNullByDefaultForMap);

        // test map with objects
        assertEquals(2, author.persons.size());
        assertEquals("friend1_ln", author.persons.get("friend1_fn").lastName);
        assertEquals("friend1_fn", author.persons.get("friend1_fn").firstName);
        assertEquals("friend2_ln", author.persons.get("friend2_fn").lastName);
        assertEquals("friend2_fn", author.persons.get("friend2_fn").firstName);
    }

}
