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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManagerComponent;
import org.nuxeo.ecm.core.blob.BlobProviderDescriptor;
import org.nuxeo.ecm.core.blob.binary.DefaultBinaryManager;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.FieldDescriptor;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author Florent Guillaume
 */
@RunWith(FeaturesRunner.class)
@Features(SQLBackendFeature.class)
public abstract class SQLBackendTestCase {

    private static final String REPOSITORY_NAME = "test";

    private final Map<String, BlobProviderDescriptor> blobProviderDescriptors = new HashMap<>();

    protected RepositoryImpl repository;

    protected Serializable[] rootAcl;

    @Before
    public void setUp() throws Exception {
        repository = newRepository(-1);
        SessionImpl session = repository.getConnection();
        rootAcl = session.getRootNode().getCollectionProperty(Model.ACL_PROP).getValue();
        session.close();
    }

    protected RepositoryImpl newRepository(long clusteringDelay) {
        return newRepository(null, clusteringDelay);
    }

    protected RepositoryImpl newRepository(String name, long clusteringDelay) {
        RepositoryDescriptor descriptor = newDescriptor(name, clusteringDelay);
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);
        sqlRepositoryService.registerContribution(descriptor, "repository", null);
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        repositoryService.start(null);
        newBlobProvider(descriptor.name);
        return sqlRepositoryService.getRepositoryImpl(descriptor.name);
    }

    protected RepositoryDescriptor newDescriptor(String name, long clusteringDelay) {
        if (name == null) {
            name = REPOSITORY_NAME;
        }
        RepositoryDescriptor descriptor = DatabaseHelper.DATABASE.getRepositoryDescriptor();
        descriptor.name = name;
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

    @After
    public void tearDown() throws Exception {
        closeRepository();
    }

    protected void closeRepository() {
        Framework.getService(EventService.class).waitForAsyncCompletion();
        BlobManagerComponent blobManager = (BlobManagerComponent) Framework.getService(BlobManager.class);
        for (BlobProviderDescriptor blobProviderDescriptor : blobProviderDescriptors.values()) {
            blobManager.unregisterBlobProvider(blobProviderDescriptor);
        }
        clearAndClose(repository);
    }

    protected void clearAndClose(RepositoryImpl repo) {
        if (repo != null) {
            SessionImpl session = repo.getConnection();
            remove(session, "SELECT ecm:uuid FROM Document WHERE ecm:isProxy = 1");
            remove(session, "SELECT ecm:uuid FROM Relation, Document");
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, 60);
            session.cleanupDeletedDocuments(0, calendar);
            session.getRootNode().getCollectionProperty(Model.ACL_PROP).setValue(rootAcl);
            session.save();
            repo.close();
        }
    }

    protected void remove(Session session, String query) {
        PartialList<Serializable> results = session.query(query, QueryFilter.EMPTY, true);
        for (Serializable result : results) {
            Node node = session.getNodeById(result);
            if (node != null) {
                session.removeNode(node);
            }
        }
    }

    public boolean isSoftDeleteEnabled() {
        return repository.getRepositoryDescriptor().getSoftDeleteEnabled();
    }

    public boolean isProxiesEnabled() {
        return repository.getRepositoryDescriptor().getProxiesEnabled();
    }

}
