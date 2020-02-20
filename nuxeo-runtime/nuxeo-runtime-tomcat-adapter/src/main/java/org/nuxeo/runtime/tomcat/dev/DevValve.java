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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.tomcat.dev;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Enable remote hot deploy and getting configuration from remote Nuxeo SDK servers
 * <p>
 * This valve is enabled only in SDK profile (i.e. dev mode). It will intercept any call to '/sdk' under the context
 * path (i.e. /nuxeo/sdk)
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DevValve extends ValveBase {

    Log log = LogFactory.getLog(DevValve.class);

    @Override
    public void invoke(Request req, Response resp) throws IOException, ServletException {

        String path = req.getServletPath();
        if (path != null && path.startsWith("/sdk/")) {
            path = path.substring("/sdk/".length());
            if ("reload".equals(path)) {
                if ("GET".equals(req.getMethod())) {
                    getReload(req, resp);
                } else if ("POST".equals(req.getMethod())) {
                    postReload(req, resp);
                }
                return;
            } else if (path.startsWith("files/")) {
                path = path.substring("files/".length());
                getFile(path, req, resp);
                return;
            }
            resp.setStatus(404);
            return;
        }
        getNext().invoke(req, resp);
    }

    private final File getHome() {
        return new File(System.getProperty("catalina.base"));
    }

    private final File getSDKFile(String path) {
        return new File(new File(getHome(), "sdk"), path);
    }

    private void getFile(String path, Request req, Response resp) {
        File file = getSDKFile(path);
        if (!file.isFile()) {
            resp.setStatus(404);
        } else {
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("UTF-8");
            resp.setStatus(200);
            try {
                @SuppressWarnings("resource") // not ours to close
                Writer out = resp.getWriter();
                sendFile(file, out);
                out.flush();
            } catch (IOException e) {
                resp.setStatus(500);
                log.error("Failed to send file: " + file, e);
            }
        }
    }

    private void sendFile(File file, Writer out) throws IOException {
        Reader in = new InputStreamReader(new FileInputStream(file), "UTF-8");
        try {
            char cbuf[] = new char[64 * 1024];
            int r = -1;
            while ((r = in.read(cbuf)) != -1) {
                if (r > 0) {
                    out.write(cbuf, 0, r);
                }
            }
        } finally {
            in.close();
        }
    }

    private void getReload(Request req, Response resp) {
        ClassLoader webLoader = req.getContext().getLoader().getClassLoader();
        if (webLoader instanceof NuxeoDevWebappClassLoader) {
            NuxeoDevWebappClassLoader loader = (NuxeoDevWebappClassLoader) webLoader;
            // only if dev.bundles was modified
            loader.getBootstrap().loadDevBundles();
            // log.error("###### reloaded dev bundles");
        }
        resp.setStatus(200);
    }

    private void postReload(Request req, Response resp) throws IOException {
        ClassLoader webLoader = req.getContext().getLoader().getClassLoader();
        if (webLoader instanceof NuxeoDevWebappClassLoader) {
            NuxeoDevWebappClassLoader loader = (NuxeoDevWebappClassLoader) webLoader;
            DevFrameworkBootstrap bootstrap = loader.getBootstrap();
            String devBundlesLocation = bootstrap.getDevBundlesLocation();
            try {
                Files.copy(req.getStream(), Paths.get(devBundlesLocation), StandardCopyOption.REPLACE_EXISTING);
                // only if dev.bundles was modified
                bootstrap.loadDevBundles();
                resp.setStatus(200);
            } catch (IOException e) {
                log.error("Unable to write to dev.bundles", e);
                resp.sendError(500, "Unable to write to dev.bundles");
            } catch (RuntimeException e) {
                log.error("Unable to reload dev.bundles", e);
                resp.sendError(500, "Unable to reload dev.bundles");
            }
        }
    }

}
