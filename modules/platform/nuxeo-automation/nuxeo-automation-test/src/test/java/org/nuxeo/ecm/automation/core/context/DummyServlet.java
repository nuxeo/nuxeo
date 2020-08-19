/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.automation.core.context;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.runtime.api.Framework;

/**
 * since 11.3
 */
public class DummyServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();

        switch (path) {
        case "/string":
            resp.getWriter().write("dummy");
            resp.getWriter().flush();
            break;
        case "/stringblob":
            downloadBlob(req, resp, Blobs.createBlob("dummy string blob"));
            break;
        case "/blob":
            File docFile = FileUtils.getResourceFileFromContext("hello.doc");
            downloadBlob(req, resp, Blobs.createBlob(docFile));
            break;
        default:
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    protected void downloadBlob(HttpServletRequest req, HttpServletResponse resp, Blob blob) throws IOException {
        var context = DownloadService.DownloadContext.builder(req, resp).blob(blob).build();
        Framework.getService(DownloadService.class).downloadBlob(context);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo();
        if ("/post".equals(path)) {
            String body = req.getReader().lines().collect(Collectors.joining());
            resp.getWriter().write(body);
            resp.getWriter().flush();
        } else {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

}
