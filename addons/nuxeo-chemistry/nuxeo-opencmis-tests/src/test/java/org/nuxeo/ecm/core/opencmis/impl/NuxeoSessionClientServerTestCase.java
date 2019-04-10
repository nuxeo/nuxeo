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
package org.nuxeo.ecm.core.opencmis.impl;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.apache.catalina.Wrapper;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.startup.Tomcat;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.bindings.CmisBindingFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.CmisVersion;
import org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet;
import org.apache.chemistry.opencmis.server.shared.BasicAuthCallContextHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.net.JIoEndpoint;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.resource.Resource;
import org.mortbay.thread.QueuedThreadPool;

/**
 * Test case of the high-level session using a client-server connection.
 */
public abstract class NuxeoSessionClientServerTestCase extends
        NuxeoSessionTestCase {

    private static final Log log = LogFactory.getLog(NuxeoSessionClientServerTestCase.class);

    public static final String HOST = "localhost";

    public static final int PORT = 17488;

    public static final boolean USE_TOMCAT = true;

    public Server server;

    public URI serverURI;

    public Tomcat tomcat;

    @Override
    public void setUpCmisSession() throws Exception {
        setUpCmisSession(USERNAME);
    }

    @Override
    protected void setUpCmisSession(String username) throws Exception {
        setUpServer();

        SessionFactory sf = SessionFactoryImpl.newInstance();
        Map<String, String> params = new HashMap<String, String>();

        params.put(SessionParameter.AUTHENTICATION_PROVIDER_CLASS,
                CmisBindingFactory.STANDARD_AUTHENTICATION_PROVIDER);

        params.put(SessionParameter.CACHE_SIZE_REPOSITORIES, "10");
        params.put(SessionParameter.CACHE_SIZE_TYPES, "100");
        params.put(SessionParameter.CACHE_SIZE_OBJECTS, "100");

        params.put(SessionParameter.REPOSITORY_ID, getRepositoryId());
        params.put(SessionParameter.USER, username);
        params.put(SessionParameter.PASSWORD, PASSWORD);

        addParams(params);

        session = sf.createSession(params);
    }

    /** Adds protocol-specific parameters. */
    protected abstract void addParams(Map<String, String> params);

    @Override
    public void tearDownCmisSession() throws Exception {
        if (session != null) {
            session.clear();
            session = null;
        }

        tearDownServer();
    }

    protected void setUpServer() throws Exception {
        if (USE_TOMCAT) {
            setUpTomcat();
        } else {
            setUpJetty();
        }
    }

    protected void tearDownServer() throws Exception {
        if (USE_TOMCAT) {
            tearDownTomcat();
        } else {
            tearDownJetty();
        }
    }

    protected void setUpJetty() throws Exception {
        server = new Server();
        Connector connector = new SocketConnector();
        connector.setHost(HOST);
        connector.setPort(PORT);
        connector.setMaxIdleTime(60 * 1000); // 60 seconds
        server.addConnector(connector);

        Context context = new Context(server, "/", Context.SESSIONS);
        context.setBaseResource(Resource.newClassPathResource("/"
                + BASE_RESOURCE));

        context.setEventListeners(getEventListeners());
        ServletHolder holder = new ServletHolder(getServlet());
        holder.setInitParameter(CmisAtomPubServlet.PARAM_CALL_CONTEXT_HANDLER,
                BasicAuthCallContextHandler.class.getName());
        context.addServlet(holder, "/*");
        // TODO filter

        serverURI = new URI("http://" + HOST + ':' + PORT + '/');
        server.start();
    }

    protected void tearDownJetty() throws Exception {
        ((QueuedThreadPool) server.getThreadPool()).setMaxStopTimeMs(1000);
        server.stop();
        killRemainingServerThreads();
        server.join();
        server = null;
    }

    @SuppressWarnings("deprecation")
    protected void killRemainingServerThreads() throws Exception {
        QueuedThreadPool tp = (QueuedThreadPool) server.getThreadPool();
        int n = tp.getThreads();
        if (n == 0) {
            return;
        }
        log.error(n + " Jetty threads failed to stop, killing them");
        Object threadsLock = getFieldValue(tp, "_threadsLock");
        @SuppressWarnings("unchecked")
        Set<Thread> threadSet = (Set<Thread>) getFieldValue(tp, "_threads");
        List<Thread> threads;
        synchronized (threadsLock) {
            threads = new ArrayList<Thread>(threadSet);
        }
        for (Thread t : threads) {
            t.stop(); // try to stop thread brutally
        }
    }

    protected void setUpTomcat() throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir("."); // for tmp dir
        tomcat.setHostname(HOST);
        tomcat.setPort(PORT);
        ProtocolHandler p = tomcat.getConnector().getProtocolHandler();
        JIoEndpoint endpoint = (JIoEndpoint) getFieldValue(p, "endpoint");
        // ServerSocketFactory factory = new
        // ReuseAddrServerSocketFactory(endpoint);
        // endpoint.setServerSocketFactory(factory);
        // endpoint.getSocketProperties().setSoReuseAddress(true);
        endpoint.setMaxKeepAliveRequests(1); // vital for clean shutdown

        URL url = getResource(BASE_RESOURCE);
        File docBase = new File(url.getPath());
        org.apache.catalina.Context context = tomcat.addContext("/",
                docBase.getAbsolutePath());
        String SERVLET_NAME = "testServlet";
        Wrapper servlet = tomcat.addServlet("/", SERVLET_NAME, getServlet());
        servlet.addInitParameter(CmisAtomPubServlet.PARAM_CALL_CONTEXT_HANDLER,
                BasicAuthCallContextHandler.class.getName());
        servlet.addInitParameter(CmisAtomPubServlet.PARAM_CMIS_VERSION,
                CmisVersion.CMIS_1_1.value());
        context.addServletMapping("/*", SERVLET_NAME);
        context.setApplicationLifecycleListeners(getEventListeners());
        Filter filter = getFilter();
        if (filter != null) {
            String FILTER_NAME = "NuxeoAuthenticationFilter";
            FilterDef filterDef = new FilterDef();
            filterDef.setFilterName(FILTER_NAME);
            filterDef.setFilterClass(filter.getClass().getName());
            context.addFilterDef(filterDef);
            FilterMap filterMap = new FilterMap();
            filterMap.setFilterName(FILTER_NAME);
            filterMap.addServletName(SERVLET_NAME);
            context.addFilterMap(filterMap);
        }

        serverURI = new URI("http://" + HOST + ':' + PORT + '/');
        tomcat.start();
        this.tomcat = tomcat;
    }

    protected void tearDownTomcat() throws Exception {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
            Thread.sleep(100);
            tomcat = null;
        }
    }

    // /** Creates SO_REUSEADDR server sockets. */
    // protected static class ReuseAddrServerSocketFactory extends
    // DefaultServerSocketFactory {
    // public ReuseAddrServerSocketFactory(AbstractEndpoint endpoint) {
    // super(endpoint);
    // }
    //
    // @Override
    // public ServerSocket createSocket(int port) throws IOException {
    // return createSocket(port, 50, null);
    // }
    //
    // @Override
    // public ServerSocket createSocket(int port, int backlog)
    // throws IOException {
    // return createSocket(port, backlog, null);
    // }
    //
    // @Override
    // public ServerSocket createSocket(int port, int backlog,
    // InetAddress ifAddress) throws IOException {
    // ServerSocket serverSocket = new ServerSocket();
    // serverSocket.setReuseAddress(true); // SO_REUSEADDR
    // serverSocket.bind(new InetSocketAddress(ifAddress, port), backlog);
    // return serverSocket;
    // }
    // }

    protected static Object getFieldValue(Object object, String name)
            throws Exception {
        Class<? extends Object> klass = object.getClass();
        Field f = null;
        while (f == null && klass != Object.class) {
            try {
                f = klass.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                klass = klass.getSuperclass();
            }
        }
        f.setAccessible(true);
        return f.get(object);
    }

    protected abstract EventListener[] getEventListeners();

    protected abstract Servlet getServlet();

    protected abstract Filter getFilter();

}
