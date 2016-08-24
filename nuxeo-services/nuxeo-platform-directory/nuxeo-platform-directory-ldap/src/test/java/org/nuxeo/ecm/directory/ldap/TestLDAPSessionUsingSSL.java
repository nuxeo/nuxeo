/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.directory.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;

public class TestLDAPSessionUsingSSL extends LDAPDirectoryTestCase {

    @Test
    public void testGetEntries() {
        try (Session session = getLDAPDirectory("userDirectory").getSession()) {
            DocumentModelList entries = session.getEntries();
            assertNotNull(entries);
            assertEquals(4, entries.size());
            List<String> entryIds = new ArrayList<String>();
            for (DocumentModel entry : entries) {
                entryIds.add(entry.getId());
            }
            Collections.sort(entryIds);
            assertEquals("Administrator", entryIds.get(0));
            assertEquals("user1", entryIds.get(1));
            assertEquals("user2", entryIds.get(2));
            assertEquals("user3", entryIds.get(3));
        }
    }

}
