/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api;

import junit.framework.TestCase;

@SuppressWarnings({"EqualsBetweenInconvertibleTypes"})
public class TestDocumentRef extends TestCase {

    protected PathRef pathref;
    protected IdRef idref;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pathref = new PathRef("path/to/doc");
        idref = new IdRef("some_uid");
    }

    public void testHashCode() {
        assertNotNull(pathref.hashCode());
        assertNotNull(idref.hashCode());
    }

    public void testType() {
        assertEquals(DocumentRef.PATH, pathref.type());
        assertEquals(DocumentRef.ID, idref.type());
    }

    public void testReference() {
        assertEquals("path/to/doc", pathref.reference());
        assertEquals("some_uid", idref.reference());
    }

    @SuppressWarnings({"SimplifiableJUnitAssertion"})
    public void testEqualsObject() {
        assertTrue(idref.equals(idref));
        assertTrue(pathref.equals(pathref));

        IdRef idref2 = new IdRef("some_uid");
        PathRef pathref2 = new PathRef("path/to", "doc");

        assertTrue(idref.equals(idref2));
        assertTrue(pathref.equals(pathref2));

        assertTrue(idref.equals(idref));
        assertTrue(pathref.equals(pathref));

        assertFalse(idref.equals(pathref));
        assertFalse(pathref.equals(idref));

        IdRef idref3 = new IdRef("path/to/name");
        PathRef pathref3 = new PathRef("some_uid");

        // pathes of different types are not comparable even though the
        // values do match
        assertFalse(idref.equals(pathref3));
        assertFalse(pathref.equals(idref3));

    }

    public void testToString() {
        assertEquals("path/to/doc", pathref.toString());
        assertEquals("some_uid", idref.toString());
    }

}
