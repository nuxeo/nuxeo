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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.site.api.SiteAwareObject;
import org.nuxeo.ecm.platform.site.api.SiteException;
import org.nuxeo.ecm.platform.site.rendering.SiteRenderingContext;
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


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        SiteManager mgr = Framework.getLocalService(SiteManager.class);
        engine = mgr.getRenderingEngine();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        double start = System.currentTimeMillis();

        String method = req.getMethod();

        CoreSession coreSession = null;


        List<DocumentModel> doc2traverse = null;
        try {
            coreSession = getCoreSession(req);
            doc2traverse = resolver.resolvePath(req, coreSession);
        } catch (Exception e) {
            displayError(resp, e, "Unable to resolve traversal path",
                    SiteConst.SC_NOT_FOUND);
            return;
        }

        List<DocumentModel> nonTraversedDocs = new ArrayList<DocumentModel>();

        SiteRequest siteRequest = new SiteRequest(req, coreSession, doc2traverse);

        // traverse all objects that are traversable
        while (!doc2traverse.isEmpty() && nonTraversedDocs.isEmpty()) {
            DocumentModel doc = doc2traverse.remove(0);

            SiteAwareObject siteOb = doc.getAdapter(SiteAwareObject.class);
            if (siteOb == null) {
                nonTraversedDocs.add(doc);
            } else {
                boolean canTraverse = false;
                try {
                    canTraverse = siteOb.traverse(siteRequest, resp);
                } catch (SiteException e) {
                    displayError(resp, e,
                            "Error during traversal of SiteObject "
                                    + siteOb.getTitle());
                    return;
                }
                if (canTraverse) {
                    siteRequest.addToTraversalPath(siteOb);
                } else {
                    nonTraversedDocs.add(doc);
                }
            }
        }

        // get non traversed object to attach them to the request
        if (0 < doc2traverse.size()) {
            nonTraversedDocs.addAll(doc2traverse);
        }

        siteRequest.setNonTraversedDocs(nonTraversedDocs);

        // call the handling methods on last traversed object
        List<SiteAwareObject> traversedObjects = siteRequest.getTraversalPath();
        SiteAwareObject lastTraversedObject = traversedObjects.get(traversedObjects.size() - 1);

        try {
            if (method.equals(SiteConst.METHOD_POST)) {
                lastTraversedObject.doPost(siteRequest, resp);
            } else if (method.equals(SiteConst.METHOD_PUT)) {
                lastTraversedObject.doPut(siteRequest, resp);
            } else if (method.equals(SiteConst.METHOD_GET)) {
                lastTraversedObject.doGet(siteRequest, resp);
            } else if (method.equals(SiteConst.METHOD_DELETE)) {
                lastTraversedObject.doDelete(siteRequest, resp);
            } else if (method.equals(SiteConst.METHOD_HEAD)) {
                lastTraversedObject.doHead(siteRequest, resp);
            }
        } catch (SiteException e) {
            displayError(resp, e, "Error during calling method " + method
                    + " on " + lastTraversedObject.getTitle());
            return;
        }

        // return is handler has done the rendering
        if (siteRequest.isRenderingCanceled()) {
            return;
        }

        RenderingContext rc = new SiteRenderingContext(siteRequest, resp,
                traversedObjects.get(0));

        double s = System.currentTimeMillis();
        try {
            engine.render(rc);
        } catch (RenderingException e) {
            displayError(resp, e, "Error during the rendering process");
        }
        System.out.println(">>>>>>>>>> RENDERING TOOK: "+ ((System.currentTimeMillis() - s)/1000));

        // rendering loop in reverse order
        /*
         * List<SiteAwareObject> siteObjects2Render = new ArrayList<SiteAwareObject>();
         * siteObjects2Render.addAll(siteRequest.getTraversalPath());
         * Collections.reverse(siteObjects2Render);
         *
         * OldRenderingContext renderContext = new
         * OldRenderingContext(siteRequest); String renderingEngineName =
         * siteRequest.getEngineName();
         *
         * InputStream out=null; for (SiteAwareObject siteOb :
         * siteObjects2Render) { siteRequest.setCurrentSiteObject(siteOb);
         * InputStream template = siteOb.getTemplate(siteRequest); if
         * (template!=null) { InputStream result=null; if
         * (siteOb.needsRendering(siteRequest)) { result =
         * render(renderingEngineName, template, renderContext); } else {
         * result=template; } renderContext.put(siteOb.getSlotId(), result);
         * out=result; } } if (out!=null) { int read=0; byte[] buffer = new
         * byte[BUFFER_SIZE]; ServletOutputStream respStream =
         * resp.getOutputStream(); while ((read = out.read(buffer)) != -1) {
         * respStream.write(buffer,0, read); } }
         */
        resp.setStatus(SiteConst.SC_OK);

        System.out.println(">>> SITE REQUEST TOOK:  "+((System.currentTimeMillis()-start)/1000));
    }

    /*
    protected InputStream render(String renderingEngineName,
            InputStream template, OldRenderingContext renderContext) {
        SiteRenderingEngine engine = RenderingEngineFactory.getEngine(renderingEngineName);

        if (engine == null)
            return new StringBufferInputStream("");

        return engine.render(template, renderContext);

    }*/

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
        writer.write("\nException message : " + t.getMessage());
        for (StackTraceElement element : t.getStackTrace()) {
            writer.write("\n" + element.toString() );
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
            String repoName = resolver.getTargetRepositoryName(request);
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

}
