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

import static org.junit.Assert.assertNotNull;

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
@Deploy("org.nuxeo.ecm.directory.multi.tests:directories-mem-config.xml")
@Deploy("org.nuxeo.ecm.directory.multi.tests:directories-mem-readonly-config.xml")
public class TestMultiDirectoryReadOnlySubDir {

    private static final String MULTI_DIRECTORY_NAME = "multi";

    @Inject
    protected DirectoryService directoryService;

    protected MemoryDirectory memdir1;

    protected MemoryDirectory memdir2;

    protected MemoryDirectoryDescriptor desc1;

    protected MemoryDirectoryDescriptor desc2;

    @Before
    public void setUp() {
        memdir1 = (MemoryDirectory) directoryService.getDirectory("dir1");
        assertNotNull(memdir1);
        memdir2 = (MemoryDirectory) directoryService.getDirectory("dir2");
        assertNotNull(memdir2);
    }

    @After
    public void tearDown() {
        Session session = directoryService.getDirectory(MULTI_DIRECTORY_NAME).getSession();
        if (session != null) {
            session.close();
        }
    }

    @Test
    public void testDeleteEntryShouldNotFail() {
        try (Session session = directoryService.getDirectory(MULTI_DIRECTORY_NAME).getSession()) {
            // The deletion should not fail because memdir2 is read-only
            session.deleteEntry("no-such-entry");
        }
    }

}
