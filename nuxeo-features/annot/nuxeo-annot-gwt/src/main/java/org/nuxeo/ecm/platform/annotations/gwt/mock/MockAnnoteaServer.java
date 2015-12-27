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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Writer writer = resp.getWriter();
        writer.write(response.toString());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        InputStream is = req.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String result;
        StringBuilder request = new StringBuilder();
        while ((result = reader.readLine()) != null) {
            request.append(result);
        };
        String r = request.toString();
        String desc = r.substring(r.indexOf(R_DESCRIPTION), r.indexOf(DESCRIPTION) + DESCRIPTION.length());
        response.insert(response.lastIndexOf("</r:RDF>"), desc);
    }

}
