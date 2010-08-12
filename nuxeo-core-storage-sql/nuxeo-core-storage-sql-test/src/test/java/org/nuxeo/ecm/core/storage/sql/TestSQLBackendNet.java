/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.ServerDescriptor;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepository;

/**
 * Tests for NetBackend.
 */
public class TestSQLBackendNet extends TestSQLBackend {

    protected ServerVCS serverVCS;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");

        // config used by server
        deployRepositoryContrib();
        // deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
        // "OSGI-INF/test-repo-core-types-contrib.xml");
        serverVCS = new ServerVCS("test");
        serverVCS.start();
    }

    protected void deployRepositoryContrib() throws Exception {
        if (DatabaseHelper.DATABASE instanceof DatabaseH2) {
            String contrib = "OSGI-INF/test-server-h2-contrib.xml";
            deployContrib("org.nuxeo.ecm.core.storage.sql.test", contrib);
        } else {
            deployContrib("org.nuxeo.ecm.core.storage.sql.test",
                    DatabaseHelper.DATABASE.getDeploymentContrib());
        }
    }

    @Override
    protected RepositoryDescriptor newDescriptor(long clusteringDelay,
            boolean fulltextDisabled) {
        RepositoryDescriptor descriptor = super.newDescriptor(clusteringDelay,
                fulltextDisabled);
        descriptor.name = "client";
        descriptor.binaryStorePath = "clientbinaries";
        descriptor.binaryManagerConnect = true;
        ServerDescriptor sd = new ServerDescriptor();
        sd.host = "localhost";
        sd.port = 8181;
        sd.path = "/nuxeo";
        descriptor.connect = Collections.singletonList(sd);
        return descriptor;
    }

    @Override
    public void tearDown() throws Exception {
        closeRepository();
        serverVCS.interrupt();
        serverVCS.join();
        super.tearDown();
    }

    public static class ServerVCS extends Thread {
        protected final String repositoryName;

        protected Repository repository;

        protected final BlockingQueue<String> methodCall = new LinkedBlockingQueue<String>(
                1);

        protected final BlockingQueue<Object> methodResult = new LinkedBlockingQueue<Object>(
                1);

        public ServerVCS(String repositoryName) {
            super("Nuxeo-VCS-Server");
            this.repositoryName = repositoryName;
        }

        @Override
        public void start() {
            super.start();
            // wait until repository ready
            serverCall("started");
        }

        public Object serverCall(String methodName) {
            try {
                methodCall.put(methodName);
                Object res = methodResult.take();
                if (res instanceof AssertionError) {
                    AssertionError e = (AssertionError) res;
                    throw (AssertionError) new AssertionError("Server Error: "
                            + e).initCause(e);
                }
                return res;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            SQLRepository repo = null;
            try {
                // Looking up the model repository will call
                // SQLRepositoryFactory.createRepository to create a
                // SQLRepository which creates a RepositoryImpl,
                // which is configured to spawn a server to listen for remote
                // connections.
                repo = (SQLRepository) NXCore.getRepositoryService().getRepositoryManager().getRepository(
                        repositoryName);
                // init root
                repo.getSession(null).close();
                repository = repo.repository;
                // now process remote calls, until interrupted by caller
                try {
                    while (true) {
                        String methodName = methodCall.take();
                        Object res;
                        try {
                            res = getClass().getMethod(methodName).invoke(this);
                        } catch (InvocationTargetException e) {
                            res = e.getCause();
                        }
                        methodResult.put(res == null ? Boolean.TRUE : res);
                    }
                } catch (InterruptedException e) {
                    // restore interrupted status
                    interrupt();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (repo != null) {
                    repo.shutdown();
                }
            }
        }

        // called from other thread via reflection: serverCall
        public void started() {
        }

        // ************ testNetInvalidations *************

        private Session session;

        private SimpleProperty title;

        // called from client thread via reflection: serverCall
        public void testNetInvalidations_Init() throws Exception {
            session = repository.getConnection();
            Node root = session.getRootNode();
            Node node = session.getChildNode(root, "foo", false);
            title = node.getSimpleProperty("tst:title");
            assertEquals("before", title.getString());
        }

        // called from client thread via reflection: serverCall
        public void testNetInvalidations_Check() throws Exception {
            // session has not saved (committed) yet, so still unchanged
            assertEquals("before", title.getString());
            session.save();
            // after commit, invalidations have been processed
            assertEquals("after", title.getString());
        }
    }

    public void testNetInvalidations() throws Exception {

        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "foo", null, "TestDoc", false);
        SimpleProperty title = node.getSimpleProperty("tst:title");
        title.setValue("before");
        session.save();
        assertEquals("before", title.getString());

        serverVCS.serverCall("testNetInvalidations_Init");

        // change title
        title.setValue("after");
        // save session and queue its invalidations to others
        // note that to be correct this has to also send the invalidations
        // server-side
        session.save();
        assertEquals("after", title.getString());

        serverVCS.serverCall("testNetInvalidations_Check");
    }

}
