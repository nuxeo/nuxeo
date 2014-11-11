/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.jetty;

import java.util.HashMap;
import java.util.Map;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ContextManager {

    protected Server server;
    protected Map<String, Context> contexts;

    public ContextManager(Server server) {
        this.server = server;
        this.contexts = new HashMap<String, Context>();
    }

    public Server getServer() {
        return server;
    }

    public synchronized void addFilter(FilterDescriptor descriptor) {
        String path = descriptor.getContext();
        Context ctx = contexts.get(path);
        if (ctx == null) {
            ctx = new Context(server, path, Context.SESSIONS | Context.NO_SECURITY);
            contexts.put(path, ctx);
        }
        FilterHolder holder = new FilterHolder(descriptor.getClazz());
        String name = descriptor.getName();
        if (name != null) {
            holder.setName(name);
        }
        String desc = descriptor.getDescription();
        if (desc != null) {
            holder.setDisplayName(desc);
        }
        Map<String,String> params = descriptor.getInitParams();
        if (params != null) {
            holder.setInitParameters(params);
        }
        ctx.addFilter(holder, descriptor.getPath(), org.mortbay.jetty.Handler.DEFAULT);
    }

    public synchronized void addServlet(ServletDescriptor descriptor) {
        String path = descriptor.getContext();
        Context ctx = contexts.get(path);
        if (ctx == null) {
            ctx = new Context(server, path, Context.SESSIONS | Context.NO_SECURITY);
            contexts.put(path, ctx);
        }
        ServletHolder holder = new ServletHolder(descriptor.getClazz());
        String name = descriptor.getName();
        if (name != null) {
            holder.setName(name);
        }
        String desc = descriptor.getDescription();
        if (desc != null) {
            holder.setDisplayName(desc);
        }
        Map<String,String> params = descriptor.getInitParams();
        if (params != null) {
            holder.setInitParameters(params);
        }
        ctx.addServlet(holder, descriptor.getPath());
    }

    public synchronized void removeFilter(FilterDescriptor descriptor) {
        //TODO not yet implemented
    }

    public synchronized void removeServlet(ServletDescriptor descriptor) {
        //TODO not yet implemented
    }

}

