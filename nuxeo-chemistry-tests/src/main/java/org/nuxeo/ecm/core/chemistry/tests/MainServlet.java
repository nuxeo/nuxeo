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

import javax.servlet.Servlet;

import org.apache.chemistry.atompub.server.CMISServlet;
import org.apache.chemistry.repository.Repository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

/**
 * Test class that runs a servlet on a repository initialized with a few simple
 * documents.
 *
 * @author Florent Guillaume
 */
public class MainServlet extends SQLRepositoryTestCase {

    private static final Log log = LogFactory.getLog(MainServlet.class);

    private static final int MINUTES = 60 * 1000; // in ms

    public static final int PORT = 8080;

    public static final String SERVLET_PATH = "/cmis";

    public static final String CMIS_SERVICE = "/repository";

    public static final String REPOSITORY_NAME = "test";

    public static void main(String[] args) throws Exception {
        MainServlet main = new MainServlet("test");
        main.setUp();
        try {
            main.main();
        } finally {
            main.tearDown();
        }
    }

    protected MainServlet(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // deployBundle("org.nuxeo.ecm.core.event");
        openSession();
    }

    @Override
    protected void tearDown() throws Exception {
        closeSession();
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
        Context context = new Context(server, SERVLET_PATH, Context.SESSIONS);
        context.addServlet(new ServletHolder(servlet), "/*");
        server.start();
        String url = "http://localhost:" + PORT + SERVLET_PATH + CMIS_SERVICE;
        log.info("CMIS server started, AtomPub service url: " + url);
        Thread.sleep(60 * MINUTES);
        server.stop();
        log.info("CMIS server stopped");

    }

}
