/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.directory.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

@RunWith(FeaturesRunner.class)
@Features(SQLDirectoryFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@LocalDeploy({ "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-schema-override.xml",
        "org.nuxeo.ecm.directory.sql.tests:test-sql-directories-bundle.xml" })
public class TestDescriptorOverride {

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testOverride() throws Exception {
        SQLDirectory sqlDir = (SQLDirectory) directoryService.getDirectory("userDirectory");
        SQLDirectoryDescriptor config = sqlDir.getDescriptor();

        assertEquals("always", config.getCreateTablePolicy());
        assertEquals(100, config.getQuerySizeLimit());
        assertFalse(config.isAutoincrementIdField());
        assertTrue(config.isComputeMultiTenantId());
        Assert.assertNull(config.cacheEntryName);
        Assert.assertNull(config.cacheEntryWithoutReferencesName);
        Assert.assertNull(config.negativeCaching);
        assertEquals("test-users.csv", config.getDataFileName());

        harness.deployContrib("org.nuxeo.ecm.directory.sql.tests", "test-sql-directories-override-bundle.xml");
        try {
            sqlDir = (SQLDirectory) directoryService.getDirectory("userDirectory");
            config = sqlDir.getDescriptor();

            // override
            assertEquals("never", config.getCreateTablePolicy());
            assertEquals(123, config.getQuerySizeLimit());
            assertTrue(config.isAutoincrementIdField());
            assertFalse(config.isComputeMultiTenantId());
            Assert.assertEquals("override-entry-cache", config.cacheEntryName);
            Assert.assertEquals("override-entry-cache-wo-ref", config.cacheEntryWithoutReferencesName);
            Assert.assertEquals(Boolean.TRUE, config.negativeCaching);

            // inherit
            assertEquals("test-users.csv", config.getDataFileName());
            assertEquals(1, config.getTableReferences().length);
        } finally {
            harness.undeployContrib("org.nuxeo.ecm.directory.sql.tests", "test-sql-directories-override-bundle.xml");
        }
    }

}
