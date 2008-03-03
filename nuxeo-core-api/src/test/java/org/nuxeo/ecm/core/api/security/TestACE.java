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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.security;

import junit.framework.TestCase;


public class TestACE extends TestCase {
    private ACE ace;

    @Override
    public void setUp() {
        ace = new ACE("bogdan", "write", false);
    }

    @Override
    public void tearDown() {
        ace = null;
    }

    public void testGetType() {
        assertFalse(ace.isGranted());
        assertTrue(ace.isDenied());
    }

    public void testGetPrincipals() {
        assertEquals("bogdan", ace.getUsername());
    }

    public void testGetPermissions() {
        assertEquals("write", ace.getPermission());
    }

    @SuppressWarnings({"ObjectEqualsNull"})
    public void testEquals() {
        ACE ace2 = new ACE("bogdan", "write", false);
        ACE ace3 = new ACE("raoul", "write", false);
        ACE ace4 = new ACE("bogdan", "read", false);
        ACE ace5 = new ACE("bogdan", "write", true);

        assertEquals(ace, ace);
        assertEquals(ace, ace2);
        assertEquals(ace2, ace);
        assertFalse(ace.equals(null));
        assertFalse(ace.equals(ace3));
        assertFalse(ace.equals(ace4));
        assertFalse(ace.equals(ace5));

        assertEquals(ace.hashCode(), ace2.hashCode());
    }

    public void testToString() {
        assertEquals("bogdan:write:false", ace.toString());
    }

}
