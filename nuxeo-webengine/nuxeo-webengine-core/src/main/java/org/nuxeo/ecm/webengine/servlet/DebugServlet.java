/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.servlet;

import java.io.IOException;
import java.lang.reflect.Proxy;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for accessing common file resources
 *
 * @author <a href="mailto:bs@nuxeo.com">Stefanescu Bogdan</a>
 */
public class DebugServlet extends HttpServlet {

    private static final long serialVersionUID = 4235895566712482208L;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/plain");
        String path = req.getPathInfo();
        if (path == null) {
            path = "java:comp/env";
        }
        try {
            Context ctx = new InitialContext();
            StringBuffer buf = new StringBuffer();
            buf.append(">>>> GLOBAL: \r\n");
            list(ctx, ">> ",  buf, true);
            Object o = ctx.lookup("java:");
            if (o instanceof Context) {
                buf.append("----------------------------------------------\r\n");
                buf.append(">>>> JAVA: \r\n");
                list((Context)o, ">> ",  buf, true);
            }
            System.out.println(buf.toString());
            resp.getWriter().write(buf.toString());

            o = ctx.lookup("jdbc/nxsqldirectory");
            System.out.println(">>NXQLDIR: "+o);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void list(Context ctx, String indent, StringBuffer buffer,
            boolean verbose) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            NamingEnumeration ne = ctx.list("");
            while (ne.hasMore()) {
                NameClassPair pair = (NameClassPair) ne.next();

                String name = pair.getName();
                String className = pair.getClassName();
                boolean recursive = false;
                boolean isLinkRef = false;
                boolean isProxy = false;
                Class c = null;
                try {
                    c = loader.loadClass(className);

                    if (Context.class.isAssignableFrom(c)) {
                        recursive = true;
                    }
                    if (LinkRef.class.isAssignableFrom(c)) {
                        isLinkRef = true;
                    }

                    isProxy = Proxy.isProxyClass(c);
                } catch (ClassNotFoundException cnfe) {
                    // If this is a $Proxy* class its a proxy
                    if (className.startsWith("$Proxy")) {
                        isProxy = true;
                        // We have to get the class from the binding
                        try {
                            Object p = ctx.lookup(name);
                            c = p.getClass();
                        } catch (NamingException e) {
                            Throwable t = e.getRootCause();
                            if (t instanceof ClassNotFoundException) {
                                // Get the class name from the exception msg
                                String msg = t.getMessage();
                                if (msg != null) {
                                    // Reset the class name to the CNFE class
                                    className = msg;
                                }
                            }
                        }
                    }
                }

                buffer.append(indent).append(" +- ").append(name);

                // Display reference targets
                if (isLinkRef) {
                    // Get the
                    try {
                        Object obj = ctx.lookupLink(name);

                        LinkRef link = (LinkRef) obj;
                        buffer.append("[link -> ");
                        buffer.append(link.getLinkName());
                        buffer.append(']');
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }

                // Display proxy interfaces
                if (isProxy) {
                    buffer.append(" (proxy: ").append(pair.getClassName());
                    if (c != null) {
                        Class[] ifaces = c.getInterfaces();
                        buffer.append(" implements ");
                        for (Class iface : ifaces) {
                            buffer.append(iface);
                            buffer.append(',');
                        }
                        buffer.setCharAt(buffer.length() - 1, ')');
                    } else {
                        buffer.append(" implements ").append(className).append(")");
                    }
                } else if (verbose) {
                    buffer.append(" (class: ").append(pair.getClassName()).append(")");
                }

                buffer.append('\n');
                if (recursive) {
                    try {
                        Object value = ctx.lookup(name);
                        if (value instanceof Context) {
                            Context subctx = (Context) value;
                            list(subctx, indent + " |  ", buffer, verbose);
                        } else {
                            buffer.append(indent).append(" |   NonContext: ").append(value);
                            buffer.append('\n');
                        }
                    } catch (Throwable t) {
                        buffer.append("Failed to lookup: ").append(name)
                                .append(", errmsg=").append(t.getMessage()).append('\n');
                    }
                }
            }
            // TODO not supported by glassfish
            // ne.close();
        } catch (NamingException ne) {
            ne.printStackTrace();
        }
    }

}
