/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bjalon
 */
package org.nuxeo.ecm.platform.usermanager.local.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.platform.usermanager.UserManagerImpl;
import org.nuxeo.ecm.platform.usermanager.UserManagerTestCase;
import org.nuxeo.ecm.platform.usermanager.UserMultiTenantManagement;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * This will test the filter on groups transformation to manage the Directory Local Configuration. Directory Local
 * Configuration management for group is a bit different than other ones. Other Directory can be segregate per
 * directory. Groups must be defined into the same directory. The segregation is managed by the suffix added to the
 * groupname.
 *
 * @author bjalon
 */
@Deploy("org.nuxeo.ecm.directory.multi")
@LocalDeploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl-multitenant/directory-for-context-config.xml")
public class TestUserManagerImplFilterTranformerForDirectoryLocalConfigManagement extends UserManagerTestCase {

    protected UserMultiTenantManagement umtm;

    @Before
    public void setUp() throws Exception {
        umtm = new DefaultUserMultiTenantManagementMock();
        // needed to simulate the directory local configuration
        ((UserManagerImpl) userManager).multiTenantManagement = umtm;
    }

    @Test
    public void testShouldThrowExceptionIfFilterOrFulltextNull() {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        HashSet<String> fulltext = new HashSet<String>();

        try {
            umtm.queryTransformer(userManager, null, null, null);
            fail();
        } catch (NuxeoException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Filter and Fulltext must be not null"));
        }

        try {
            umtm.queryTransformer(userManager, filter, null, null);
            fail();
        } catch (NuxeoException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Filter and Fulltext must be not null"));
        }

        try {
            umtm.queryTransformer(userManager, null, fulltext, null);
            fail();
        } catch (NuxeoException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Filter and Fulltext must be not null"));
        }
    }

    @Test
    public void testShouldReturnAFilterNotChangedIfNoDirectoryLocalConfig() {
        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        HashSet<String> fulltext = new HashSet<String>();

        umtm.queryTransformer(userManager, filter, fulltext, null);
        assertEquals(0, filter.size());
        assertEquals(0, fulltext.size());

        filter.put("groupname", "test");
        fulltext.add("groupname");
        umtm.queryTransformer(userManager, filter, fulltext, null);
        assertEquals(1, filter.size());
        assertEquals("test", filter.get("groupname"));
        assertEquals(1, fulltext.size());

        filter.put("groupname", "test");
        fulltext = new HashSet<String>();
        fulltext.add("test");
        umtm.queryTransformer(userManager, filter, fulltext, null);
        assertEquals(1, filter.size());
        assertEquals("test", filter.get("groupname"));
        assertEquals(1, fulltext.size());

        filter.put("groupname", "test");
        fulltext.add("groupname");
        umtm.queryTransformer(userManager, filter, fulltext, null);
        assertEquals(1, filter.size());
        assertEquals("test", filter.get("groupname"));
        assertEquals(2, fulltext.size());
    }

    @Test
    public void testShouldReturnAFilterWithSuffixAdded() {
        DocumentModel fakeDoc = new SimpleDocumentModel();

        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        HashSet<String> fulltext = new HashSet<String>();

        umtm.queryTransformer(userManager, filter, fulltext, fakeDoc);
        assertEquals(1, filter.size());
        assertEquals("%-tenanta", filter.get("groupname"));
        assertEquals(1, fulltext.size());
        assertTrue(fulltext.contains("groupname"));

        filter = new HashMap<String, Serializable>();
        fulltext = new HashSet<String>();

        filter.put("groupname", "test");
        umtm.queryTransformer(userManager, filter, fulltext, fakeDoc);
        assertEquals(1, filter.size());
        assertEquals("test-tenanta", filter.get("groupname"));
        assertEquals(0, fulltext.size());

        filter.put("groupname", "test%");
        fulltext.add("groupname");
        umtm.queryTransformer(userManager, filter, fulltext, fakeDoc);
        assertEquals(1, filter.size());
        assertEquals("test%-tenanta", filter.get("groupname"));
        assertEquals(1, fulltext.size());
        assertTrue(fulltext.contains("groupname"));
    }
}
