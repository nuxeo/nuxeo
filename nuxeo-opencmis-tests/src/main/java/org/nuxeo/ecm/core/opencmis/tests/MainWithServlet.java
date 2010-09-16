/*
 * (C) Copyright 2009,2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.opencmis.tests;

import java.net.URI;
import java.util.EventListener;

import javax.servlet.Servlet;

import org.apache.chemistry.opencmis.server.impl.atompub.BasicAuthCallContextHandler;
import org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

/**
 * Test class that runs a CMIS servlet on a repository initialized with a few
 * simple documents.
 */
public class MainWithServlet extends SQLRepositoryTestCase {

    private static final Log log = LogFactory.getLog(MainWithServlet.class);

    // from CmisAtomPubServlet TODO make it public
    public static final String PARAM_CALL_CONTEXT_HANDLER = "callContextHandler";

    public static final String HOST = "localhost";

    public static final int PORT = 17480;

    public static final int WAIT_MINUTES = 60;

    public Server server;

    public URI serverURI;

    public static void main(String[] args) throws Exception {
        new MainWithServlet().intanceMain(args);
    }

    public void intanceMain(String[] args) throws Exception {
        setUp();
        try {
            Thread.sleep(1000 * 60 * WAIT_MINUTES);
        } finally {
            tearDown();
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // deployBundle("org.nuxeo.ecm.core.event");
        openSession();

        Helper.makeNuxeoRepository(session);

        log.warn("CMIS repository starting...");
        setUpServer(session);
        log.warn("CMIS repository started, AtomPub service url: " + serverURI);
    }

    @Override
    public void tearDown() throws Exception {
        tearDownServer();
        closeSession();
        super.tearDown();
    }

    protected void setUpServer(CoreSession coreSession) throws Exception {
        server = new Server();
        Connector connector = new SocketConnector();
        connector.setHost(HOST);
        connector.setPort(PORT);
        server.addConnector(connector);

        Context context = new Context(server, "/", Context.SESSIONS);
        setUpContext(context);

        context.setEventListeners(getEventListeners(coreSession));
        ServletHolder holder = new ServletHolder(getServlet());
        holder.setInitParameter(PARAM_CALL_CONTEXT_HANDLER,
                BasicAuthCallContextHandler.class.getName());
        context.addServlet(holder, "/*");

        serverURI = new URI("http://" + HOST + ':' + PORT + '/');
        server.start();
    }

    protected void tearDownServer() throws Exception {
        log.warn("CMIS repository stopping...");
        server.stop();
        server.join();
        server = null;
        log.warn("CMIS repository stopped");
    }

    protected void setUpContext(Context context) throws Exception {
        // overridden for WebServices
    }

    protected Servlet getServlet() {
        return new CmisAtomPubServlet();
    }

    protected EventListener[] getEventListeners(CoreSession coreSession) {
        return new EventListener[] { new NuxeoCmisContextListener(
                coreSession.getSessionId()) };
        // overridden for WebServices
    }

}