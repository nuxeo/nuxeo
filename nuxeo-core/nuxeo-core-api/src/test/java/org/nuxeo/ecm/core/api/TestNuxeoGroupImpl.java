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

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.api.impl.NuxeoGroupImpl;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestNuxeoGroupImpl {

    @Test
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

    @SuppressWarnings({ "ObjectEqualsNull" })
    @Test
    public void testEquals() {
        NuxeoGroup group1 = new NuxeoGroupImpl("mygroup");
        NuxeoGroup group2 = new NuxeoGroupImpl("mygroup");
        NuxeoGroup group3 = new NuxeoGroupImpl("yourgroup", "yourlabel");

        assertEquals(group1, group1);
        assertEquals(group1, group2);
        assertEquals(group1.hashCode(), group2.hashCode());
        assertFalse(group1.equals(null));
        assertFalse(group1.equals(group3));
        assertEquals("mygroup", group2.getLabel());

        group3.setName("mygroup");
        assertEquals(group1, group3);
        assertEquals(group1.hashCode(), group3.hashCode());
    }

}
