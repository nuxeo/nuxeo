/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 *
 */

package org.nuxeo.ecm.directory.multi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.memory.MemoryDirectory;
import org.nuxeo.ecm.directory.memory.MemoryDirectoryFactory;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ MultiDirectoryFeature.class })
@LocalDeploy("org.nuxeo.ecm.directory.multi.tests:directories-security-config.xml")
public class TestMultiDirectorySecurity {

    DirectoryService directoryService;

    MemoryDirectoryFactory memoryDirectoryFactory;

    MemoryDirectory memdir1;

    MemoryDirectory memdir2;

    MemoryDirectory memdir3;

    MultiDirectory multiDir;

    MultiDirectorySession dir;

    MultiDirectorySession dirGroup;

    @Inject
    ClientLoginFeature dummyLogin;

    public static final String SUPER_USER = "superUser";

    public static final String READER_USER = "readerUser";

    @Before
    public void setUp() throws Exception {
        // mem dir factory
        directoryService = Framework.getLocalService(DirectoryService.class);
        memoryDirectoryFactory = new MemoryDirectoryFactory();
        directoryService.registerDirectory("memdirs", memoryDirectoryFactory);

        // create and register mem directories
        Map<String, Object> e;

        // dir 1
        Set<String> schema1Set = new HashSet<String>(
                Arrays.asList("uid", "foo"));
        memdir1 = new MemoryDirectory("dir1", "schema1", schema1Set, "uid",
                "foo");
        memoryDirectoryFactory.registerDirectory(memdir1);

        Session dir1 = memdir1.getSession();
        e = new HashMap<String, Object>();
        e.put("uid", "1");
        e.put("foo", "foo1");
        dir1.createEntry(e);
        e = new HashMap<String, Object>();
        e.put("uid", "2");
        e.put("foo", "foo2");
        dir1.createEntry(e);

        // dir 2
        Set<String> schema2Set = new HashSet<String>(Arrays.asList("id", "bar"));
        memdir2 = new MemoryDirectory("dir2", "schema2", schema2Set, "id", null);
        memoryDirectoryFactory.registerDirectory(memdir2);

        Session dir2 = memdir2.getSession();
        e = new HashMap<String, Object>();
        e.put("id", "1");
        e.put("bar", "bar1");
        dir2.createEntry(e);
        e = new HashMap<String, Object>();
        e.put("id", "2");
        e.put("bar", "bar2");
        dir2.createEntry(e);

        // dir 3
        Set<String> schema3Set = new HashSet<String>(Arrays.asList("uid",
                "thefoo", "thebar"));
        memdir3 = new MemoryDirectory("dir3", "schema3", schema3Set, "uid",
                "thefoo");
        memoryDirectoryFactory.registerDirectory(memdir3);

        Session dir3 = memdir3.getSession();
        e = new HashMap<String, Object>();
        e.put("uid", "3");
        e.put("thefoo", "foo3");
        e.put("thebar", "bar3");
        dir3.createEntry(e);
        e = new HashMap<String, Object>();
        e.put("uid", "4");
        e.put("thefoo", "foo4");
        e.put("thebar", "bar4");
        dir3.createEntry(e);

        // the multi directory
        multiDir = (MultiDirectory) directoryService.getDirectory("multi");
        dir = (MultiDirectorySession) multiDir.getSession();

        dirGroup = (MultiDirectorySession) ((MultiDirectory) directoryService.getDirectory("multi-group")).getSession();
    }

    @After
    public void tearDown() throws Exception {
        memoryDirectoryFactory.unregisterDirectory(memdir1);
        memoryDirectoryFactory.unregisterDirectory(memdir2);
        memoryDirectoryFactory.unregisterDirectory(memdir3);
        directoryService.unregisterDirectory("memdirs", memoryDirectoryFactory);
    }

    @Test
    public void readerCanGetEntry() throws Exception {
        // Given a reader user
        dummyLogin.loginAs(READER_USER);

        DocumentModel entry;
        entry = dir.getEntry("1");
        assertNotNull(entry);

        dummyLogin.logout();
    }

    @Test
    public void readerCantCreateEntry() throws Exception {
        // Given a reader user
        dummyLogin.loginAs(READER_USER);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("uid", "5");
        map.put("thefoo", "foo5");
        map.put("thebar", "bar5");
        DocumentModel entry = dir.createEntry(map);
        assertNull(entry);

        entry = dir.getEntry("5");
        assertNull(entry);

        dummyLogin.logout();
    }

    @Test
    public void superUserCanCreate() throws Exception {
        // Given a super user
        dummyLogin.loginAs(SUPER_USER);

        Map<String, Object> map = new HashMap<String, Object>();
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
        dummyLogin.loginAs(SUPER_USER);

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
        dummyLogin.loginAs(READER_USER);

        // multi-subdirs update
        DocumentModel e = dir.getEntry("1");
        assertEquals("foo1", e.getProperty("schema3", "thefoo"));
        assertEquals("bar1", e.getProperty("schema3", "thebar"));
        e.setProperty("schema3", "thefoo", "fffooo1");
        e.setProperty("schema3", "thebar", "babar1");
        dir.updateEntry(e);
        e = dir.getEntry("1");
        assertEquals("foo1", e.getProperty("schema3", "thefoo"));
        assertEquals("bar1", e.getProperty("schema3", "thebar"));

        dummyLogin.logout();
    }

    @Test
    public void superUserCanDeleteEntry() throws Exception {
        // Given a super user
        dummyLogin.loginAs(SUPER_USER);

        dir.deleteEntry("1");
        assertNull(dir.getEntry("1"));
        dir.deleteEntry("3");
        assertNull(dir.getEntry("3"));

        dummyLogin.logout();
    }

    @Test
    public void readerUserCantDeleteEntry() throws Exception {
        // Given a reader user
        dummyLogin.loginAs(READER_USER);

        dir.deleteEntry("1");
        assertNotNull(dir.getEntry("1"));

        dummyLogin.logout();
    }

    @Test
    public void superUserCanQuery() throws Exception {
        // Given a super user
        dummyLogin.loginAs(SUPER_USER);

        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        DocumentModelList entries;

        // empty filter means everything (like getEntries)
        entries = dir.query(filter);
        assertNotNull(entries);
        assertEquals(4, entries.size());

        dummyLogin.logout();
    }

    @Test
    public void everyoneUserCanCreateAndGet() throws Exception {
        // Given a user in the everyone group 
        //(default in dummy login any user is member of everyone)
        dummyLogin.loginAs("anEveryoneUser");

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("uid", "5");
        map.put("thefoo", "foo5");
        map.put("thebar", "bar5");
        
        //When I call the multi-group dir
        DocumentModel entry = dirGroup.createEntry(map);
        assertNotNull(entry);

        //I can create and then get entry
        entry = dirGroup.getEntry("5");
        assertNotNull(entry);

        dummyLogin.logout();
    }

}
