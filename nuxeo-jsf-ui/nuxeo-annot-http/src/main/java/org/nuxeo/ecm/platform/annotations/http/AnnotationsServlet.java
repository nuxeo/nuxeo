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
import java.io.InputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author Alexandre Russel
 */
public class AnnotationsServlet extends HttpServlet {

    private static final String REPLACE_SOURCE = "replace_source";

    private static final String W3C_ANNOTATES = "w3c_annotates";

    private static final String DOCUMENT_URL = "document_url";

    private static final long serialVersionUID = 1L;

    private final AnnotationServiceFacade facade;

    public AnnotationsServlet() {
        facade = new AnnotationServiceFacade();
    }

    // HTTP Methods
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String w3c_annotates = req.getParameter(W3C_ANNOTATES);
        String annId = null;
        if (req.getPathInfo() != null) {
            annId = req.getPathInfo().replaceFirst("/", "");
        }
        resp.setContentType("application/xml");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Pragma", "no-cache");
        if (annId != null) {
            facade.getAnnotation(annId, (NuxeoPrincipal) req.getUserPrincipal(), resp.getOutputStream(),
                    req.getRequestURL() + "/");
        } else if (w3c_annotates != null && !w3c_annotates.isEmpty()) {
            facade.query(w3c_annotates, resp.getOutputStream(), (NuxeoPrincipal) req.getUserPrincipal());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Pragma", "no-cache");

        String replace_source = req.getParameter(REPLACE_SOURCE);
        if (replace_source != null) {
            try (InputStream is = req.getInputStream()) {
                facade.updateAnnotation(is, (NuxeoPrincipal) req.getUserPrincipal(), resp.getOutputStream(),
                        getBaseUrl(req));
            }
        } else {
            StringBuffer baseUrl = req.getRequestURL();
            facade.createAnnotation(req.getInputStream(), (NuxeoPrincipal) req.getUserPrincipal(),
                    resp.getOutputStream(), baseUrl.toString());
        }
    }

    private static String getBaseUrl(HttpServletRequest req) {
        StringBuffer url = req.getRequestURL();
        int index = url.indexOf(req.getServletPath());
        return url.substring(0, index);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Pragma", "no-cache");
        String annId = req.getPathInfo().replaceFirst("/", "");
        String documentUrl = req.getParameter(DOCUMENT_URL);
        facade.deleteFor(documentUrl, annId, (NuxeoPrincipal) req.getUserPrincipal(), getBaseUrl(req) + "/");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Pragma", "no-cache");
        facade.updateAnnotation(req.getInputStream(), (NuxeoPrincipal) req.getUserPrincipal(), resp.getOutputStream(),
                req.getRequestURL() + "/");
    }

}
