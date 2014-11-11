/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.directory.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;

/**
 */
public class TestLDAPSessionUsingSSL extends LDAPDirectoryTestCase {

    @Before
    public void setUp() throws Exception {
    	EXTERNAL_SERVER_SETUP = "TestDirectoriesWithExternalOpenLDAP-SSL.xml";
        super.setUp();
    }
    
    @Test
    public void testGetEntries() throws ClientException {
        Session session = getLDAPDirectory("userDirectory").getSession();
        try {
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
        } finally {
            session.close();
        }
    }

}
