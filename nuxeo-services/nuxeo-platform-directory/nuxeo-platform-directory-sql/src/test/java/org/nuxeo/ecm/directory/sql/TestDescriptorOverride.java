/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
        SQLDirectoryDescriptor config = sqlDir.getConfig();

        assertEquals("always", config.getCreateTablePolicy());
        assertEquals(100, config.getQuerySizeLimit());
        assertFalse(config.isAutoincrementIdField());
        assertTrue(config.isComputeMultiTenantId());
        Assert.assertNull(config.cacheEntryName);
        Assert.assertNull(config.cacheEntryWithoutReferencesName);
        assertEquals("test-users.csv", config.getDataFileName());

        harness.deployContrib("org.nuxeo.ecm.directory.sql.tests", "test-sql-directories-override-bundle.xml");
        try {
            sqlDir = (SQLDirectory) directoryService.getDirectory("userDirectory");
            config = sqlDir.getConfig();

            // override
            assertEquals("never", config.getCreateTablePolicy());
            assertEquals(123, config.getQuerySizeLimit());
            assertTrue(config.isAutoincrementIdField());
            assertFalse(config.isComputeMultiTenantId());
            Assert.assertEquals("override-entry-cache", config.cacheEntryName);
            Assert.assertEquals("override-entry-cache-wo-ref", config.cacheEntryWithoutReferencesName);

            // inherit
            assertEquals("test-users.csv", config.getDataFileName());
            assertEquals(1, config.getTableReferences().length);
        } finally {
            harness.undeployContrib("org.nuxeo.ecm.directory.sql.tests", "test-sql-directories-override-bundle.xml");
        }
    }

}
