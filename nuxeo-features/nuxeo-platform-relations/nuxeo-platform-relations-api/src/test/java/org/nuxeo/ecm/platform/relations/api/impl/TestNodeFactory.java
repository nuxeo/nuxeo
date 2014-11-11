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
 * $Id: TestNodeFactory.java 22853 2007-07-22 21:09:50Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.Resource;

public class TestNodeFactory extends TestCase {

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

    public void testCreateBlank() {
        BlankImpl blank = NodeFactory.createBlank();
        assertNotNull(blank);
        assertFalse(blank.isLiteral());
        assertTrue(blank.isBlank());
        assertFalse(blank.isResource());
        assertNull(blank.getId());
    }

    public void testCreateBlankWithId() {
        BlankImpl blank = NodeFactory.createBlank("myId");
        assertNotNull(blank);
        assertFalse(blank.isLiteral());
        assertTrue(blank.isBlank());
        assertFalse(blank.isResource());
        assertEquals("myId", blank.getId());
    }

    public void testCreateResource() {
        Resource res = NodeFactory.createResource("http://uri");
        assertNotNull(res);
        assertFalse(res.isLiteral());
        assertFalse(res.isBlank());
        assertTrue(res.isResource());
        assertEquals("http://uri", res.getUri());
    }

    public void testCreateQNameResource() {
        QNameResource res = NodeFactory.createQNameResource("http://dummy/",
                "uri");
        assertNotNull(res);
        assertFalse(res.isLiteral());
        assertFalse(res.isBlank());
        assertTrue(res.isResource());
        assertEquals("http://dummy/uri", res.getUri());
        assertEquals("uri", res.getLocalName());
        assertEquals(res.getClass(), QNameResourceImpl.class);
    }

}
