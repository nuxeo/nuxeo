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

import java.util.Collections;

import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.ServerDescriptor;

/**
 * Tests for NetBackend.
 */
public class TestSQLBackendNet extends TestSQLBackend {

    protected Thread remoteVCS;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");

        // config used by remote
        deployRepositoryContrib();
        // deployContrib("org.nuxeo.ecm.core.storage.sql.test.tests",
        // "OSGI-INF/test-repo-core-types-contrib.xml");
        remoteVCS = new RemoteVCS("test");
        remoteVCS.start();
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
        remoteVCS.interrupt();
        remoteVCS.join();
        super.tearDown();
    }

    public static class RemoteVCS extends Thread {
        protected final String repositoryName;

        private final Object monitor = new String();

        public RemoteVCS(String repositoryName) {
            super("Nuxeo-VCS-Remote");
            this.repositoryName = repositoryName;
        }

        @Override
        public void start() {
            synchronized (monitor) {
                super.start();
                // wait until ready
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void run() {
            org.nuxeo.ecm.core.model.Repository repository = null;
            try {
                // Looking up the model repository will call
                // SQLRepositoryFactory.createRepository to create a
                // SQLRepository which creates a RepositoryImpl,
                // which is configured to spawn a server to listen for remote
                // connections.
                repository = NXCore.getRepositoryService().getRepositoryManager().getRepository(
                        repositoryName);
                // init root
                repository.getSession(null).close();
                // notify ready
                synchronized (monitor) {
                    monitor.notify();
                }
                // now wait until interrupted by caller
                try {
                    while (true) {
                        sleep(1000);
                    }
                } catch (InterruptedException e) {
                    // restore interrupted status
                    interrupt();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (repository != null) {
                    repository.shutdown();
                }
            }
        }
    }

}
