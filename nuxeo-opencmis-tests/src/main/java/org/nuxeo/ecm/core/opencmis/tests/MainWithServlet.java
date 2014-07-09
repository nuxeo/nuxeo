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
package org.nuxeo.ecm.core.opencmis.tests;

import java.net.URI;
import java.util.EventListener;

import javax.servlet.Servlet;

import org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet;
import org.apache.chemistry.opencmis.server.shared.BasicAuthCallContextHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisContextListener;
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
        database.sleepForFulltext();
        closeSession();

        log.warn("CMIS repository starting...");
        setUpServer();
        log.warn("CMIS repository started, AtomPub service url: " + serverURI);
    }

    @Override
    public void tearDown() throws Exception {
        tearDownServer();
        super.tearDown();
    }

    protected void setUpServer() throws Exception {
        server = new Server();
        Connector connector = new SocketConnector();
        connector.setHost(HOST);
        connector.setPort(PORT);
        server.addConnector(connector);

        Context context = new Context(server, "/", Context.SESSIONS);
        setUpContext(context);

        context.setEventListeners(getEventListeners());
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

    protected EventListener[] getEventListeners() {
        return new EventListener[] { new NuxeoCmisContextListener() };
        // overridden for WebServices
    }

}
