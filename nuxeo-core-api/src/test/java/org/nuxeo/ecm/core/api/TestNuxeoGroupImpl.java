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
