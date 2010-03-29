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
package org.nuxeo.ecm.core.chemistry.impl;

import javax.servlet.Servlet;

import org.apache.chemistry.Repository;
import org.apache.chemistry.atompub.client.APPRepositoryService;
import org.apache.chemistry.atompub.server.servlet.CMISServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 * Tests the Nuxeo backend with an AtomPub client.
 */
public class TestNuxeoChemistryAtomPub extends NuxeoChemistryTestCase {

    private static final Log log = LogFactory.getLog(TestNuxeoChemistryAtomPub.class);

    public static final String HOST = "0.0.0.0";

    public static final int PORT = 8287;

    public static final String SERVLET_PATH = "/cmis";

    public static final String CMIS_SERVICE = "/repository";

    public Server server;

    public String startServer() throws Exception {
        Repository repository = makeNuxeoRepository(session);

        server = new Server();
        Connector connector = new SocketConnector();
        connector.setHost(HOST);
        connector.setPort(PORT);
        server.setConnectors(new Connector[] { connector });
        Servlet servlet = new CMISServlet(repository);
        ServletHolder servletHolder = new ServletHolder(servlet);
        Context context = new Context(server, SERVLET_PATH, Context.SESSIONS);
        context.addServlet(servletHolder, "/*");
        server.start();
        String serverUrl = "http://" + HOST + ':' + PORT + SERVLET_PATH
                + CMIS_SERVICE;
        log.info("CMIS server started, AtomPub service url: " + serverUrl);
        return serverUrl;
    }

    public void stopServer() throws Exception {
        // Thread.sleep(60 * MINUTES);
        if (server != null) {
            server.stop();
            log.info("CMIS server stopped");
        }
    }

    @Override
    public Repository makeRepository() throws Exception {
        String serverUrl = startServer();
        return new APPRepositoryService(serverUrl, null).getDefaultRepository();
    }

    @Override
    public void setUp() throws Exception {
        try {
            super.setUp();
        } catch (Exception e) {
            stopServer();
            throw e;
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        stopServer();
    }

    @Override
    public void testQueryScore() throws Exception {
        // TODO cannot return SEARCH_SCORE result set column through AtomPub yet
    }

}
