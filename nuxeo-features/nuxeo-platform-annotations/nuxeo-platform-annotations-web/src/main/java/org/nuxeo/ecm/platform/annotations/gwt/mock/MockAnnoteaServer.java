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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.mock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Alexandre Russel
 *
 */
public class MockAnnoteaServer extends HttpServlet {
    private static final String DESCRIPTION = "</r:Description>";

    private static final String R_DESCRIPTION = "<r:Description>";

    private static final StringBuilder response = new StringBuilder(
            " <r:RDF xmlns:r=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n"
                    + "           xmlns:a=\"http://www.w3.org/2000/10/annotation-ns#\" \n"
                    + "           xmlns:d=\"http://purl.org/dc/elements/1.1/\"></r:RDF>");

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Writer writer = resp.getWriter();
        writer.write(response.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        InputStream  is = req.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String result;
        StringBuilder request = new StringBuilder();
        while((result = reader.readLine()) != null) {
            request.append(result);
        };
        String r = request.toString();
        String desc = r.substring(r.indexOf(R_DESCRIPTION), r.indexOf(DESCRIPTION) + DESCRIPTION.length());
        response.insert(response.lastIndexOf("</r:RDF>"), desc);
    }
}
