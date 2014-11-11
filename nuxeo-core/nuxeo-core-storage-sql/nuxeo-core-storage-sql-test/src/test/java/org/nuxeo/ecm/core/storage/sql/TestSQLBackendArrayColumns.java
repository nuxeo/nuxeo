/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assume.assumeTrue;

import org.junit.BeforeClass;
import org.junit.Ignore;

/**
 * All the tests of TestSQLBackend with storage of collections as array columns
 * activated.
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
    protected RepositoryDescriptor newDescriptor(long clusteringDelay,
            boolean fulltextDisabled) {
        RepositoryDescriptor descriptor = super.newDescriptor(clusteringDelay,
                fulltextDisabled);
        descriptor.arrayColumns = true;
        return descriptor;
    }

    @Ignore
    @Override
    public void testBasicsUpgrade() {
        // ignored
    }

    @Ignore
    @Override
    public void testVersionsUpgrade() {
        // ignored
    }

    @Ignore
    @Override
    public void testLastContributorUpgrade() {
        // ignored
    }

    @Ignore
    @Override
    public void testLocksUpgrade() {
        // ignored
    }

    // TODO add to TestSQLBackend these tests that exercise arrays:
    // TestSQLRepositoryQuery.testQueryMultiple
    // TestSQLRepositoryQuery.testQueryNegativeMultiple
    // TestSQLRepositoryFulltextQuery.testFulltext (multi-valued field)

}
