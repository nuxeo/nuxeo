/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Use it as the webengine servlet in web.xml if you want hot reload, otherwise use {@link ServletContainer}.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ReloadingJerseyServlet extends ServletContainer implements Reloadable {

    private static final long serialVersionUID = 1L;

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
        WebEngine engine = Framework.getService(WebEngine.class);
        if (engine.tryReload()) {
            reload();
        }
    }
}
