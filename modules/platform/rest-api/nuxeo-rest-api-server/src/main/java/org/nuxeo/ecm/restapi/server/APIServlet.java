/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * This servlet is bound to /api and dispatch calls to /site/api in oder to have better looking URLs.
 *
 * @since 5.7.3
 */
public class APIServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String toReplace = req.getContextPath() + "/api";
        String newPath = req.getRequestURI();
        if (newPath.startsWith(toReplace)) {
            newPath = "/site/api" + newPath.substring(toReplace.length());
        } else {
            throw new NuxeoException("Cannot forward " + newPath);
        }
        RequestDispatcher rd = req.getRequestDispatcher(newPath);
        rd.forward(req, resp);
    }

}
