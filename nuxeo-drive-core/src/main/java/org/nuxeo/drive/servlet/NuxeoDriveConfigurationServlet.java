/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.servlet;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.nuxeo.common.Environment;

/**
 * Servlet that retrieves the Nuxeo Drive global configuration.
 *
 * @since 8.10-HF20
 * @implNote The configuration file is expected to be in the server's configuration folder, copied from the
 *           {@code drive} template.
 */
public class NuxeoDriveConfigurationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String NUXEO_DRIVE_CONFIGURATION_FILE = "nuxeo-drive-config.json";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        File configurationFolder = Environment.getDefault().getConfig();
        File configurationFile = new File(configurationFolder, NUXEO_DRIVE_CONFIGURATION_FILE);
        if (!configurationFile.exists()) {
            resp.sendError(SC_NOT_FOUND,
                    String.format(
                            "Could not find the %s file in the server's configuration folder,"
                                    + " make sure the nuxeo-drive package is up-to-date"
                                    + " and you server is configured to use the \"drive\" template.",
                            NUXEO_DRIVE_CONFIGURATION_FILE));
            return;
        }

        try (InputStream in = new FileInputStream(configurationFile)) {
            resp.setContentType("application/json");
            @SuppressWarnings("resource")
            OutputStream out = resp.getOutputStream(); // not ours to close
            IOUtils.copy(in, out);
            out.flush();
            resp.flushBuffer();
        }
    }

}
