/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.ra;

import java.io.File;

import javax.naming.InitialContext;
import javax.transaction.TransactionManager;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.embedded.junit.BaseTestCase;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.security.SecurityManager;
import org.nuxeo.ecm.core.storage.sql.Node;
import org.nuxeo.ecm.core.storage.sql.Repository;
import org.nuxeo.ecm.core.storage.sql.Session;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Florent Guillaume
 */
public class ResourceAdapterTestCase extends BaseTestCase {

    private static final Log log = LogFactory.getLog(ResourceAdapterTestCase.class);

    private final PublicNXRuntimeTestCase runtimeTestCase;

    public static Test suite() {
        return preProcessedTest(ResourceAdapterTestCase.class);
    }

    public static class PublicNXRuntimeTestCase extends NXRuntimeTestCase {

        @Override
        public void setUp() throws Exception {
            super.setUp();
        }

        @Override
        public void tearDown() throws Exception {
            super.tearDown();
        }
    }

    public ResourceAdapterTestCase() {
        super(ResourceAdapterTestCase.class.getSimpleName());
        runtimeTestCase = new PublicNXRuntimeTestCase();
    }

    @Override
    public void setUp() throws Exception {
        // path from src/test/resources/deploy/nuxeo-sql-ds.xml
        deleteRecursive(new File("target/test/repository"));

        super.setUp();
        runtimeTestCase.setUp();
        runtimeTestCase.deployBundle("org.nuxeo.ecm.core.schema");
        runtimeTestCase.deployBundle("org.nuxeo.ecm.core.event");
        runtimeTestCase.deployContrib(
                "org.nuxeo.ecm.core.storage.sql.ra.tests",
                "OSGI-INF/test-core-types-contrib.xml");
        assertNotNull(Framework.getService(SchemaManager.class));
    }

    @Override
    public void tearDown() throws Exception {
        runtimeTestCase.tearDown();
        super.tearDown();
    }

    public static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            for (String child : file.list()) {
                deleteRecursive(new File(file, child));
            }
        }
        file.delete();
    }

    public void testSaveOnCommit() throws Exception {
        InitialContext context = new InitialContext();
        TransactionManager tm = (TransactionManager) context.lookup("java:/TransactionManager");
        Repository repository = (Repository) context.lookup("java:NuxeoRepository/test");
        assertNotNull(repository);

        // check the core APIs that Repository also implements
        org.nuxeo.ecm.core.model.Repository repo = (org.nuxeo.ecm.core.model.Repository) repository;
        SecurityManager sm = repo.getNuxeoSecurityManager();
        assertNotNull(sm);

        // first transaction
        tm.begin();
        Session session = repository.getConnection();
        log.info("using " + session);
        Node root = session.getRootNode();
        assertNotNull(root);
        session.addChildNode(root, "foo", null, "TestDoc", false);
        // let commit do an implicit save
        tm.commit();

        // do stuff outside a transaction, because due to the
        // track-connection-by-tx parameter in the datasource, if there is a
        // connection then all calls to getConnection will return a connection
        // tied to the same underlying ManagedConnection
        //
        // we can't know what the JCA pool does, test two sessions to be sure,
        // at least one of them will be different than the initial one above if
        // the implicit save failed
        Session session1 = repository.getConnection();
        Session session2 = repository.getConnection();
        Node foo1 = session1.getNodeByPath("/foo", null);
        Node foo2 = session2.getNodeByPath("/foo", null);
        assertNotNull(foo1);
        assertNotNull(foo2);
    }

    /**
     * Test that connection sharing allows use of several connections at the
     * same time. Previously, while one connection was in use, the other ones
     * were deassociated and could not be used.
     */
    public void testConnectionSharing() throws Exception {
        InitialContext context = new InitialContext();
        TransactionManager tm = (TransactionManager) context.lookup("java:/TransactionManager");
        Repository repository = (Repository) context.lookup("java:NuxeoRepository/test");
        assertNotNull(repository);

        tm.begin();
        Session session1 = repository.getConnection();
        Session session2 = repository.getConnection();
        // the following access fails if sharing closes other connections
        // (Cannot use closed connection handle)
        Node root1 = session1.getRootNode();
        Node root2 = session2.getRootNode();
        assertNotNull(root1);
        assertNotNull(root2);
        session1.addChildNode(root1, "foo", null, "TestDoc", false);
        // check that this is immediately seen from other connection (underlying
        // ManagedConnection is the same)
        Node foo2 = session2.getNodeByPath("/foo", null);
        assertNotNull(foo2);
        tm.commit();
    }

}
