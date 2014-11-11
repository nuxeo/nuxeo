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
package org.nuxeo.ecm.webengine.app;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.app.impl.DefaultContext;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.impl.AbstractWebContext;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineFilter implements Filter {

    protected WebEngine engine;
    
    
//    protected boolean enableJsp = false;
//    private static boolean isTaglibLoaded = false;

    
    public void init(FilterConfig filterConfig) throws ServletException {
        engine = Framework.getLocalService(WebEngine.class);
//        String v = Framework.getProperty("org.nuxeo.ecm.webengine.enableJsp");
//        if ("true".equals(v)) {
//            enableJsp = true;
//        }
   }

    public void destroy() {
        engine = null;
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {        
        if (request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest)request;
            HttpServletResponse resp = (HttpServletResponse)response;
            DefaultContext ctx = new DefaultContext((HttpServletRequest)request);
            WebEngine.setActiveContext(ctx);
            request.setAttribute(WebContext.class.getName(), ctx);
            try {
                preRequest(req, resp);
                chain.doFilter(request, response);
                postRequest(req, resp);
            } catch (ServletException e) {
                throw e;
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new ServletException(e);
            } finally {
                cleanup(ctx, req, resp);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
    
    
    public void preRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // need to set the encoding of characters manually
        if (null == request.getCharacterEncoding()) {
            request.setCharacterEncoding("UTF-8");
        }
        //response.setCharacterEncoding("UTF-8");
//TODO: remove this        
//        if (enableJsp) {
//            WebEngine engine = Framework.getLocalService(WebEngine.class);
//            if (!isTaglibLoaded) {
//                synchronized (this) {
//                    if (!isTaglibLoaded) {
//                        engine.loadJspTaglib(this);
//                        isTaglibLoaded = true;
//                    }
//                }
//            }
//            engine.initJspRequestSupport(this, request,
//                    response);
//        }

    }

    public void postRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // check if the target resource don't want automatic headers to be inserted
        if (null != request.getAttribute("org.nuxeo.webengine.DisableAutoHeaders")) {
            // insert automatic headers
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.addHeader("Cache-Control", "no-store");
            response.addHeader("Cache-Control", "must-revalidate");
            response.addHeader("Expires", "0");
            response.setDateHeader("Expires", 0); // prevents caching
        }        
    }
    
    public void cleanup(AbstractWebContext ctx, HttpServletRequest request, HttpServletResponse response) {
        if (ctx != null) {
            UserSession us = UserSession.tryGetCurrentSession(request);
            if (us != null) {
                us.terminateRequest(request);
            }
        }
        WebEngine.setActiveContext(null);
    }
    
    
}
