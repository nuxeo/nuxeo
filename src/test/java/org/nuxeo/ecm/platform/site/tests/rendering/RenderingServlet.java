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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.site.tests.rendering;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.FreemarkerEngine;
import org.nuxeo.runtime.api.Framework;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RenderingServlet extends HttpServlet {

    private static final long serialVersionUID = 7547124250731789991L;

    private RenderingEngine engine;


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String engineClass = getInitParameter("engine");
        if (engineClass == null) {
            engine = new FreemarkerEngine();
        } else {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Class<?> klass = null;
                if (cl == null) {
                    klass = Class.forName(engineClass);
                } else {
                    klass = cl.loadClass(engineClass);
                }
                engine = (RenderingEngine)klass.newInstance();
                engine.setResourceLocator(new ResourceLocator() {
                    public URL getResource(String key) {
                        ClassLoader cl =Thread.currentThread().getContextClassLoader();
                        if (cl == null) {
                            cl = RenderingServlet.class.getClassLoader();
                        }
                        return cl.getResource(key);
                    }
                });
            } catch (Exception e) {
                throw new ServletException("Failed to instantiate engine", e);
            }
        }
    }

    @Override
    public void destroy() {
        engine = null;
        super.destroy();
    }

    /**
     * Must
     * @param req
     * @return
     */
    protected RenderingContext createContext(HttpServletRequest req) {
        try {
            DocumentModel doc = null;
            String id = req.getParameter("id");
            CoreSession session = Framework.getService(RepositoryManager.class).getDefaultRepository().open();
            if (id == null) {
                doc = session.getRootDocument();
            } else {
                doc = session.getDocument(new IdRef(id));
            }
            return new HttpSimpleContext(doc, req);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        RenderingContext ctx = createContext(req);
        OutputStream out = new BufferedOutputStream(resp.getOutputStream());
        try {
            engine.render(ctx);
        } catch (RenderingException e) {
            throw new ServletException("Rendering failed", e);
        } finally {
            out.flush();
        }

    }

}
