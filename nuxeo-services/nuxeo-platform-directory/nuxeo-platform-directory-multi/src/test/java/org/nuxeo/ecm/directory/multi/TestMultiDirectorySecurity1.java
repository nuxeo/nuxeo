/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *
 */

package org.nuxeo.ecm.directory.multi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectorySecurityException;
import org.nuxeo.ecm.directory.PermissionDescriptor;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.memory.MemoryDirectory;
import org.nuxeo.ecm.directory.memory.MemoryDirectoryDescriptor;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ MultiDirectoryFeature.class })
@LocalDeploy("org.nuxeo.ecm.directory.multi.tests:directories-security-config.xml")
public class TestMultiDirectorySecurity1 {

    DirectoryService directoryService;

    MemoryDirectory memdir1;

    MemoryDirectory memdir2;

    MemoryDirectory memdir3;

    MultiDirectory multiDir;

    MultiDirectorySession dir;

    @Inject
    ClientLoginFeature dummyLogin;

    protected MemoryDirectoryDescriptor desc1;

    protected MemoryDirectoryDescriptor desc2;

    protected MemoryDirectoryDescriptor desc3;

    public static final String SUPER_USER = "superUser";

    public static final String READER_USER = "readerUser";

    @Before
    public void setUp() throws Exception {
        // mem dir factory
        directoryService = Framework.getService(DirectoryService.class);

        // create and register mem directories
        Map<String, Object> e;

        PermissionDescriptor perm1 = new PermissionDescriptor();
        perm1.name = "Read";
        perm1.users = new String[] { READER_USER };
        PermissionDescriptor perm2 = new PermissionDescriptor();
        perm2.name = "Write";
        perm2.users = new String[] { SUPER_USER };
        PermissionDescriptor[] permissions = new PermissionDescriptor[] { perm1, perm2 };

        // dir 1
        desc1 = new MemoryDirectoryDescriptor();
        desc1.name = "dir1";
        desc1.schemaName = "schema1";
        desc1.schemaSet = new HashSet<>(Arrays.asList("uid", "foo"));
        desc1.idField = "uid";
        desc1.passwordField = "foo";
        desc1.permissions = permissions;
        directoryService.registerDirectoryDescriptor(desc1);
        memdir1 = (MemoryDirectory) directoryService.getDirectory("dir1");

        try (Session dir1 = memdir1.getSession()) {
            e = new HashMap<>();
            e.put("uid", "1");
            e.put("foo", "foo1");
            dir1.createEntry(e);
            e = new HashMap<>();
            e.put("uid", "2");
            e.put("foo", "foo2");
            dir1.createEntry(e);
        }

        // dir 2
        desc2 = new MemoryDirectoryDescriptor();
        desc2.name = "dir2";
        desc2.schemaName = "schema2";
        desc2.schemaSet = new HashSet<>(Arrays.asList("id", "bar"));
        desc2.idField = "id";
        desc2.passwordField = null;
        desc2.permissions = permissions;
        directoryService.registerDirectoryDescriptor(desc2);
        memdir2 = (MemoryDirectory) directoryService.getDirectory("dir2");

        try (Session dir2 = memdir2.getSession()) {
            e = new HashMap<>();
            e.put("id", "1");
            e.put("bar", "bar1");
            dir2.createEntry(e);
            e = new HashMap<>();
            e.put("id", "2");
            e.put("bar", "bar2");
            dir2.createEntry(e);
        }

        // dir 3
        desc3 = new MemoryDirectoryDescriptor();
        desc3.name = "dir3";
        desc3.schemaName = "schema3";
        desc3.schemaSet = new HashSet<>(Arrays.asList("uid", "thefoo", "thebar"));
        desc3.idField = "uid";
        desc3.passwordField = "thefoo";
        desc3.permissions = permissions;
        directoryService.registerDirectoryDescriptor(desc3);
        memdir3 = (MemoryDirectory) directoryService.getDirectory("dir3");

        try (Session dir3 = memdir3.getSession()) {
            e = new HashMap<>();
            e.put("uid", "3");
            e.put("thefoo", "foo3");
            e.put("thebar", "bar3");
            dir3.createEntry(e);
            e = new HashMap<>();
            e.put("uid", "4");
            e.put("thefoo", "foo4");
            e.put("thebar", "bar4");
            dir3.createEntry(e);
        }


        // the multi directory
        multiDir = (MultiDirectory) directoryService.getDirectory("multi");
        dir = (MultiDirectorySession) multiDir.getSession();
    }

