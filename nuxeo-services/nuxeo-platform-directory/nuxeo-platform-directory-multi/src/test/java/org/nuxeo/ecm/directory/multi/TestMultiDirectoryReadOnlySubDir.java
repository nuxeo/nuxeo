/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.Arrays;
import java.util.HashSet;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.memory.MemoryDirectory;
import org.nuxeo.ecm.directory.memory.MemoryDirectoryDescriptor;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 9.2
 */
@RunWith(FeaturesRunner.class)
@Features({ MultiDirectoryFeature.class })
@Deploy("org.nuxeo.ecm.directory.multi.tests:directories-readonly-subdir-config.xml")
public class TestMultiDirectoryReadOnlySubDir {

    private static final String MULTI_DIRECTORY_NAME = "multi";

    @Inject
    protected DirectoryService directoryService;

    protected MemoryDirectory memdir1;

    protected MemoryDirectory memdir2;

    protected MemoryDirectoryDescriptor desc1;

    protected MemoryDirectoryDescriptor desc2;

    @Before
    public void setUp() throws Exception {

        // dir 1
        desc1 = new MemoryDirectoryDescriptor();
        desc1.name = "dir1";
        desc1.schemaName = "schema1";
        desc1.schemaSet = new HashSet<>(Arrays.asList("uid", "foo"));
        desc1.idField = "uid";
        desc1.passwordField = "foo";
        directoryService.registerDirectoryDescriptor(desc1);
        memdir1 = (MemoryDirectory) directoryService.getDirectory("dir1");

        // dir 2
        desc2 = new MemoryDirectoryDescriptor();
        desc2.name = "dir2";
        desc2.schemaName = "schema2";
        desc2.schemaSet = new HashSet<>(Arrays.asList("id", "bar"));
        desc2.idField = "id";
        desc2.passwordField = null;
        desc2.readOnly = true;
        directoryService.registerDirectoryDescriptor(desc2);
        memdir2 = (MemoryDirectory) directoryService.getDirectory("dir2");

    }

    @After
    public void tearDown() throws Exception {
        Session session = directoryService.getDirectory(MULTI_DIRECTORY_NAME).getSession();
        if (session != null) {
            session.close();
        }
        directoryService.unregisterDirectoryDescriptor(desc1);
        directoryService.unregisterDirectoryDescriptor(desc2);
    }

    @Test
    public void testDeleteEntryShouldNotFail() {
        try (Session session = directoryService.getDirectory(MULTI_DIRECTORY_NAME).getSession()) {
            // The deletion should not fail because memdir2 is read-only
            session.deleteEntry("no-such-entry");
        }
    }

}
