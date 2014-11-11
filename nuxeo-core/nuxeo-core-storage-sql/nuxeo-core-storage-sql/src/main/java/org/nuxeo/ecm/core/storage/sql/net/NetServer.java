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
package org.nuxeo.ecm.core.storage.sql.net;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletMapping;
import org.nuxeo.ecm.core.storage.sql.BinaryManager;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.ServerDescriptor;

/**
 * Network server for a repository. Can receive remote connections for
 * {@link Mapper} and {@link BinaryManager} access (depending on the registered
 * servlets).
 * <p>
 * Runs an embedded Jetty server.
 */
public class NetServer {

    protected static NetServer instance;

    protected final Server server;

    public static String add(ServerDescriptor serverDescriptor,
            String servletName, Servlet servlet, String path) {
        return instance().addRepositoryServer(serverDescriptor, servletName,
                servlet, path);
    }

    public static Servlet get(ServerDescriptor serverDescriptor, String servletName) {
        return instance().getServlet(serverDescriptor, servletName);
    }
    public static void remove(ServerDescriptor serverDescriptor,
            String servletName) {
        instance().removeRepositoryServer(serverDescriptor, servletName);
    }

    protected static synchronized NetServer instance() {
        if (instance == null) {
            instance = new NetServer();
            try {
                instance.server.start();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    protected static synchronized void shutDown() {
        if (instance != null) {
            Server server = instance.server;
            instance = null;
            try {
                server.stop();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected NetServer() {
        server = new Server();
        // set empty handlers otherwise getHandlers fails
        server.setHandlers(new ContextHandler[0]);
    }

    protected String addRepositoryServer(ServerDescriptor serverDescriptor,
            String servletName, Servlet servlet, String path) {
        try {
            addConnector(serverDescriptor);
            String contextPath = getContextPath(serverDescriptor);
            Context context = addContext(contextPath);
            ServletHandler servletHandler = context.getServletHandler();
            ServletHolder servletHolder = new ServletHolder(servlet);
            servletHolder.setName(servletName);
            if (!path.startsWith("/")) {
                path = '/' + path;
            }
            servletHandler.addServletWithMapping(servletHolder, path);
            return "http://" + serverDescriptor.host + ':'
                    + serverDescriptor.port + contextPath + path;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void removeRepositoryServer(ServerDescriptor serverDescriptor,
            String servletName) {
        try {
            boolean stop = removeConnector(serverDescriptor);
            Context context = removeContext(getContextPath(serverDescriptor));
            ServletHandler servletHandler = context.getServletHandler();

            // remove servlet mapping
            LinkedList<ServletMapping> sml = new LinkedList<ServletMapping>(
                    Arrays.asList(servletHandler.getServletMappings()));
            for (Iterator<ServletMapping> it = sml.iterator(); it.hasNext();) {
                ServletMapping sm = it.next();
                if (sm.getServletName().equals(servletName)) {
                    it.remove();
                    break;
                }
            }
            servletHandler.setServletMappings(sml.toArray(new ServletMapping[0]));

            // remove servlet
            List<ServletHolder> sl = new LinkedList<ServletHolder>(
                    Arrays.asList(servletHandler.getServlets()));
            for (Iterator<ServletHolder> it = sl.iterator(); it.hasNext();) {
                ServletHolder s = it.next();
                if (s.getName().equals(servletName)) {
                    it.remove();
                    break;
                }
            }
            servletHandler.setServlets(sl.toArray(new ServletHolder[0]));

            if (stop) {
                shutDown();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Servlet getServlet(ServerDescriptor serverDescriptor, String servletName) {
        Context context = getContext(getContextPath(serverDescriptor));
        ServletHandler handler = context.getServletHandler();
        ServletHolder holder = handler.getServlet(servletName);
        try {
            return holder.getServlet();
        } catch (ServletException e) {
            throw new RuntimeException("No such servlet "
                    + serverDescriptor.getUrl() + ":" + servletName, e);
        }
    }

    protected String getContextPath(ServerDescriptor serverDescriptor) {
        String contextPath = serverDescriptor.path;
        if (!contextPath.startsWith("/")) {
            contextPath = '/' + contextPath;
        }
        return contextPath;
    }

    protected final Map<Connector, AtomicLong> connectorRefs = new HashMap<Connector, AtomicLong>();

    protected void addConnector(ServerDescriptor serverDescriptor)
            throws Exception {
        Connector connector = getConnector(serverDescriptor);
        if (connector == null) {
            connector = new SocketConnector();
            connector.setHost(serverDescriptor.host);
            connector.setPort(serverDescriptor.port);
            server.addConnector(connector);
            connector.start();
            connectorRefs.put(connector, new AtomicLong());
        }
        connectorRefs.get(connector).incrementAndGet();
    }

    /** Returns {@code true} if last connector removed. */
    protected boolean removeConnector(ServerDescriptor serverDescriptor)
            throws Exception {
        Connector connector = getConnector(serverDescriptor);
        if (connector == null) {
            throw new RuntimeException("Unknown connector for: "
                    + serverDescriptor);
        }
        long refs = connectorRefs.get(connector).decrementAndGet();
        if (refs == 0) {
            connectorRefs.remove(connector);
            connector.stop();
            List<Connector> cl = new LinkedList<Connector>(
                    Arrays.asList(server.getConnectors()));
            cl.remove(connector);
            server.setConnectors(cl.toArray(new Connector[0]));
            if (cl.size() == 0) {
                return true;
            }
        }
        return false;
    }

    protected Connector getConnector(ServerDescriptor serverDescriptor) {
        Connector[] connectors = server.getConnectors();
        if (connectors == null) {
            return null;
        }
        for (Connector c : connectors) {
            if (c.getHost().equals(serverDescriptor.host)
                    && c.getPort() == serverDescriptor.port) {
                return c;
            }
        }
        return null;
    }

    protected final Map<Context, AtomicLong> contextRefs = new HashMap<Context, AtomicLong>();

    protected Context addContext(String path) throws Exception {
        Context context = getContext(path);
        if (context == null) {
            context = new Context(server, path, Context.SESSIONS);
            context.start();
            contextRefs.put(context, new AtomicLong());
        }
        contextRefs.get(context).incrementAndGet();
        return context;
    }

    protected Context removeContext(String path) throws Exception {
        Context context = getContext(path);
        if (context == null) {
            throw new RuntimeException("Unknown context: " + path);
        }
        long refs = contextRefs.get(context).decrementAndGet();
        if (refs == 0) {
            contextRefs.remove(context);
            context.stop();
            server.removeHandler(context);
        }
        return context;
    }

    protected Context getContext(String path) {
        Handler[] handlers = server.getHandlers();
        if (handlers == null) {
            return null;
        }
        for (Handler h : handlers) {
            if (!(h instanceof Context)) {
                continue;
            }
            Context c = (Context) h;
            if (c.getContextPath().equals(path)) {
                return c;
            }
        }
        return null;
    }

}
