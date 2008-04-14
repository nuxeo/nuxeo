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

package org.nuxeo.ecm.platform.site.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.site.api.SiteException;
import org.nuxeo.ecm.platform.site.rendering.ServletRequestView;
import org.nuxeo.ecm.platform.site.resolver.DefaultSiteResolver;
import org.nuxeo.ecm.platform.site.resolver.SiteResourceResolver;
import org.nuxeo.ecm.platform.site.template.SiteManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Servlet for publishing SiteObjects
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
public class SiteServlet extends HttpServlet {

    private static final long serialVersionUID = 965764764858L;

    private static final Log log = LogFactory.getLog(SiteServlet.class);

    public static final String CORESESSION_KEY = "SiteCoreSession";

    protected static final int BUFFER_SIZE = 4096 * 16;

    protected static SiteResourceResolver resolver = new DefaultSiteResolver();

    private RenderingEngine engine;
    private SiteManager manager;
    //private SiteRenderingContext siteRenderingContext = new SiteRenderingContext();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        manager = Framework.getLocalService(SiteManager.class);
        engine = manager.getRenderingEngine();
        HashMap<String, Object> env = new HashMap<String, Object>();
        env.put("installDir", manager.getRootDirectory());
        env.put("engine", "Nuxeo Site Engine");
        env.put("version", "1.0.0");
        engine.setSharedVariable("env", env);
        engine.setSharedDocumentView(new ServletRequestView());
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        double start = System.currentTimeMillis();

        CoreSession coreSession = null;

        String path = req.getPathInfo();
        if (path == null || path.length() <= 1) {
            try {
                showIndex(req, resp);
            } catch (Exception e) {
                displayError(resp, null, "Failed to show server main index",
                        SiteConst.SC_INTERNAL_SERVER_ERROR);
            }
            System.out.println(">>> SITE REQUEST TOOK:  "+((System.currentTimeMillis()-start)/1000));
            return;
        }

        SiteRequest siteRequest = null;
        try {
            coreSession = getCoreSession(req);
            siteRequest = createRequest(req, resp, coreSession);
        } catch (Exception e) {
            displayError(resp, e, "Failed to get a core session",
                    SiteConst.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        String method = req.getMethod();
        SiteObject lastTraversedObject = null;
        try {
            lastTraversedObject = siteRequest.traverse();
            if (lastTraversedObject == null) {
                displayError(resp, null, "Site Root is not a supported object ");
                return;
            }
            if (method.equals(SiteConst.METHOD_POST)) {
                lastTraversedObject.getAdapter().doPost(siteRequest, resp);
            } else if (method.equals(SiteConst.METHOD_PUT)) {
                lastTraversedObject.getAdapter().doPut(siteRequest, resp);
            } else if (method.equals(SiteConst.METHOD_GET)) {
                lastTraversedObject.getAdapter().doGet(siteRequest, resp);
            } else if (method.equals(SiteConst.METHOD_DELETE)) {
                lastTraversedObject.getAdapter().doDelete(siteRequest, resp);
            } else if (method.equals(SiteConst.METHOD_HEAD)) {
                lastTraversedObject.getAdapter().doHead(siteRequest, resp);
            }
        } catch (SiteException e) {
            if (lastTraversedObject != null) {
            displayError(resp, e, "Error during calling method " + method
                    + " on " + lastTraversedObject.getAdapter().getName());
            } else {
                displayError(resp, e, "Error during traversal");
            }
            return;
        }

        // return is handler has done the rendering
        if (siteRequest.isRenderingCanceled()) {
            return;
        }

        double s = System.currentTimeMillis();
        try {
            engine.render(siteRequest.getSiteRoot());
        } catch (RenderingException e) {
            displayError(resp, e, "Error during the rendering process");
        }
        System.out.println(">>>>>>>>>> RENDERING TOOK: "+ ((System.currentTimeMillis() - s)/1000));
        resp.setStatus(SiteConst.SC_OK);
        System.out.println(">>> SITE REQUEST TOOK:  "+((System.currentTimeMillis()-start)/1000));
    }

    protected void displayError(HttpServletResponse resp, Throwable t,
            String message, int code) {
        PrintWriter writer = null;
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
        if (t instanceof SiteException) {
            SiteException st = (SiteException) t;
            displayError(resp, t, message, st.getReturnCode());
        } else {
            displayError(resp, t, message, SiteConst.SC_INTERNAL_SERVER_ERROR);
        }
    }

    protected CoreSession getCoreSession(HttpServletRequest request)
            throws Exception {

        // for testing
        CoreSession session = (CoreSession) request.getAttribute("TestCoreSession");

        HttpSession httpSession = request.getSession(true);
        if (session == null) {
            session = (CoreSession) httpSession.getAttribute(CORESESSION_KEY);
        }
        if (session == null) {
            String repoName = getTargetRepositoryName(request);
            RepositoryManager rm = Framework.getService(RepositoryManager.class);
            Repository repo = rm.getRepository(repoName);
            if (repo == null) {
                throw new ClientException("Unable to get " + repoName
                        + " repository");
            }
            session = repo.open();
        }
        if (httpSession != null) {
            httpSession.setAttribute(CORESESSION_KEY, session);
        }
        return session;
    }

    public String getTargetRepositoryName(HttpServletRequest req) {
        return "default";
    }

    /**
     * Build a circular list to describe traversal path
     * @param pathInfo
     * @param session
     * @return
     * @throws Exception
     */
    public SiteRequest createRequest(HttpServletRequest req, HttpServletResponse resp, CoreSession session) throws Exception {
        SiteRequest siteReq = new SiteRequest(req, resp, session);
        Path path = new Path(req.getPathInfo());
        if (path.segmentCount() == 0) {
            return siteReq;
        }
        String[] segments = path.segments();
        String name = segments[0];
        DocumentModel doc = resolver.getSiteRoot(name, session);
        siteReq.addSiteObject(name, doc);
        if (segments.length > 1) {
            for (int i=1; i<segments.length; i++) {
                name = segments[i];
                doc = resolver.getSiteSegment(doc, name, session);
                siteReq.addSiteObject(segments[i], doc);
                if (doc == null) {
                    for (; i<segments.length; i++) {
                        siteReq.addSiteObject(segments[i], null);
                    }
                    break;
                }
            }
        }
        return siteReq;
    }


    public void showIndex(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            double s = System.currentTimeMillis();
            engine.render(new SiteRenderingContext(request, response, manager));
            System.out.println(">>>>>>>>>> STATIC RENDERING TOOK: "+ ((System.currentTimeMillis() - s)/1000));
        } catch (RenderingException e) {
            displayError(response, e, "Error during the rendering process");
        }
    }

}
