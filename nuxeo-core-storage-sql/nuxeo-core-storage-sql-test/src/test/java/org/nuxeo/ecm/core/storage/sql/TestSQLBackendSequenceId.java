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
 * All the tests of TestSQLBackend with sequence-based ids.
 */
public class TestSQLBackendSequenceId extends TestSQLBackend {

    /**
     * Only run for databases that support sequence ids.
     */
    @BeforeClass
    public static void assumeSoftDeleteSupported() {
        assumeTrue(DatabaseHelper.DATABASE.supportsSequenceId());
    }

    @Override
    protected RepositoryDescriptor newDescriptor(long clusteringDelay,
            boolean fulltextDisabled) {
        RepositoryDescriptor descriptor = super.newDescriptor(clusteringDelay,
                fulltextDisabled);
        descriptor.idType = "sequence";
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

}
