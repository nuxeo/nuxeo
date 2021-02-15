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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.local.WithUser;
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
@Deploy("org.nuxeo.ecm.directory.multi.tests:directories-mem-perm-config2.xml")
public class TestMultiDirectorySecurity2 {

    @Inject
    protected DirectoryService directoryService;

    MemoryDirectory memdir1;

    MemoryDirectory memdir2;

    MemoryDirectory memdir3;

    MultiDirectorySession dirGroup;

    protected MemoryDirectoryDescriptor desc1;

    protected MemoryDirectoryDescriptor desc2;

    protected MemoryDirectoryDescriptor desc3;

    public static final String EVERYONE_GROUP = "Everyone";

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

        dirGroup = (MultiDirectorySession) directoryService.getDirectory("multi-group").getSession();
        assertNotNull(dirGroup);
    }

    @After
    public void tearDown() {
        TestDirectoryHelper.clearDirectory(memdir1);
        TestDirectoryHelper.clearDirectory(memdir2);
        TestDirectoryHelper.clearDirectory(memdir3);
    }

    @Test
    @WithUser("anEveryoneUser")
    public void everyoneUserCanCreateAndGet() {
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
    }

}
