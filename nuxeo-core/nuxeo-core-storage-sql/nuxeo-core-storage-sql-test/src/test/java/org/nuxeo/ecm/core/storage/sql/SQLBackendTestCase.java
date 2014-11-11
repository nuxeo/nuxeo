/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.util.Arrays;

import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.FieldDescriptor;
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
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.core.storage.sql");
        if (initDatabase()) {
            DatabaseHelper.DATABASE.setUp();
        }
        repository = newRepository(-1, false);
    }

    protected Repository newRepository(long clusteringDelay,
            boolean fulltextDisabled) throws Exception {
        RepositoryDescriptor descriptor = newDescriptor(clusteringDelay,
                fulltextDisabled);
        RepositoryImpl repo = new RepositoryImpl(descriptor);
        RepositoryResolver.registerTestRepository(repo);
        return repo;
    }

    protected RepositoryDescriptor newDescriptor(long clusteringDelay,
            boolean fulltextDisabled) {
        RepositoryDescriptor descriptor = DatabaseHelper.DATABASE.getRepositoryDescriptor();
        descriptor.clusteringEnabled = clusteringDelay != -1;
        descriptor.clusteringDelay = clusteringDelay;
        FieldDescriptor schemaField = new FieldDescriptor();
        schemaField.field = "tst:bignote";
        schemaField.type = Model.FIELD_TYPE_LARGETEXT;
        descriptor.schemaFields = Arrays.asList(schemaField);
        descriptor.binaryStorePath = "testbinaries";
        descriptor.fulltextDisabled = fulltextDisabled;
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

}
