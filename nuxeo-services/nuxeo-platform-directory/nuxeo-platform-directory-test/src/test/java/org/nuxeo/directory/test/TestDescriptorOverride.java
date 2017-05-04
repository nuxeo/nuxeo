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
package org.nuxeo.directory.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

@RunWith(FeaturesRunner.class)
@Features(DirectoryFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestDescriptorOverride {

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected DirectoryFeature feature;

    @Inject
    protected DirectoryService directoryService;

    @Before
    public void setUp() throws Exception {
        harness.deployContrib(feature.getTestBundleName(), "test-sql-directories-schema-override.xml");
        harness.deployContrib(feature.getTestBundleName(), "test-sql-directories-bundle.xml");
    }

    @Test
    public void testOverride() throws Exception {
        Directory directory = directoryService.getDirectory("userDirectory");
        BaseDirectoryDescriptor config = ((AbstractDirectory) directory).getDescriptor();

        assertEquals("always", config.getCreateTablePolicy());
        assertFalse(config.isAutoincrementIdField());
        Assert.assertNull(config.cacheEntryName);
        Assert.assertNull(config.cacheEntryWithoutReferencesName);
        Assert.assertNull(config.negativeCaching);
        assertEquals("test-users.csv", config.getDataFileName());

        harness.deployContrib(feature.getTestBundleName(), "test-sql-directories-override-bundle.xml");
        try {
            directory = directoryService.getDirectory("userDirectory");
            config = ((AbstractDirectory) directory).getDescriptor();

            // override
            assertEquals("never", config.getCreateTablePolicy());
            assertTrue(config.isAutoincrementIdField());
            Assert.assertEquals("override-entry-cache", config.cacheEntryName);
            Assert.assertEquals("override-entry-cache-wo-ref", config.cacheEntryWithoutReferencesName);
            Assert.assertEquals(Boolean.TRUE, config.negativeCaching);

            // inherit
            assertEquals("test-users.csv", config.getDataFileName());
        } finally {
            harness.undeployContrib(feature.getTestBundleName(), "test-sql-directories-override-bundle.xml");
        }
    }

}
