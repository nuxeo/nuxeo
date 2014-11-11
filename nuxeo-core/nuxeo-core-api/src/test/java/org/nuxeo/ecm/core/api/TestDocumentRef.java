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

import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

@SuppressWarnings({"EqualsBetweenInconvertibleTypes"})
public class TestDocumentRef {

    protected PathRef pathref;
    protected IdRef idref;

    @Before
    public void setUp() throws Exception {
        pathref = new PathRef("path/to/doc");
        idref = new IdRef("some_uid");
    }

    @Test
    public void testHashCode() {
        assertNotNull(pathref.hashCode());
        assertNotNull(idref.hashCode());
    }

    @Test
    public void testType() {
        assertEquals(DocumentRef.PATH, pathref.type());
        assertEquals(DocumentRef.ID, idref.type());
    }

    @Test
    public void testReference() {
        assertEquals("path/to/doc", pathref.reference());
        assertEquals("some_uid", idref.reference());
    }

    @SuppressWarnings({"SimplifiableJUnitAssertion"})
    @Test
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

        // check that we have a consistent behavior when wrapped into document locations
        DocumentLocation server1IdRefLocation = new DocumentLocationImpl("server1", idref);
        DocumentLocation server1IdRef2Location = new DocumentLocationImpl("server1", idref2);
        DocumentLocation server1IdRef3Location = new DocumentLocationImpl("server1", idref3);
        DocumentLocation server1PathRefLocation = new DocumentLocationImpl("server1", pathref);
        DocumentLocation server1PathRef2Location = new DocumentLocationImpl("server1", pathref2);
        DocumentLocation server1PathRef3Location = new DocumentLocationImpl("server1", pathref3);
        DocumentLocation server2IdRefLocation = new DocumentLocationImpl("server2", idref);
        DocumentLocation server2PathRefLocation = new DocumentLocationImpl("server2", pathref);

        assertEquals(server1IdRefLocation, server1IdRefLocation);
        assertEquals(server1IdRefLocation, server1IdRef2Location);
        assertFalse(server1IdRefLocation.equals(server1IdRef3Location));
        assertFalse(server1IdRefLocation.equals(server1PathRefLocation));
        assertFalse(server1IdRefLocation.equals(server1PathRef3Location));
        assertFalse(server1IdRefLocation.equals(server2IdRefLocation));

        assertEquals(server1PathRefLocation, server1PathRef2Location);
        assertFalse(server1PathRefLocation.equals(server1PathRef3Location));
        assertFalse(server1PathRefLocation.equals(server2PathRefLocation));

        // check hashCode consistency
        assertTrue(server1IdRefLocation.hashCode() == server1IdRef2Location.hashCode());
        assertTrue(server1IdRefLocation.hashCode() != server2IdRefLocation.hashCode());
        assertTrue(server1IdRefLocation.hashCode() != server1IdRef3Location.hashCode());
        assertTrue(server1IdRefLocation.hashCode() != server1PathRef2Location.hashCode());

        assertTrue(server1PathRefLocation.hashCode() == server1PathRef2Location.hashCode());
        assertTrue(server1PathRefLocation.hashCode() != server1PathRef3Location.hashCode());
        assertTrue(server1PathRefLocation.hashCode() != server2PathRefLocation.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("path/to/doc", pathref.toString());
        assertEquals("some_uid", idref.toString());
    }

}
