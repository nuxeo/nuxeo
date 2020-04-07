/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.runtime.cluster.ClusterService;
import org.nuxeo.runtime.cluster.ClusterServiceImpl;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * Runs tests with cluster mode activated.
 */
@Deploy("org.nuxeo.ecm.core.storage.sql.test.tests:OSGI-INF/test-backend-core-types-contrib.xml")
@Deploy("org.nuxeo.ecm.core.storage.sql.test.tests:OSGI-INF/test-cluster.xml") // node "1"
public class TestSQLBackendClustering extends SQLBackendTestCase {

    @Inject
    protected ClusterService clusterService;

    @Before
    public void checkSupported() {
        assumeTrue(DatabaseHelper.DATABASE.supportsClustering());
    }

    @After
    public void resetClusterNodeId() {
        setNodeId("1");
    }

    protected void setNodeId(String nodeId) {
        ((ClusterServiceImpl) clusterService).setNodeId(nodeId);
    }

    @Test
    public void testClustering() throws Exception {

        // get two clustered repositories
        repository.close();
        repository = newRepository(); // node 1 from XML config
        setNodeId("2");
        RepositoryImpl repository2 = newRepository();

        ClusterTestJob r1 = new ClusterTestJob(repository, repository2);
        ClusterTestJob r2 = new ClusterTestJob(repository, repository2);
        LockStepJob.run(r1, r2);

        clearAndClose(repository2);
    }

    protected static class ClusterTestJob extends LockStepJob {

        protected Repository repository1;

        protected Repository repository2;

        private static final long DELAY = 500; // ms

        public ClusterTestJob(Repository repository1, Repository repository2) {
            this.repository1 = repository1;
            this.repository2 = repository2;
        }

        @Override
        public void job() throws Exception {
            Session session1 = null;
            Session session2 = null;
            Node folder1 = null;
            Node folder2 = null;
            SimpleProperty title1 = null;
            SimpleProperty title2 = null;
            if (thread(1)) {
                // session1 creates root node and does a save
                // which resets invalidation timeout
                session1 = repository1.getConnection();
            }
            if (thread(2)) {
                session2 = repository2.getConnection();
                session2.save(); // save resets invalidations timeout
            }
            if (thread(1)) {
                // in session1, create base folder
                Node root1 = session1.getRootNode();
                folder1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
                title1 = folder1.getSimpleProperty("tst:title");
                session1.save();
            }
            if (thread(2)) {
                // in session2, retrieve folder and check children
                Node root2 = session2.getRootNode();
                folder2 = session2.getChildNode(root2, "foo", false);
                assertNotNull(folder2);
                title2 = folder2.getSimpleProperty("tst:title");
                session2.getChildren(folder2, null, false);
            }
            if (thread(1)) {
                // in session1, add document
                session1.addChildNode(folder1, "gee", null, "TestDoc", false);
                session1.save();
            }
            if (thread(2)) {
                // in session2, try to get document
                // immediate check, invalidation delay means not done yet
                session2.save();
                Node doc2 = session2.getChildNode(folder2, "gee", false);
                // assertNull(doc2); // could fail if machine very slow
                Thread.sleep(DELAY + 1); // wait invalidation delay
                session2.save(); // process invalidations (non-transactional)
                doc2 = session2.getChildNode(folder2, "gee", false);
                assertNotNull(doc2);
            }
            if (thread(1)) {
                // in session1 change title
                title1.setValue("yo");
            }
            if (thread(2)) {
                assertNull(title2.getString());
            }
            if (thread(1)) {
                // save session1 (queues its invalidations to others)
                session1.save();
            }
            if (thread(2)) {
                // session2 has not saved (committed) yet, so still unmodified
                assertNull(title2.getString());
                // immediate check, invalidation delay means not done yet
                session2.save();
                // assertNull(title2.getString()); // could fail if machine very slow
                Thread.sleep(DELAY + 1); // wait invalidation delay
                session2.save();
                // after commit, invalidations have been processed
                assertEquals("yo", title2.getString());
            }
            if (thread(1)) {
                // written properties aren't shared
                title1.setValue("mama");
            }
            if (thread(2)) {
                title2.setValue("glop");
            }
            if (thread(1)) {
                session1.save();
                assertEquals("mama", title1.getString());
            }
            if (thread(2)) {
                assertEquals("glop", title2.getString());
                session2.save(); // and notifies invalidations
            }
            if (thread(1)) {
                // in non-transaction mode, session1 has not processed
                // its invalidations yet, call save() to process them artificially
                Thread.sleep(DELAY + 1); // wait invalidation delay
                session1.save();
                // session2 save wins
                assertEquals("glop", title1.getString());
            }
            if (thread(2)) {
                assertEquals("glop", title2.getString());
            }
        }

    }

    @Test
    public void testLockingParallelClustered() throws Throwable {
        Serializable nodeId = TestSQLBackend.createNode(repository);

        // get two clustered repositories
        repository.close();
        repository = newRepository();
        setNodeId("2");
        Repository repository2 = newRepository();

        TestSQLBackend.runParallelLocking(nodeId, repository, repository2);
    }

    @Test
    public void testClusterInvalidationsPropagatorLeak() throws Exception {
        List<?> queues = repository.invalidationsPropagator.queues;
        assertEquals(0, queues.size());
        Session session = repository.getConnection();
        assertEquals(1, queues.size());
        session.close();
        assertEquals(0, queues.size());
    }

}
