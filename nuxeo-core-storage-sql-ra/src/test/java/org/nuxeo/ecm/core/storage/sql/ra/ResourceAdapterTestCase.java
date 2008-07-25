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

    private static Log log = LogFactory.getLog(ResourceAdapterTestCase.class);

    private PublicNXRuntimeTestCase runtimeTestCase;

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

    public void testRepo() throws Exception {
        log.info("Test Repo ----------------");
        InitialContext context = new InitialContext();

        TransactionManager tm = (TransactionManager) context.lookup("java:/TransactionManager");
        tm.begin();

        Repository repository = (Repository) context.lookup("java:NuxeoRepository/test");
        assertNotNull(repository);
        log.info("Got repo " + repository);

        Session session = repository.getConnection();
        Node root = session.getRootNode();
        assertNotNull(root);

        /* Now check the core APIs that this also implements */
        org.nuxeo.ecm.core.model.Repository repo = (org.nuxeo.ecm.core.model.Repository) repository;
        SecurityManager sm = repo.getSecurityManager();
        assertNotNull(sm);

        tm.commit();
        log.info("End ----------------");
    }

}
