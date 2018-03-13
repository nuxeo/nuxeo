/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assume.assumeTrue;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * All the tests of TestSQLBackend with storage of collections as array columns activated.
 */
public class TestSQLBackendArrayColumns extends TestSQLBackend {

    /**
     * Only run for databases that support array columns.
     */
    @BeforeClass
    public static void assumeArrayColumnsSupported() {
        assumeTrue(DatabaseHelper.DATABASE.supportsArrayColumns());
    }

    @Override
    protected RepositoryDescriptor newDescriptor(String name, long clusteringDelay) {
        RepositoryDescriptor descriptor = super.newDescriptor(name, clusteringDelay);
        descriptor.setArrayColumns(true);
        return descriptor;
    }

    @Override
    protected boolean useArrayColumns() {
        return DatabaseHelper.DATABASE.supportsArrayColumns();
    }

    @Override
    @Test
    @Ignore("NXP-19542")
    public void testParallelArrayUpdate() throws Exception {
        super.testParallelArrayUpdate();
    }

    // TODO add to TestSQLBackend these tests that exercise arrays:
    // TestSQLRepositoryQuery.testQueryMultiple
    // TestSQLRepositoryQuery.testQueryNegativeMultiple
    // TestSQLRepositoryFulltextQuery.testFulltext (multi-valued field)

}
