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
    protected RepositoryDescriptor newDescriptor(String name,
            long clusteringDelay) {
        RepositoryDescriptor descriptor = super.newDescriptor(name,
                clusteringDelay);
        descriptor.idType = "sequence";
        return descriptor;
    }

}
