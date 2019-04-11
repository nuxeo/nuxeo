/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.nuxeo.ecm.webengine.jaxrs.Activator;
import org.nuxeo.ecm.webengine.jaxrs.servlet.ApplicationServlet;

/**
 * WebEngine integration with OSGi JAX-RS model from ECR.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebEngineServlet extends ApplicationServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public void init(ServletConfig config) throws ServletException {
        bundle = Activator.getInstance().getContext().getBundle();
        super.init(config);
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        containerService(request, response);
    }

    @Override
    protected void containerService(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        if (isDirty) {
            reloadContainer();
        }
        String method = request.getMethod().toUpperCase();
        if (!"GET".equals(method)) {
            // force reading properties because jersey is consuming one
            // character
            // from the input stream - see WebComponent.isEntityPresent.
            request.getParameterMap();
        }
        container.service(request, response);
    }
}
