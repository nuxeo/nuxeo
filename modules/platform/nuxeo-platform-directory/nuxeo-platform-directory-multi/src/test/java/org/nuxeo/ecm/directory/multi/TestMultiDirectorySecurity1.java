/*
 * (C) Copyright 2014-2019 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.local.WithUser;
import org.nuxeo.ecm.directory.DirectorySecurityException;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.memory.MemoryDirectory;
import org.nuxeo.ecm.directory.memory.MemoryDirectoryDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ MultiDirectoryFeature.class })
@Deploy("org.nuxeo.ecm.directory.multi.tests:directories-security-config.xml")
@Deploy("org.nuxeo.ecm.directory.multi.tests:directories-mem-config.xml")
@Deploy("org.nuxeo.ecm.directory.multi.tests:directories-mem-perm-config1.xml")
public class TestMultiDirectorySecurity1 {

    @Inject
    protected DirectoryService directoryService;

    MemoryDirectory memdir1;

    MemoryDirectory memdir2;

    MemoryDirectory memdir3;

    MultiDirectory multiDir;

    MultiDirectorySession dir;

    protected MemoryDirectoryDescriptor desc1;

    protected MemoryDirectoryDescriptor desc2;

    protected MemoryDirectoryDescriptor desc3;

    public static final String SUPER_USER = "superUser";

    public static final String READER_USER = "readerUser";

    @Before
    public void setUp() throws Exception {
        // as WithUser logs in the desired user before @Before and logs out after @After we need more permissions
        Framework.doPrivileged(this::setUpWithPrivileged);
    }

    public void setUpWithPrivileged() {
        // init mem directories
        memdir1 = (MemoryDirectory) directoryService.getDirectory("dir1");
        TestDirectoryHelper.fillMemDir(memdir1, List.of( //
                Map.of("uid", "1", "password", "pw1", "foo", "foo1"), //
                Map.of("uid", "2", "password", "pw2", "foo", "foo2")));
        memdir2 = (MemoryDirectory) directoryService.getDirectory("dir2");
        TestDirectoryHelper.fillMemDir(memdir2, List.of( //
                Map.of("id", "1", "bar", "bar1"), //
                Map.of("id", "2", "bar", "bar2")));
        memdir3 = (MemoryDirectory) directoryService.getDirectory("dir3");
        TestDirectoryHelper.fillMemDir(memdir3, List.of( //
                Map.of("uid", "3", "thepass", "pw3", "thefoo", "foo3", "thebar", "bar3"), //
                Map.of("uid", "4", "thepass", "pw4", "thefoo", "foo4", "thebar", "bar4")));

        // the multi directory
        multiDir = (MultiDirectory) directoryService.getDirectory("multi");
        assertNotNull(multiDir);
        dir = multiDir.getSession();
    }

    @After
    @WithUser // as Administrator
    public void tearDown() {
        if (dir != null) {
            dir.close();
        }
        TestDirectoryHelper.clearDirectory(memdir1);
        TestDirectoryHelper.clearDirectory(memdir2);
        TestDirectoryHelper.clearDirectory(memdir3);
    }

    @Test
    @WithUser(SUPER_USER)
    public void superUserHasWritePermissionOnSubDirectory() {
        assertTrue(multiDir.getSession().hasPermission("Write"));
    }

    @Test
    @WithUser(READER_USER)
    public void readerCanGetEntry() {
        DocumentModel entry;
        entry = dir.getEntry("1");
        assertNotNull(entry);
    }

    @Test
    @WithUser(READER_USER)
    public void readerCantCreateEntry() {
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
    }

    @Test
    @WithUser(SUPER_USER)
    public void superUserCanCreate() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", "5");
        map.put("thefoo", "foo5");
        map.put("thebar", "bar5");
        DocumentModel entry = dir.createEntry(map);
        assertNotNull(entry);

        entry = dir.getEntry("5");
        assertNotNull(entry);
    }

    @Test
    @WithUser(SUPER_USER)
    public void superUserCanUpdateEntry() {
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
    }

    @Test
    @WithUser(READER_USER)
    public void readerUserCantUpdateEntry() {
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
    }

    @Test
    @WithUser(SUPER_USER)
    public void superUserCanDeleteEntry() {
        dir.deleteEntry("1");
        assertNull(dir.getEntry("1"));
        dir.deleteEntry("3");
        assertNull(dir.getEntry("3"));
    }

    @Test
    @WithUser(READER_USER)
    public void readerUserCantDeleteEntry() {
        try {
            dir.deleteEntry("1");
            fail("Should not be able to delete entry");
        } catch (DirectorySecurityException ee) {
            // ok
        }
        assertNotNull(dir.getEntry("1"));
    }

    @Test
    @WithUser(SUPER_USER)
    public void superUserCanQuery() {
        Map<String, Serializable> filter = new HashMap<>();
        DocumentModelList entries;

        // empty filter means everything (like getEntries)
        entries = dir.query(filter);
        assertNotNull(entries);
        assertEquals(4, entries.size());
    }
}
