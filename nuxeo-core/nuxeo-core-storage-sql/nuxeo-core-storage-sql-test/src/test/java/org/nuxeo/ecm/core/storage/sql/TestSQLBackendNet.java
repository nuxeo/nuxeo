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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.apache.log4j.lf5.util.StreamUtils;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.storage.EventConstants;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.ServerDescriptor;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepository;
import org.nuxeo.ecm.core.storage.sql.listeners.DummyTestListener;

/**
 * Tests for NetBackend.
 */
public class TestSQLBackendNet extends TestSQLBackend {

    // defined in repo XML config as well
    private static final String SERVER_REPO_NAME = "test";

    private static final String CLIENT_REPO_NAME = "client";

    private static final String CLIENT_REPO_NAME_2 = CLIENT_REPO_NAME + "2";

    public static List<Event> EVENTS = DummyTestListener.EVENTS_RECEIVED;

    protected String repoName;

    protected ServerVCS serverVCS;

    @Before
    public void setUp() throws Exception {
        repoName = CLIENT_REPO_NAME;
        super.setUp();
        deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests", "OSGI-INF/test-listeners-invalidations-contrib.xml");
        deployRepositoryContrib();
        serverVCS = new ServerVCS(SERVER_REPO_NAME);
        serverVCS.start();
    }

    // config used by server
    protected void deployRepositoryContrib() throws Exception {
        if (DatabaseHelper.DATABASE instanceof DatabaseH2) {
            String contrib = "OSGI-INF/test-server-h2-contrib.xml";
            deployContrib("org.nuxeo.ecm.core.storage.sql.test", contrib);
        } else if (DatabaseHelper.DATABASE instanceof DatabasePostgreSQL) {
            String contrib = "OSGI-INF/test-server-postgresql-contrib.xml";
            deployContrib("org.nuxeo.ecm.core.storage.sql.test", contrib);
        } else {
            deployContrib("org.nuxeo.ecm.core.storage.sql.test", DatabaseHelper.DATABASE.getDeploymentContrib());
        }
    }

    // descriptor used by client
    @Override
    protected RepositoryDescriptor newDescriptor(long clusteringDelay, boolean fulltextDisabled) {
        RepositoryDescriptor descriptor = super.newDescriptor(clusteringDelay, fulltextDisabled);
        descriptor.name = repoName;
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

    @After
    public void tearDown() throws Exception {
        closeRepository();
        serverVCS.interrupt();
        serverVCS.join();
        super.tearDown();
    }

    public static class ServerVCS extends Thread {

        protected final String repositoryName;

        protected Repository repository;

        protected final BlockingQueue<String> methodCall = new LinkedBlockingQueue<String>(1);

        protected final BlockingQueue<Object> methodResult = new LinkedBlockingQueue<Object>(1);

        public ServerVCS(String repositoryName) {
            super("Nuxeo-VCS-Server-Test");
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
                    throw (AssertionError) new AssertionError("Server Error: " + e).initCause(e);
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
                repo = (SQLRepository) NXCore.getRepositoryService().getRepositoryManager().getRepository(repositoryName);
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
            // remote inval from client to server
            checkEvent(1, false, SERVER_REPO_NAME, node.getId());
        }

        // called from client thread via reflection: serverCall
        public void testNetInvalidations_Change() throws Exception {
            title.setValue("new");
            assertEquals(2, EVENTS.size());

            session.save();
            // server self inval
            checkEvent(2, true, SERVER_REPO_NAME, node.getId());
        }
    }

    @Test
    public void testClientServerInvalidations() throws Exception {
        Session session = repository.getConnection();
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "foo", null, "TestDoc", false);
        Serializable nodeId = node.getId();
        SimpleProperty title = node.getSimpleProperty("tst:title");
        title.setValue("before");
        session.save();
        assertEquals("before", title.getString());

        DummyTestListener.clear();

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
        // repo 1 self inval
        checkEvent(0, true, CLIENT_REPO_NAME, nodeId);
        serverVCS.serverCall("testNetInvalidations_Check");
        // now change prop on the server
        serverVCS.serverCall("testNetInvalidations_Change");
        // check visible change after save
        assertEquals("after", title.getString());

        session.save();
        assertEquals("new", title.getString());

        // remote inval from server to client
        checkEvent(3, false, CLIENT_REPO_NAME, nodeId);
    }

    // this test is similar to clustering tests
    @Test
    public void testTwoClientsInvalidations() throws Exception {
        repoName = CLIENT_REPO_NAME_2;
        repository2 = newRepository(-1, false);

        Session session1 = repository.getConnection();

        Node root1 = session1.getRootNode();
        Node node1 = session1.addChildNode(root1, "foo", null, "TestDoc", false);
        Serializable nodeId = node1.getId();
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

        DummyTestListener.clear();

        // change title in session 1
        title1.setValue("after");
        // save session and queue its invalidations to others
        // note that to be correct this has to also send the invalidations
        // server-side
        session1.save();
        assertEquals("after", title1.getString());

        // repo 1 self inval
        checkEvent(0, true, CLIENT_REPO_NAME, nodeId);
        // session2 has not saved (committed) yet, so still unchanged
        assertEquals("before", title2.getString());

        session2.save();
        // after commit/save, invalidations have been processed
        assertEquals("after", title2.getString());

        // and event sent from repo 1 to repo 2
        checkEvent(1, false, CLIENT_REPO_NAME_2, nodeId);
    }

    protected SimpleProperty getBlob(Session session) throws StorageException, IOException {
        Node root = session.getRootNode();
        Node node = session.addChildNode(root, "pff", null, "content", false);
        SimpleProperty prop = node.getSimpleProperty("data");
        assertNotNull(prop);
        return prop;
    }

    @Test
    public void testSerializeRepoBinaries() throws Exception {
        BinaryManager binMgr = ((RepositoryImpl) repository).binaryManager;
        StringBlob blob = new StringBlob("dummy");
        Binary data = binMgr.getBinary(blob.getStream());
        checkSerialization(data);
    }

    protected void checkSerialization(Binary data) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(data);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        Binary unmarshalled = (Binary) ois.readObject();
        String unmarshalledString = new String(StreamUtils.getBytes(unmarshalled.getStream()));
        String originalString = new String(StreamUtils.getBytes(data.getStream()));
        assertEquals(unmarshalledString, originalString);
    }

    @Test
    public void testSerializeDisconnectedBinaries() throws Exception {
        File file = File.createTempFile("nuxeo-test-", ".blob");
        file.deleteOnExit();
        Binary data = new Binary(file, "abc");
        checkSerialization(data);
    }

    protected static void checkEvent(int i, boolean local, String repo, Serializable id) {
        assertTrue("size=" + EVENTS.size() + ", i=" + i, i < EVENTS.size());
        assertEquals(i, EVENTS.size() - 1);

        Event event = EVENTS.get(i);
        assertEquals(EventConstants.EVENT_VCS_INVALIDATIONS, event.getName());

        EventContext ctx = event.getContext();
        assertEquals(repo, ctx.getRepositoryName());

        @SuppressWarnings("unchecked")
        Set<String> set = (Set<String>) ctx.getProperty(EventConstants.INVAL_MODIFIED_DOC_IDS);
        assertEquals(Collections.singleton(id), set);
// NXP-5808 cannot distinguish local/cluster invalidations
//        Boolean loc = (Boolean) ctx.getProperty(EventConstants.INVAL_LOCAL);
//        assertEquals(Boolean.valueOf(local), loc);
    }

}
