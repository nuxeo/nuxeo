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

package org.nuxeo.runtime;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestVersion {

    Version version;

    @Before
    public void setUp() {
        version = new Version(1, 2, 3);
    }

    @Test
    public void testAccessors() {
        assertEquals(1, version.getMajorVersion());
        assertEquals(2, version.getMinorVersion());
        assertEquals(3, version.getUpdateVersion());
        assertEquals("1.2.3", version.toString());
    }

    @Test
    public void testParseString() {
        version = Version.parseString("0.0.0");
        assertEquals("0.0.0", version.toString());
        version = Version.parseString("2");
        assertEquals("2.0.0", version.toString());
        version = Version.parseString("3.1");
        assertEquals("3.1.0", version.toString());
    }

    @SuppressWarnings({ "SimplifiableJUnitAssertion", "EqualsBetweenInconvertibleTypes" })
    @Test
    public void testEquals() {
        assertTrue(version.equals(new Version(1, 2, 3)));
        assertFalse(version.equals(""));
    }

    @Test
    public void testIsGreaterThan() {
        assertTrue(version.isGreaterThan(new Version(0, 0, 0)));
        assertTrue(version.isGreaterThan(new Version(0, 0, 1)));
        assertTrue(version.isGreaterThan(new Version(0, 1, 0)));
        assertTrue(version.isGreaterThan(new Version(1, 0, 0)));
        assertTrue(version.isGreaterThan(new Version(1, 2, 0)));
        assertTrue(version.isGreaterThan(new Version(1, 2, 2)));

        assertFalse(version.isGreaterThan(new Version(1, 2, 3)));
        assertFalse(version.isGreaterThan(new Version(1, 2, 4)));
        assertFalse(version.isGreaterThan(new Version(1, 3, 0)));
        assertFalse(version.isGreaterThan(new Version(2, 0, 0)));
        assertFalse(version.isGreaterThan(new Version(2, 0, 1)));
        assertFalse(version.isGreaterThan(new Version(2, 1, 0)));
    }

    @Test
    public void testIsGreaterOrEqualThan() {
        assertTrue(version.isGreaterOrEqualThan(new Version(0, 0, 0)));
        assertTrue(version.isGreaterOrEqualThan(new Version(0, 0, 1)));
        assertTrue(version.isGreaterOrEqualThan(new Version(0, 1, 0)));
        assertTrue(version.isGreaterOrEqualThan(new Version(1, 0, 0)));
        assertTrue(version.isGreaterOrEqualThan(new Version(1, 2, 0)));
        assertTrue(version.isGreaterOrEqualThan(new Version(1, 2, 2)));
        assertTrue(version.isGreaterOrEqualThan(new Version(1, 2, 3)));

        assertFalse(version.isGreaterOrEqualThan(new Version(1, 2, 4)));
        assertFalse(version.isGreaterOrEqualThan(new Version(1, 3, 0)));
        assertFalse(version.isGreaterOrEqualThan(new Version(2, 0, 0)));
        assertFalse(version.isGreaterOrEqualThan(new Version(2, 0, 1)));
        assertFalse(version.isGreaterOrEqualThan(new Version(2, 1, 0)));
    }

    @Test
    public void testHashCode() {
        version = new Version(0, 0, 0);
        assertEquals(0, version.hashCode());
    }

}
