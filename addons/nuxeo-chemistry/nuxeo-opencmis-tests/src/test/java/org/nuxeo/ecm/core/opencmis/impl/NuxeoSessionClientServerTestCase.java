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
package org.nuxeo.ecm.core.opencmis.impl;

import java.net.URI;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.bindings.CmisBindingFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.SessionType;
import org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet;
import org.apache.chemistry.opencmis.server.shared.BasicAuthCallContextHandler;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisAuthHandler;
import org.nuxeo.ecm.core.opencmis.bindings.TrustingLoginProvider;

/**
 * Test case of the high-level session using a client-server connection.
 */
public abstract class NuxeoSessionClientServerTestCase extends
        NuxeoSessionTestCase {

    public static final String HOST = "localhost";

    public static final int PORT = 17488;

    public Server server;

    public URI serverURI;

    @Override
    public void setUpCmisSession() throws Exception {
        setUpServer();

        SessionFactory sf = SessionFactoryImpl.newInstance();
        Map<String, String> params = new HashMap<String, String>();

        params.put(SessionParameter.SESSION_TYPE,
                SessionType.PERSISTENT.value());
        params.put(SessionParameter.AUTHENTICATION_PROVIDER_CLASS,
                CmisBindingFactory.STANDARD_AUTHENTICATION_PROVIDER);

        params.put(SessionParameter.CACHE_SIZE_REPOSITORIES, "10");
        params.put(SessionParameter.CACHE_SIZE_TYPES, "100");
        params.put(SessionParameter.CACHE_SIZE_OBJECTS, "100");

        params.put(SessionParameter.REPOSITORY_ID, getRepositoryId());
        params.put(SessionParameter.USER, USERNAME);
        params.put(SessionParameter.PASSWORD, PASSWORD);

        addParams(params);

        session = sf.createSession(params);
    }

    /** Adds protocol-specific parameters. */
    protected abstract void addParams(Map<String, String> params);

    @Override
    public void tearDownCmisSession() throws Exception {
        session.clear(); // TODO XXX close
        session = null;

        tearDownServer();
    }

    protected void setUpServer() throws Exception {
        // disable login checks
        System.setProperty(NuxeoCmisAuthHandler.LOGIN_PROVIDER_PROP,
                TrustingLoginProvider.class.getName());

        server = new Server();
        Connector connector = new SocketConnector();
        connector.setHost(HOST);
        connector.setPort(PORT);
        connector.setMaxIdleTime(60 * 1000); // 60 seconds
        server.addConnector(connector);

        Context context = new Context(server, "/", Context.SESSIONS);
        setUpContext(context);

        context.setEventListeners(getEventListeners());
        ServletHolder holder = new ServletHolder(getServlet());
        holder.setInitParameter(CmisAtomPubServlet.PARAM_CALL_CONTEXT_HANDLER,
                BasicAuthCallContextHandler.class.getName());
        context.addServlet(holder, "/*");

        serverURI = new URI("http://" + HOST + ':' + PORT + '/');
        server.start();
    }

    protected void tearDownServer() throws Exception {
        System.clearProperty(NuxeoCmisAuthHandler.LOGIN_PROVIDER_PROP);
        server.stop();
        server.join();
        server = null;
    }

    protected void setUpContext(Context context) throws Exception {
        // overridden for WebServices
    }

    protected abstract EventListener[] getEventListeners();

    protected abstract Servlet getServlet();

}
