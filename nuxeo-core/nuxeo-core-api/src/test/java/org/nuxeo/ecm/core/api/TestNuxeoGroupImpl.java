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

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;

public class TestNuxeoGroupImpl extends TestCase {

    public void testConstructor() {
        NuxeoGroup group = new NuxeoGroupImpl("mygroup");

        assertEquals("mygroup", group.getName());
                assertEquals("mygroup", group.toString());

        assertEquals(0, group.getMemberUsers().size());
        assertEquals(0, group.getMemberGroups().size());
        assertEquals(0, group.getParentGroups().size());

        List<String> users = Arrays.asList("joe", "jane");
        group.setMemberUsers(users);
        assertEquals(users, group.getMemberUsers());

        List<String> groups = Arrays.asList("sales", "admin");
        group.setMemberGroups(groups);
        assertEquals(groups, group.getMemberGroups());

        List<String> parentGroups = Arrays.asList("xx", "yy");
        group.setParentGroups(parentGroups);
        assertEquals(parentGroups, group.getParentGroups());
    }

    @SuppressWarnings({"ObjectEqualsNull"})
    public void testEquals() {
        NuxeoGroup group1 = new NuxeoGroupImpl("mygroup");
        NuxeoGroup group2 = new NuxeoGroupImpl("mygroup");
        NuxeoGroup group3 = new NuxeoGroupImpl("yourgroup");

        assertEquals(group1, group1);
        assertEquals(group1, group2);
        assertEquals(group1.hashCode(), group2.hashCode());
        assertFalse(group1.equals(null));
        assertFalse(group1.equals(group3));

        group3.setName("mygroup");
        assertEquals(group1, group3);
        assertEquals(group1.hashCode(), group3.hashCode());
    }

}
