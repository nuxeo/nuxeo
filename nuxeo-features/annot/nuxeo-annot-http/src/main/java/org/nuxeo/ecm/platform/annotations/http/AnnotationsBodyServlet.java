/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.http;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author Alexandre Russel
 */
public class AnnotationsBodyServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final AnnotationServiceFacade facade;

    public AnnotationsBodyServlet() {
        facade = new AnnotationServiceFacade();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String annId = req.getPathInfo().replaceFirst("/", "");
        String body = facade.getAnnotationBody(annId, (NuxeoPrincipal) req.getUserPrincipal(), req.getRequestURL() + "/");
        resp.getWriter().write(body);
    }
}
