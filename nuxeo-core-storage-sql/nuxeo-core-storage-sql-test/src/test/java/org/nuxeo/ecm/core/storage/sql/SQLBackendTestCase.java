/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.Arrays;

import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.FieldDescriptor;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Florent Guillaume
 */
public abstract class SQLBackendTestCase extends NXRuntimeTestCase {

    public Repository repository;

    public Repository repository2;

    /** Set to false for client unit tests */
    public boolean initDatabase() {
        return true;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.core.storage");
        deployBundle("org.nuxeo.ecm.core.storage.sql");
        if (initDatabase()) {
            DatabaseHelper.DATABASE.setUp();
        }
        repository = newRepository(-1);
    }

    protected Repository newRepository(long clusteringDelay) throws Exception {
        return newRepository(null, clusteringDelay);
    }

    protected Repository newRepository(String name, long clusteringDelay)
            throws Exception {
        RepositoryDescriptor descriptor = newDescriptor(name, clusteringDelay);
        RepositoryImpl repo = new RepositoryImpl(descriptor);
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);
        sqlRepositoryService.registerTestRepository(repo);
        return repo;
    }

    protected RepositoryDescriptor newDescriptor(String name,
            long clusteringDelay) {
        if (name == null) {
            name = DatabaseHelper.DATABASE.repositoryName;
        }
        RepositoryDescriptor descriptor = DatabaseHelper.DATABASE.getRepositoryDescriptor();
        descriptor.name = name;
        descriptor.setClusteringEnabled(clusteringDelay != -1);
        descriptor.setClusteringDelay(clusteringDelay);
        FieldDescriptor schemaField1 = new FieldDescriptor();
        schemaField1.field = "tst:bignote";
        schemaField1.type = Model.FIELD_TYPE_LARGETEXT;
        FieldDescriptor schemaField2 = new FieldDescriptor();
        schemaField2.field = "tst:bignotes";
        schemaField2.type = Model.FIELD_TYPE_LARGETEXT;
        descriptor.schemaFields = Arrays.asList(schemaField1, schemaField2);
        descriptor.binaryStorePath = "testbinaries";
        // disable fulltext because fulltext workers wouldn't have any
        // high-level repository to get a session from anyway.
        descriptor.setFulltextDisabled(true);
        return descriptor;
    }

    @Override
    public void tearDown() throws Exception {
        closeRepository();
        if (initDatabase()) {
            DatabaseHelper.DATABASE.tearDown();
        }
        super.tearDown();
    }

    protected void closeRepository() throws Exception {
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        if (repository != null) {
            repository.close();
            repository = null;
        }
        if (repository2 != null) {
            repository2.close();
            repository2 = null;
        }
    }

    public boolean isSoftDeleteEnabled() {
        return ((RepositoryImpl) repository).getRepositoryDescriptor().getSoftDeleteEnabled();
    }

    public boolean isProxiesEnabled() {
        return ((RepositoryImpl) repository).getRepositoryDescriptor().getProxiesEnabled();
    }

}