    @After
    public void tearDown() throws Exception {
        if (dir != null) {
            dir.close();
        }
        directoryService = Framework.getService(DirectoryService.class);
        directoryService.unregisterDirectoryDescriptor(desc1);
        directoryService.unregisterDirectoryDescriptor(desc2);
        directoryService.unregisterDirectoryDescriptor(desc3);
    }

    @Test
    public void superUserHasWritePermissionOnSubDirectory() throws Exception {
        dummyLogin.login(SUPER_USER);
        assertTrue(((MultiDirectorySession) multiDir.getSession()).hasPermission("Write"));
        dummyLogin.logout();
    }

    @Test
    public void readerCanGetEntry() throws Exception {
        // Given a reader user
        dummyLogin.login(READER_USER);

        DocumentModel entry;
        entry = dir.getEntry("1");
        assertNotNull(entry);

        dummyLogin.logout();
    }

    @Test
    public void readerCantCreateEntry() throws Exception {
        // Given a reader user
        dummyLogin.login(READER_USER);

        Map<String, Object> map = new HashMap<>();
        map.put("uid", "5");
        map.put("thefoo", "foo5");
        map.put("thebar", "bar5");
        try {
            dir.createEntry(map);
            fail("Should not be able to create entry");
        } catch (DirectorySecurityException ee) {
            // ok
        }

        DocumentModel entry = dir.getEntry("5");
        assertNull(entry);

        dummyLogin.logout();
    }

    @Test
    public void superUserCanCreate() throws Exception {
        // Given a super user
        dummyLogin.login(SUPER_USER);

        Map<String, Object> map = new HashMap<>();
        map.put("uid", "5");
        map.put("thefoo", "foo5");
        map.put("thebar", "bar5");
        DocumentModel entry = dir.createEntry(map);
        assertNotNull(entry);

        entry = dir.getEntry("5");
        assertNotNull(entry);

        dummyLogin.logout();
    }

    @Test
    public void superUserCanUpdateEntry() throws Exception {
        // Given a super user
        dummyLogin.login(SUPER_USER);

        // multi-subdirs update
        DocumentModel e = dir.getEntry("1");
        assertEquals("foo1", e.getProperty("schema3", "thefoo"));
        assertEquals("bar1", e.getProperty("schema3", "thebar"));
        e.setProperty("schema3", "thefoo", "fffooo1");
        e.setProperty("schema3", "thebar", "babar1");
        dir.updateEntry(e);
        e = dir.getEntry("1");
        assertEquals("fffooo1", e.getProperty("schema3", "thefoo"));
        assertEquals("babar1", e.getProperty("schema3", "thebar"));

        dummyLogin.logout();
    }

    @Test
    public void readerUserCantUpdateEntry() throws Exception {
        // Given a reader user
        dummyLogin.login(READER_USER);

        // multi-subdirs update
        DocumentModel e = dir.getEntry("1");
        assertEquals("foo1", e.getProperty("schema3", "thefoo"));
        assertEquals("bar1", e.getProperty("schema3", "thebar"));
        e.setProperty("schema3", "thefoo", "fffooo1");
        e.setProperty("schema3", "thebar", "babar1");
        try {
            dir.updateEntry(e);
            fail("Should not be able to update entry");
        } catch (DirectorySecurityException ee) {
            // ok
        }

        e = dir.getEntry("1");
        assertEquals("foo1", e.getProperty("schema3", "thefoo"));
        assertEquals("bar1", e.getProperty("schema3", "thebar"));

        dummyLogin.logout();
    }

    @Test
    public void superUserCanDeleteEntry() throws Exception {
        // Given a super user
        dummyLogin.login(SUPER_USER);

        dir.deleteEntry("1");
        assertNull(dir.getEntry("1"));
        dir.deleteEntry("3");
        assertNull(dir.getEntry("3"));

        dummyLogin.logout();
    }

    @Test
    public void readerUserCantDeleteEntry() throws Exception {
        // Given a reader user
        dummyLogin.login(READER_USER);

        try {
            dir.deleteEntry("1");
            fail("Should not be able to delete entry");
        } catch (DirectorySecurityException ee) {
            // ok
        }
        assertNotNull(dir.getEntry("1"));

        dummyLogin.logout();
    }

    @Test
    public void superUserCanQuery() throws Exception {
        // Given a super user
        dummyLogin.login(SUPER_USER);

        Map<String, Serializable> filter = new HashMap<>();
        DocumentModelList entries;

        // empty filter means everything (like getEntries)
        entries = dir.query(filter);
        assertNotNull(entries);
        assertEquals(4, entries.size());

        dummyLogin.logout();
    }
}
