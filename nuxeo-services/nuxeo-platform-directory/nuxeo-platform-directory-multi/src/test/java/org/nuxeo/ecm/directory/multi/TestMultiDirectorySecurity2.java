/*
 * (C) Copyright 2017-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.directory.multi;

import static org.junit.Assert.assertNotNull;

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
import org.nuxeo.ecm.directory.PermissionDescriptor;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.memory.MemoryDirectory;
import org.nuxeo.ecm.directory.memory.MemoryDirectoryDescriptor;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ MultiDirectoryFeature.class })
@Deploy("org.nuxeo.ecm.directory.multi.tests:directories-security-config.xml")
public class TestMultiDirectorySecurity2 {

    DirectoryService directoryService;

    MemoryDirectory memdir1;

    MemoryDirectory memdir2;

    MemoryDirectory memdir3;

    MultiDirectorySession dirGroup;

    @Inject
    ClientLoginFeature dummyLogin;

    protected MemoryDirectoryDescriptor desc1;

    protected MemoryDirectoryDescriptor desc2;

    protected MemoryDirectoryDescriptor desc3;

    public static final String EVERYONE_GROUP = "Everyone";

    @Before
    public void setUp() throws Exception {
        // mem dir factory
        directoryService = Framework.getService(DirectoryService.class);

        // create and register mem directories
        Map<String, Object> e;

        PermissionDescriptor perm = new PermissionDescriptor();
        perm.name = "Write";
        perm.groups = new String[] { EVERYONE_GROUP };
        PermissionDescriptor[] permissions = new PermissionDescriptor[] { perm };

        // dir 1
        desc1 = new MemoryDirectoryDescriptor();
        desc1.name = "dir1";
        desc1.schemaName = "schema1";
        desc1.schemaSet = new HashSet<>(Arrays.asList("uid", "password", "foo"));
        desc1.idField = "uid";
        desc1.passwordField = "password";
        desc1.permissions = permissions;
        directoryService.registerDirectoryDescriptor(desc1);
        memdir1 = (MemoryDirectory) directoryService.getDirectory("dir1");

        try (Session dir1 = memdir1.getSession()) {
            e = new HashMap<>();
            e.put("uid", "1");
            e.put("password", "pw1");
            e.put("foo", "foo1");
            dir1.createEntry(e);
            e = new HashMap<>();
            e.put("uid", "2");
            e.put("password", "pw2");
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
        desc3.schemaSet = new HashSet<>(Arrays.asList("uid", "thepass", "thefoo", "thebar"));
        desc3.idField = "uid";
        desc3.passwordField = "thepass";
        desc3.permissions = permissions;
        directoryService.registerDirectoryDescriptor(desc3);
        memdir3 = (MemoryDirectory) directoryService.getDirectory("dir3");

        try (Session dir3 = memdir3.getSession()) {
            e = new HashMap<>();
            e.put("uid", "3");
            e.put("thepass", "pw3");
            e.put("thefoo", "foo3");
            e.put("thebar", "bar3");
            dir3.createEntry(e);
            e = new HashMap<>();
            e.put("uid", "4");
            e.put("thepass", "pw4");
            e.put("thefoo", "foo4");
            e.put("thebar", "bar4");
            dir3.createEntry(e);
        }

        dirGroup = (MultiDirectorySession) directoryService.getDirectory("multi-group").getSession();
    }

    @After
    public void tearDown() {
        directoryService = Framework.getService(DirectoryService.class);
        directoryService.unregisterDirectoryDescriptor(desc1);
        directoryService.unregisterDirectoryDescriptor(desc2);
        directoryService.unregisterDirectoryDescriptor(desc3);
    }

    @Test
    public void everyoneUserCanCreateAndGet() throws Exception {
        // Given a user in the everyone group
        // (default in dummy login any user is member of everyone)
        dummyLogin.login("anEveryoneUser");

        Map<String, Object> map = new HashMap<>();
        map.put("uid", "5");
        map.put("thefoo", "foo5");
        map.put("thebar", "bar5");

        // When I call the multi-group dir
        DocumentModel entry = dirGroup.createEntry(map);
        assertNotNull(entry);

        // I can create and then get entry
        entry = dirGroup.getEntry("5");
        assertNotNull(entry);

        dummyLogin.logout();
    }

}
