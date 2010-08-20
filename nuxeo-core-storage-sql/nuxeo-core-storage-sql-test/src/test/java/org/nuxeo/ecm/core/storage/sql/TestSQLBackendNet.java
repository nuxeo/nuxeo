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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.storage.EventConstants;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.ServerDescriptor;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepository;
import org.nuxeo.ecm.core.storage.sql.listeners.DummyTestListener;

/**
 * Tests for NetBackend.
 */
public class TestSQLBackendNet extends TestSQLBackend {

    protected ServerVCS serverVCS;

    public static List<Event> EVENTS = DummyTestListener.EVENTS_RECEIVED;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
                "OSGI-INF/test-listeners-invalidations-contrib.xml");
        deployRepositoryContrib();
        serverVCS = new ServerVCS("test");
        serverVCS.start();
    }

    // config used by server
    protected void deployRepositoryContrib() throws Exception {
        if (DatabaseHelper.DATABASE instanceof DatabaseH2) {
            String contrib = "OSGI-INF/test-server-h2-contrib.xml";
            deployContrib("org.nuxeo.ecm.core.storage.sql.test", contrib);
        } else {
            deployContrib("org.nuxeo.ecm.core.storage.sql.test",
                    DatabaseHelper.DATABASE.getDeploymentContrib());
        }
    }

    // descriptor used by client
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
        descriptor.sendInvalidationEvents = true;
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

        private Node node;

        // called from client thread via reflection: serverCall
        public void testNetInvalidations_Init() throws Exception {
            session = repository.getConnection();
            Node root = session.getRootNode();
            node = session.getChildNode(root, "foo", false);
            title = node.getSimpleProperty("tst:title");
            assertEquals("before", title.getString());
        }

        // called from client thread via reflection: serverCall
        public void testNetInvalidations_Check() throws Exception {
            // session has not saved (committed) yet, so still unchanged
            assertEquals("before", title.getString());
            assertEquals(1, EVENTS.size());
            session.save();
            // after save, remote invalidations have been processed
            assertEquals("after", title.getString());
            assertEquals(2, EVENTS.size());
            // remote inval from client to server
            checkEvent(EVENTS.get(1), node.getId(), false);
        }

        // called from client thread via reflection: serverCall
        public void testNetInvalidations_Change() throws Exception {
            title.setValue("new");
            assertEquals(2, EVENTS.size());
            session.save();
            assertEquals(3, EVENTS.size());
            checkEvent(EVENTS.get(2), node.getId(), true); // server self inval
        }
    }

    public void testClientServerInvalidations() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "foo", null, "TestDoc", false);
        SimpleProperty title = node.getSimpleProperty("tst:title");
        title.setValue("before");
        session.save();
        assertEquals("before", title.getString());

        EVENTS.clear();

        serverVCS.serverCall("testNetInvalidations_Init");

        // change title
        title.setValue("after");
        assertEquals(0, EVENTS.size());
        // save session and queue its invalidations to others
        // note that to be correct this has to also send the invalidations
        // server-side
        session.save();
        assertEquals("after", title.getString());
        // events received
        assertEquals(1, EVENTS.size());
        // repo 1 self inval
        checkEvent(EVENTS.get(0), node.getId(), true);

        serverVCS.serverCall("testNetInvalidations_Check");

        // now change prop on the server
        serverVCS.serverCall("testNetInvalidations_Change");

        // check visible change after save
        assertEquals("after", title.getString());
        assertEquals(3, EVENTS.size());
        session.save();
        assertEquals("new", title.getString());
        assertEquals(4, EVENTS.size());
        // remote inval from server to client
        checkEvent(EVENTS.get(3), node.getId(), false);
    }

    // this test is similar to clustering tests
    public void testTwoClientsInvalidations() throws Exception {
        repository2 = newRepository(-1, false);

        Session session1 = repository.getConnection();

        Node root1 = session1.getRootNode();
        Node node1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        SimpleProperty title1 = node1.getSimpleProperty("tst:title");
        title1.setValue("before");
        session1.save();
        assertEquals("before", title1.getString());

        // check session 2 has before state
        Session session2 = repository2.getConnection();
        Node root2 = session2.getRootNode();
        Node node2 = session2.getChildNode(root2, "foo", false);
        SimpleProperty title2 = node2.getSimpleProperty("tst:title");
        assertEquals("before", title2.getString());

        EVENTS.clear();

        // change title in session 1
        title1.setValue("after");
        assertEquals(0, EVENTS.size());
        // save session and queue its invalidations to others
        // note that to be correct this has to also send the invalidations
        // server-side
        session1.save();
        assertEquals("after", title1.getString());
        assertEquals(1, EVENTS.size());
        // repo 1 self inval
        checkEvent(EVENTS.get(0), node1.getId(), true);

        // session2 has not saved (committed) yet, so still unchanged
        assertEquals("before", title2.getString());
        assertEquals(1, EVENTS.size());
        session2.save();
        // after commit/save, invalidations have been processed
        assertEquals("after", title2.getString());
        // and event sent from repo 1 to repo 2
        assertEquals(2, EVENTS.size());
        checkEvent(EVENTS.get(1), node1.getId(), false);
    }

    protected static void checkEvent(Event event, Serializable id, boolean local) {
        assertEquals(EventConstants.EVENT_VCS_INVALIDATIONS, event.getName());
        EventContext ctx = event.getContext();
        @SuppressWarnings("unchecked")
        Set<String> set = (Set<String>) ctx.getProperty(EventConstants.INVAL_MODIFIED_DOC_IDS);
        assertEquals(Collections.singleton(id), set);
        Boolean loc = (Boolean) ctx.getProperty(EventConstants.INVAL_LOCAL);
        assertEquals(Boolean.valueOf(local), loc);
    }

}
