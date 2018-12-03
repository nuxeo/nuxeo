/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.directory.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.login.test.ClientLoginFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, ClientLoginFeature.class })
@Deploy("org.nuxeo.ecm.directory.api")
@Deploy("org.nuxeo.ecm.directory")
@Deploy("org.nuxeo.ecm.directory.core.tests:core/types-config.xml")
@Deploy("org.nuxeo.ecm.directory.core.tests:core/directory-data-config.xml")
public class TestCoreDirectoryDataFile {

    @Inject
    protected DirectoryService directoryService;

    protected Session session;

    @Before
    public void setUp() throws Exception {
        Directory dir = directoryService.getDirectory("coreDirWithData");
        session = dir.getSession();
    }

    @After
    public void tearDown() {
        session.close();
    }

    @Test
    public void testGetEntry() {
        Framework.doPrivileged(() -> {
            DocumentModel entry;
            // missing entry
            entry = session.getEntry("no-such-entry");
            assertNull(entry);
            // this one was created by reading the data file
            entry = session.getEntry("blauid");
            assertNotNull(entry);
            assertEquals("blafoo", entry.getPropertyValue("foo"));
            assertEquals("blabar", entry.getPropertyValue("bar"));
        });
    }

}
