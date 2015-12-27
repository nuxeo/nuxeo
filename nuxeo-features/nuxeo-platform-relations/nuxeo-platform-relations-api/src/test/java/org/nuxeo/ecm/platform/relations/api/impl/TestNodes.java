/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: TestNodes.java 19155 2007-05-22 16:19:48Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.exceptions.InvalidLiteralException;

public class TestNodes {

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testEqualsResource() {
        Resource res1 = new ResourceImpl("http://namespace/uri");
        Resource res2 = new ResourceImpl("http://namespace/uri");
        Resource res3 = new ResourceImpl("http://namespace/urieuh");
        assertEquals(res1, res2);
        assertEquals(res1.hashCode(), res2.hashCode());
        assertFalse(res1.equals(res3));
    }

    @Test
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

    @Test
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

    @Test
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
