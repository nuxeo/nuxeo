/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webengine.server.resteasy;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.plugins.providers.ByteArrayProvider;
import org.jboss.resteasy.plugins.providers.DefaultTextPlain;
import org.jboss.resteasy.plugins.providers.FormUrlEncodedProvider;
import org.jboss.resteasy.plugins.providers.InputStreamProvider;
import org.jboss.resteasy.plugins.providers.StreamingOutputProvider;
import org.jboss.resteasy.plugins.providers.StringTextStar;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * Mostly copied from {@link HttpServletDispatcher}.
 * <p>
 * Modifications:
 * <ul>
 * <li>Changed Dispatcher implementation.
 * <li>Added methods to register root resources without {@link Path} annotation.
 * <li>Added WebEngine initialization
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebEngineServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected WebEngineDispatcher dispatcher;
//    protected WebEngine engine;

    protected void initializeBuiltinProviders(
            ResteasyProviderFactory providerFactory) {
        // RegisterBuiltin.register(providerFactory);
        try {
            providerFactory.addMessageBodyReader(new DefaultTextPlain());
            providerFactory.addMessageBodyWriter(new DefaultTextPlain());
            providerFactory.addMessageBodyReader(new StringTextStar());
            providerFactory.addMessageBodyWriter(new StringTextStar());
            providerFactory.addMessageBodyReader(new InputStreamProvider());
            providerFactory.addMessageBodyWriter(new InputStreamProvider());
            providerFactory.addMessageBodyReader(new ByteArrayProvider());
            providerFactory.addMessageBodyWriter(new ByteArrayProvider());
            providerFactory.addMessageBodyReader(new FormUrlEncodedProvider());
            providerFactory.addMessageBodyWriter(new FormUrlEncodedProvider());
            providerFactory.addMessageBodyWriter(new StreamingOutputProvider());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        ResourceContainer rc = (ResourceContainer) Framework.getRuntime().getComponent(ResourceContainer.NAME);
        dispatcher = rc.getDispatcher();
        initializeBuiltinProviders(dispatcher.getProviderFactory());
    }


    @Override
    protected void service(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) throws ServletException,
            IOException {
//        if (engine == null) {
//            synchronized (engine) {
//                if (engine == null) {
//                    try {
//                        engine = Framework.getService(WebEngine.class);
//                        engine.getModuleManager(); // force module loading
//                    } catch (Exception e) {
//                        throw new ServletException(e);
//                    }
//                }
//            }
//        }
        httpServletResponse.setHeader("Pragma", "no-cache");
        dispatcher.service(httpServletRequest, httpServletResponse);
    }

}
