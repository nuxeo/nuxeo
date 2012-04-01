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

package org.nuxeo.ecm.core.api.security;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestACE {
    private ACE ace;

    @Before
    public void setUp() {
        ace = new ACE("bogdan", "write", false);
    }

    @After
    public void tearDown() {
        ace = null;
    }

    @Test
    public void testGetType() {
        assertFalse(ace.isGranted());
        assertTrue(ace.isDenied());
    }

    @Test
    public void testGetPrincipals() {
        assertEquals("bogdan", ace.getUsername());
    }

    @Test
    public void testGetPermissions() {
        assertEquals("write", ace.getPermission());
    }

    @SuppressWarnings({"ObjectEqualsNull"})
    @Test
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

    @Test
    public void testToString() {
        assertEquals("bogdan:write:false", ace.toString());
    }

}
