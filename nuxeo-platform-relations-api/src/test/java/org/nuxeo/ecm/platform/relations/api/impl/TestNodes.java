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
 * $Id: TestNodes.java 19155 2007-05-22 16:19:48Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.exceptions.InvalidLiteralException;

public class TestNodes extends TestCase {

    public void testValidLiteral() {
        LiteralImpl lit1 = new LiteralImpl("Hello");
        lit1.setType("text");
        try {
            lit1.setLanguage("en");
            fail("Should have raised an InvalidLiteralException");
        } catch (InvalidLiteralException e) {
        }
        LiteralImpl lit2 = new LiteralImpl("Hello");
        lit2.setLanguage("en");
        try {
            lit2.setType("text");
            fail("Should have raised an InvalidLiteralException");
        } catch (InvalidLiteralException e) {
        }
    }

    public void testEqualsLiteral() {
        LiteralImpl lit1 = new LiteralImpl("Hello");
        assertEquals(lit1, lit1);

        LiteralImpl lit2 = new LiteralImpl(String.valueOf("Hello"));
        assertEquals(lit1, lit2);
        assertEquals(lit1.hashCode(), lit2.hashCode());

        LiteralImpl lit3 = new LiteralImpl("Hello");
        lit3.setLanguage("en");
        LiteralImpl lit4 = new LiteralImpl("Hello");
        lit4.setType("text");
        assertFalse(lit1.equals(lit3));
        assertFalse(lit1.equals(lit4));
        assertFalse(lit3.equals(lit4));
    }

    public void testEqualsBlank() {
        BlankImpl bl1 = new BlankImpl();
        BlankImpl bl2 = new BlankImpl();
        BlankImpl bl3 = new BlankImpl("hello");
        BlankImpl bl4 = new BlankImpl("hello");
        assertEquals(bl1, bl2);
        assertEquals(bl1.hashCode(), bl2.hashCode());
        assertEquals(bl3, bl4);
        assertFalse(bl1.equals(bl3));
    }

    public void testEqualsResource() {
        Resource res1 = new ResourceImpl("http://namespace/uri");
        Resource res2 = new ResourceImpl("http://namespace/uri");
        Resource res3 = new ResourceImpl("http://namespace/urieuh");
        assertEquals(res1, res2);
        assertEquals(res1.hashCode(), res2.hashCode());
        assertFalse(res1.equals(res3));
    }

    public void testLiteralSerialization() throws Exception {
        LiteralImpl lit = new LiteralImpl("Hello");

        // serialize
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(lit);
        oos.close();
        assertTrue(out.toByteArray().length > 0);

        // deserialize
        byte[] pickled = out.toByteArray();
        InputStream in = new ByteArrayInputStream(pickled);
        ObjectInputStream ois = new ObjectInputStream(in);
        Object o = ois.readObject();

        LiteralImpl newLit = (LiteralImpl) o;

        assertEquals(newLit, lit);
    }

    public void testBlankSerialization() throws Exception {
        BlankImpl blank = new BlankImpl("hello");

        // serialize
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(blank);
        oos.close();
        assertTrue(out.toByteArray().length > 0);

        // deserialize
        byte[] pickled = out.toByteArray();
        InputStream in = new ByteArrayInputStream(pickled);
        ObjectInputStream ois = new ObjectInputStream(in);
        Object o = ois.readObject();

        BlankImpl newBlank = (BlankImpl) o;

        assertEquals(newBlank, blank);
    }

    public void testResourceSerialization() throws Exception {
        Resource res = new ResourceImpl("http://namespace/uri");

        // serialize
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(res);
        oos.close();
        assertTrue(out.toByteArray().length > 0);

        // deserialize
        byte[] pickled = out.toByteArray();
        InputStream in = new ByteArrayInputStream(pickled);
        ObjectInputStream ois = new ObjectInputStream(in);
        Object o = ois.readObject();

        Resource newRes = (Resource) o;

        assertEquals(newRes, res);
    }

}
