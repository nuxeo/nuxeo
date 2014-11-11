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

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.security.impl.ACLImpl;

public class TestACL extends TestCase {
    private ACL acl;

    @Override
    public void setUp() {
        acl = new ACLImpl("test acl");
    }

    @Override
    public void tearDown() {
        acl = null;
    }

    public void testGetName() {
        assertEquals("test acl", acl.getName());
    }

    public void testAddingACEs() {
        assertEquals(0, acl.getACEs().length);
        acl.add(new ACE("bogdan", "write", false));
        assertEquals(1, acl.getACEs().length);
    }

}
