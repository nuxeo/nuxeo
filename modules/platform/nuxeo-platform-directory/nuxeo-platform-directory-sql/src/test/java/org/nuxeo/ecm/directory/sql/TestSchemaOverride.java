/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.directory.sql;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.CREATE_TABLE_POLICY_ON_MISSING_COLUMNS;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;

@RunWith(FeaturesRunner.class)
@Features(SQLDirectoryFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.directory.sql.tests:test-basic-types-directories-bundle.xml")
public class TestSchemaOverride {

    @Inject
    protected HotDeployer deployer;

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testSchemaOverrideWhenPolicyIsOnMissingColumn() throws Exception {
        var descriptor = directoryService.getDirectoryDescriptor("userDirectory");
        assertEquals(CREATE_TABLE_POLICY_ON_MISSING_COLUMNS, descriptor.getCreateTablePolicy());

        // check that the directory is correctly initialized
        var directory = directoryService.getDirectory("userDirectory");
        var entries = directory.getSession().query(new QueryBuilder(), false);
        assertEquals(3, entries.size());

        // deploy the user schema override
        deployer.deploy("org.nuxeo.ecm.directory.sql.tests:test-sql-directories-schema-override.xml");

        // check that we can still query the directory
        directory = directoryService.getDirectory("userDirectory");
        entries = directory.getSession().query(new QueryBuilder(), false);
        assertEquals(3, entries.size());
    }
}
