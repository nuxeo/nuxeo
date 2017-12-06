/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManagerComponent;
import org.nuxeo.ecm.core.blob.BlobProviderDescriptor;
import org.nuxeo.ecm.core.blob.binary.DefaultBinaryManager;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.FieldDescriptor;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Florent Guillaume
 */
public abstract class SQLBackendTestCase extends NXRuntimeTestCase {

    private static final String REPOSITORY_NAME = "test";

    private Map<String, BlobProviderDescriptor> blobProviderDescriptors = new HashMap<>();

    public Repository repository;

    public Repository repository2;

    @Override
    public void setUp() throws Exception {
        deployBundle("org.nuxeo.runtime.jtajca");
        deployBundle("org.nuxeo.runtime.datasource");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.core.storage");
        deployBundle("org.nuxeo.ecm.core.storage.sql");
        deployBundle("org.nuxeo.ecm.platform.el");
        DatabaseHelper.DATABASE.setUp();
        deployTestContrib("org.nuxeo.ecm.core.storage", "OSGI-INF/test-repo-ds.xml");
    }

    @Override
    protected void postSetUp() throws Exception {
	repository = newRepository(-1);
    }

    protected Repository newRepository(long clusteringDelay) throws Exception {
        return newRepository(null, clusteringDelay);
    }

    protected Repository newRepository(String name, long clusteringDelay) throws Exception {
        RepositoryDescriptor descriptor = newDescriptor(name, clusteringDelay);
        RepositoryImpl repo = new RepositoryImpl(descriptor);
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);
        sqlRepositoryService.registerTestRepository(repo);
        newBlobProvider(descriptor.name);
        return repo;
    }

    protected RepositoryDescriptor newDescriptor(String name, long clusteringDelay) {
        if (name == null) {
            name = REPOSITORY_NAME;
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
        // disable fulltext because fulltext workers wouldn't have any
        // high-level repository to get a session from anyway.
        descriptor.setFulltextDisabled(true);
        return descriptor;
    }

    protected void newBlobProvider(String name) {
        BlobProviderDescriptor blobProviderDescriptor = newBlobProviderDescriptor(name);
        BlobManagerComponent blobManager = (BlobManagerComponent) Framework.getService(BlobManager.class);
        blobManager.registerBlobProvider(blobProviderDescriptor);
        blobProviderDescriptors.put(name, blobProviderDescriptor);
    }

    protected BlobProviderDescriptor newBlobProviderDescriptor(String name) {
        BlobProviderDescriptor descr = new BlobProviderDescriptor();
        descr.name = name;
        descr.klass = DefaultBinaryManager.class;
        return descr;
    }

    @Override
    public void tearDown() throws Exception {
        closeRepository();
    }

    protected void closeRepository() throws Exception {
        Framework.getService(EventService.class).waitForAsyncCompletion();
        BlobManagerComponent blobManager = (BlobManagerComponent) Framework.getService(BlobManager.class);
        for (BlobProviderDescriptor blobProviderDescriptor : blobProviderDescriptors.values()) {
            blobManager.unregisterBlobProvider(blobProviderDescriptor);
        }
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
