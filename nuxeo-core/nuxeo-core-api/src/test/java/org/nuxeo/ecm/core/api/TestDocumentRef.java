/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api;

import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

@SuppressWarnings({ "EqualsBetweenInconvertibleTypes" })
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
