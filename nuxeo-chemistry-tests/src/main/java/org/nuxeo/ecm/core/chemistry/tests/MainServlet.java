/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.chemistry.tests;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.chemistry.atompub.server.CMISServlet;
import org.apache.chemistry.repository.Repository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test class that runs a servlet on a repository initialized with a few simple
 * documents.
 *
 * @author Florent Guillaume
 */
public class MainServlet extends NXRuntimeTestCase {

    private static final Log log = LogFactory.getLog(MainServlet.class);

    private static final int MINUTES = 60 * 1000; // in ms

    public static final int PORT = 8080;

    public static final String SERVLET_PATH = "/cmis";

    public static final String CMIS_SERVICE = "/repository";

    public static final String REPOSITORY_NAME = "test";

    private CoreSession session;

    public static void main(String[] args) throws Exception {
        MainServlet main = new MainServlet();
        main.setUp();
        try {
            main.main();
        } finally {
            main.tearDown();
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");

        DatabaseHelper.DATABASE.setUp();
        deployContrib("org.nuxeo.ecm.core.storage.sql.test",
                DatabaseHelper.DATABASE.getDeploymentContrib());

        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", SecurityConstants.ADMINISTRATOR);
        session = CoreInstance.getInstance().open(REPOSITORY_NAME, context);
    }

    @Override
    public void tearDown() throws Exception {
        try {
            CoreInstance.getInstance().close(session);
        } catch (Exception e) {
            // ignore
        }
        try {
            DatabaseHelper.DATABASE.tearDown();
        } catch (Exception e) {
            // ignore
        }
        super.tearDown();
    }

    public static Repository makeRepository() throws IOException {
        // XXX return repo;
        return null;
    }

    public void main() throws Exception {
        Repository repository = makeRepository();
        Server server = new Server(PORT);
        Servlet servlet = new CMISServlet(repository);
        ServletHolder servletHolder = new ServletHolder(servlet);
        Context context = new Context(server, SERVLET_PATH, Context.SESSIONS);
        context.addServlet(servletHolder, "/*");
        server.start();
        String url = "http://localhost:" + PORT + SERVLET_PATH + CMIS_SERVICE;
        log.info("CMIS server started, AtomPub service url: " + url);
        Thread.sleep(60 * MINUTES);
        server.stop();
        log.info("CMIS server stopped");

    }

}
