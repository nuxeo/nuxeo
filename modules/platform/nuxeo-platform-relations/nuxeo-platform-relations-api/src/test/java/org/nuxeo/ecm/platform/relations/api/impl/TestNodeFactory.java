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
 * $Id: TestNodeFactory.java 22853 2007-07-22 21:09:50Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.Resource;

public class TestNodeFactory {

    @Test
    public void testCreateLiteral() {
        LiteralImpl lit = NodeFactory.createLiteral("Hello");
        assertNotNull(lit);
        assertTrue(lit.isLiteral());
        assertFalse(lit.isBlank());
        assertFalse(lit.isResource());
        assertEquals("Hello", lit.getValue());
        assertNull(lit.getLanguage());
        assertNull(lit.getType());
    }

    @Test
    public void testCreateLiteralWithLanguage() {
        LiteralImpl lit = NodeFactory.createLiteral("Hello", "en");
        assertNotNull(lit);
        assertTrue(lit.isLiteral());
        assertFalse(lit.isBlank());
        assertFalse(lit.isResource());
        assertEquals("Hello", lit.getValue());
        assertEquals("en", lit.getLanguage());
        assertNull(lit.getType());
    }

    @Test
    public void testCreateTypedLiteral() {
        LiteralImpl lit = NodeFactory.createTypedLiteral("Hello", "myType");
        assertNotNull(lit);
        assertTrue(lit.isLiteral());
        assertFalse(lit.isBlank());
        assertFalse(lit.isResource());
        assertEquals("Hello", lit.getValue());
        assertNull(lit.getLanguage());
        assertEquals("myType", lit.getType());
    }

    @Test
    public void testCreateBlank() {
        BlankImpl blank = NodeFactory.createBlank();
        assertNotNull(blank);
        assertFalse(blank.isLiteral());
        assertTrue(blank.isBlank());
        assertFalse(blank.isResource());
        assertNull(blank.getId());
    }

    @Test
    public void testCreateBlankWithId() {
        BlankImpl blank = NodeFactory.createBlank("myId");
        assertNotNull(blank);
        assertFalse(blank.isLiteral());
        assertTrue(blank.isBlank());
        assertFalse(blank.isResource());
        assertEquals("myId", blank.getId());
    }

    @Test
    public void testCreateResource() {
        Resource res = NodeFactory.createResource("http://uri");
        assertNotNull(res);
        assertFalse(res.isLiteral());
        assertFalse(res.isBlank());
        assertTrue(res.isResource());
        assertEquals("http://uri", res.getUri());
    }

    @Test
    public void testCreateQNameResource() {
        QNameResource res = NodeFactory.createQNameResource("http://dummy/", "uri");
        assertNotNull(res);
        assertFalse(res.isLiteral());
        assertFalse(res.isBlank());
        assertTrue(res.isResource());
        assertEquals("http://dummy/uri", res.getUri());
        assertEquals("uri", res.getLocalName());
        assertEquals(res.getClass(), QNameResourceImpl.class);
    }

}
