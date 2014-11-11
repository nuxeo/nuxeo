/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
        this.bundle = Activator.getInstance().getContext().getBundle();
        super.init(config);
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        containerService(request, response);
    }

    @Override
    protected void containerService(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
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
