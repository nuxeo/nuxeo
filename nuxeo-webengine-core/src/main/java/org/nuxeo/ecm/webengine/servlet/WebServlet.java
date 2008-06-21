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
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.webengine.DefaultWebContext;
import org.nuxeo.ecm.webengine.RequestHandler;
import org.nuxeo.ecm.webengine.WebApplication;
import org.nuxeo.ecm.webengine.WebContext;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.WebObject;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.runtime.api.Framework;

/**
 * Servlet for publishing SiteObjects
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebServlet extends HttpServlet {

    private static final long serialVersionUID = 965764764858L;

    private static final Log log = LogFactory.getLog(WebServlet.class);

    protected static final int BUFFER_SIZE = 4096 * 16;

    private final static ThreadLocal<WebContext> CONTEXT = new ThreadLocal<WebContext>();


    private WebEngine engine;


    public final static WebContext getContext() {
        return CONTEXT.get();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        engine = Framework.getLocalService(WebEngine.class);
    }


    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        //double start = System.currentTimeMillis();
        resp.setContentType("text/html");
        if (req.getMethod().equals(WebConst.METHOD_HEAD)) {
            resp = new NoBodyResponse(resp);
        }

        // create the request path
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            pathInfo = "/";
        }
        Path path = new Path(pathInfo);
        // get the application that match that path
        WebApplication app = engine.getApplicationByPath(path);
        if (app == null) {
            // don't have a context so send an error
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No web application found on that path");
            return;
        }

        WebContext context = null;
        try {
            // create the request context
            context = app.createContext(path, req, resp);
            CONTEXT.set(context);
            service(context, req, resp);
        } catch (Throwable e) {
            WebException we = WebException.wrap(e);
            log.error("Site Servlet failed to handle request", e);
            if (context == null) { // create an empty context
                context = new DefaultWebContext(app, req, resp);
            }
            ScriptFile page = context.getFile(app.getErrorPage());
            if (page == null) {
                displayError(resp, we, "ErrorPage not found: "+app.getErrorPage(),
                        WebConst.SC_INTERNAL_SERVER_ERROR);
                return;
            }
            try {
                context.setProperty("error", we);
                context.exec(page, null);
            } catch (Throwable ee) {
                displayError(resp, ee, "Failed to show error page",
                        WebConst.SC_INTERNAL_SERVER_ERROR);
            }
        } finally {
            CONTEXT.set(null);
        }
        //System.out.println(">>> SITE REQUEST TOOK:  "+((System.currentTimeMillis()-start)/1000));
    }

    protected void service(WebContext context, HttpServletRequest req, HttpServletResponse resp)
            throws Exception {
        if (context.getLastObject() == null) { // a request outside the root
            showStaticPage(context);
            return;
        }

        String method = req.getMethod();
        WebObject targetObject = context.getTargetObject();
        if (targetObject == null) {
            showStaticPage(context);
            return;
        }
        // avoid running default action handling mechanism when invocation is redirected to scripts
        if (context.getPathInfo().getScript() == null) {
            RequestHandler handler = targetObject.getRequestHandler();
            if (handler != null) {
                if (method.equals(WebConst.METHOD_POST)) {
                    handler.doPost(targetObject);
                } else if (method.equals(WebConst.METHOD_PUT)) {
                    handler.doPut(targetObject);
                } else if (method.equals(WebConst.METHOD_GET)) {
                    handler.doGet(targetObject);
                } else if (method.equals(WebConst.METHOD_DELETE)) {
                    handler.doDelete(targetObject);
                } else if (method.equals(WebConst.METHOD_HEAD)) {
                    handler.doHead(targetObject);
                }
            }
        }

        // return is handler has done the rendering
        if (context.isCanceled()) {
            return;
        }

        //double s = System.currentTimeMillis();
        ScriptFile script = context.getTargetScript();
        if (script != null) {
            context.exec(script, null);
        } else {
            context.getResponse().setStatus(WebConst.SC_NOT_FOUND);
        }
        //System.out.println(">>>>>>>>>> RENDERING TOOK: " + ((System.currentTimeMillis() - s) / 1000));
    }


    protected static void displayError(HttpServletResponse resp, Throwable t,
            String message, int code) {
        PrintWriter writer;
        try {
            writer = resp.getWriter();
        } catch (IOException e) {
            log.error("Unable to output Error ", e);
            log.error("Application error was " + message, e);
            return;
        }

        writer.write("\nError occured during Site rendering");
        writer.write("\nSite Error message : " + message);
        if (t != null) {
            writer.write("\nException message : " + t.getMessage());
            for (StackTraceElement element : t.getStackTrace()) {
                writer.write("\n" + element.toString() );
            }
        }

        resp.setStatus(code);
    }

    protected void displayError(HttpServletResponse resp, Throwable t,
            String message) {
        if (t instanceof WebException) {
            WebException st = (WebException) t;
            displayError(resp, t, message, st.getReturnCode());
        } else {
            displayError(resp, t, message, WebConst.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public void showStaticPage(WebContext context) throws Exception {
        try {
            //double s = System.currentTimeMillis();
            ScriptFile script = context.getTargetScript();
            if (script != null) {
                context.exec(script, null);
            } else {
                context.getResponse().sendError(WebConst.SC_NOT_FOUND);
            }
            //System.out.println(">>>>>>>>>> STATIC RENDERING TOOK: "+ ((System.currentTimeMillis() - s)/1000));
        } catch (WebException e) {
            displayError(context.getResponse(), e, "Error during the rendering process");
        }
    }

    @Override
    public void destroy() {
        DefaultWebContext.destroyAnonymousSession();
    }

}
