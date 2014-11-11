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
package org.nuxeo.ecm.webengine.app.jersey;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.jaxrs.Reloadable;
import org.nuxeo.runtime.api.Framework;

import com.sun.jersey.spi.container.servlet.ServletContainer;

/**
 * JAX-RS servlet based on jersey servlet to provide hot reloading.
 * <p>
 * Use it as the webengine servlet in web.xml if you want hot reload, otherwise
 * use {@link ServletContainer}.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ReloadingJerseyServlet extends ServletContainer implements
        Reloadable {

    private static final long serialVersionUID = 1L;

    protected WebEngine engine;

    @Override
    public void init() throws ServletException {
        super.init();
        engine = Framework.getLocalService(WebEngine.class);
    }

    @Override
    public void destroy() {
        engine = null;
        super.destroy();
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (engine == null) {
            engine = Framework.getLocalService(WebEngine.class);
        }
        if (engine.isDevMode()) {
            reloadIfNeeded();
        }
        String method = request.getMethod().toUpperCase();
        if (!"GET".equals(method)) {
            // force reading properties because jersey is consuming one
            // character
            // from the input stream - see WebComponent.isEntityPresent.
            request.getParameterMap();
        }
        super.service(request, response);
    }

    public synchronized void reloadIfNeeded() {
        if (engine.tryReload()) {
            reload();
        }
    }
}
