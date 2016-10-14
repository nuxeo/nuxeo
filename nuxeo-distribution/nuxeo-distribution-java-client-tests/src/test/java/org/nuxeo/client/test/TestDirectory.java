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
 *         Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.client.test;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.client.api.objects.directory.Directory;
import org.nuxeo.client.api.objects.directory.DirectoryEntry;
import org.nuxeo.client.api.objects.directory.DirectoryEntryProperties;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.ecm.restapi.test.RestServerInit;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 0.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@LocalDeploy("org.nuxeo.java.client.test:test-directories-sql-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class TestDirectory extends TestBase {

    @Before
    public void authentication() {
        login();
    }

    @Test
    public void itCanGetDirectory() {
        Directory directory = nuxeoClient.getDirectoryManager().fetchDirectory("continent");
        assertNotNull(directory);
        assertEquals(7, directory.getDirectoryEntries().size());
    }

    @Ignore("JAVACLIENT-41")
    @Test
    public void itCanUpdateDirectory() {
        DirectoryEntry entry = new DirectoryEntry();
        DirectoryEntryProperties directoryEntryProperties = new DirectoryEntryProperties();
        directoryEntryProperties.setId(("test"));
        directoryEntryProperties.setLabel("test");
        directoryEntryProperties.setObsolete(0);
        directoryEntryProperties.setOrdering(0);
        entry.setProperties(directoryEntryProperties);
        DirectoryEntry result = nuxeoClient.getDirectoryManager().createDirectoryEntry("continent", entry);
        assertNotNull(result);
        assertEquals("continent", result.getDirectoryName());
    }

}