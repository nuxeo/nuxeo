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
 * $Id$
 */

package org.nuxeo.common.xmap;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

public class ReverseXMapTest extends TestCase{

    public void testReverse() throws Exception {
        XMap xmap = new XMap();
        xmap.register(Author.class);

        URL url = Thread.currentThread().getContextClassLoader().getResource("test-xmap.xml");

        Author author = (Author) xmap.load(url);
        try {
            xmap.toXML(new Exception());
            fail("should throw exception ('Exeption' type is not registred)");
        } catch (RuntimeException e){
            // just check if exception is thrown
        }

        // save the object

        System.out.println(xmap.toXML(author));
        File file = File.createTempFile("xmap", "xml");
        file.deleteOnExit();
        xmap.toXML(author, file);

        // load from new created map
        xmap = new XMap();
        xmap.register(Author.class);
        author = (Author) xmap.load(file.toURL());

        assertEquals("First test 22", author.title);
        assertEquals("bla bla", author.description);
        assertEquals(author, author.name.owner);
        assertEquals("my first name", author.name.firstName);
        assertEquals("my last name", author.name.lastName);
        assertEquals("The content", author.content.trim());
        assertEquals("author", author.nameType);
        assertEquals(32, author.age);
        assertEquals("test1", author.getId());
        assertEquals("friend1_fn", author.friends.get(0).firstName);
        assertEquals("friend1_ln", author.friends.get(0).lastName);
        assertEquals("friend2_fn", author.friends.get(1).firstName);
        assertEquals("friend2_ln", author.friends.get(1).lastName);

//        assertEquals("Test <b>content</b>", author.testContent.trim());
        String t = author.testContent2.getFirstChild().getTextContent().trim();
        assertEquals("Test", t);

        assertFalse(author.content.equals(author.content.trim()));

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
