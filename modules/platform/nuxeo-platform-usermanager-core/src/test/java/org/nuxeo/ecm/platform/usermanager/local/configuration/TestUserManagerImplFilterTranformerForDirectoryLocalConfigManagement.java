/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and others.
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

/**
 * This will test the filter on groups transformation to manage the Directory Local Configuration. Directory Local
 * Configuration management for group is a bit different than other ones. Other Directory can be segregate per
 * directory. Groups must be defined into the same directory. The segregation is managed by the suffix added to the
 * groupname.
 *
 * @author bjalon
 */
@Deploy("org.nuxeo.ecm.directory.multi")
@Deploy("org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl-multitenant/directory-for-context-config.xml")
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
        Map<String, Serializable> filter = new HashMap<>();
        HashSet<String> fulltext = new HashSet<>();

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
        Map<String, Serializable> filter = new HashMap<>();
        HashSet<String> fulltext = new HashSet<>();

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
        fulltext = new HashSet<>();
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
        DocumentModel fakeDoc = SimpleDocumentModel.empty();

        Map<String, Serializable> filter = new HashMap<>();
        HashSet<String> fulltext = new HashSet<>();

        umtm.queryTransformer(userManager, filter, fulltext, fakeDoc);
        assertEquals(1, filter.size());
        assertEquals("%-tenanta", filter.get("groupname"));
        assertEquals(1, fulltext.size());
        assertTrue(fulltext.contains("groupname"));

        filter = new HashMap<>();
        fulltext = new HashSet<>();

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
