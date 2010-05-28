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
package org.nuxeo.ecm.core.storage.sql.net;

import javax.servlet.Servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.ServerDescriptor;

/**
 * Network server for a repository.
 * <p>
 * Runs an embedded Jetty server.
 */
public class NetServer {

    private static final Log log = LogFactory.getLog(NetServer.class);

    public static Server startServer(RepositoryDescriptor repositoryDescriptor) {
        ServerDescriptor serverDescriptor = repositoryDescriptor.listen;
        Server server = new Server();
        Connector connector = new SocketConnector();
        connector.setHost(serverDescriptor.host);
        connector.setPort(serverDescriptor.port);
        String path = serverDescriptor.path;
        if (!path.startsWith("/")) {
            path = '/' + path;
        }
        server.setConnectors(new Connector[] { connector });
        Servlet servlet = new NetServlet(repositoryDescriptor);
        ServletHolder servletHolder = new ServletHolder(servlet);
        Context context = new Context(server, path, Context.SESSIONS);
        context.addServlet(servletHolder, "/*");
        try {
            server.start();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String serverUrl = "http://" + serverDescriptor.host + ':'
                + serverDescriptor.port + path;
        log.info(String.format("VCS server for repository '%s' started on: %s",
                repositoryDescriptor.name, serverUrl));
        return server;
    }

    public static void stopServer(Object object) {
        if (!(object instanceof Server)) {
            throw new RuntimeException("Not a Server: " + object);
        }
        Server server = (Server) object;
        try {
            server.stop();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
