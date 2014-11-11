/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.tomcat.dev;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Enable remote hot deploy and getting configuration from remote Nuxeo SDK
 * servers
 * <p>
 * This valve is enabled only in SDK profile (i.e. dev mode). It will intercept
 * any call to '/sdk' under the context path (i.e. /nuxeo/sdk)
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class DevValve extends ValveBase {

    Log log = LogFactory.getLog(DevValve.class);

    @Override
    public void invoke(Request req, Response resp) throws IOException,
            ServletException {

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
        if (file == null) {
            resp.setStatus(404);
        } else {
            resp.setContentType("text/plain");
            resp.setCharacterEncoding("UTF-8");
            resp.setStatus(200);
            try {
                Writer out = resp.getWriter();
                sendFile(file, out);
                out.flush();
            } catch (Exception e) {
                resp.setStatus(500);
                log.error("Failed to send file: " + file, e);
            }
        }
    }

    private void sendFile(File file, Writer out) throws IOException {
        Reader in = new InputStreamReader(new FileInputStream(file), "UTF-8");
        char cbuf[] = new char[64 * 1024];
        int r = -1;
        while ((r = in.read(cbuf)) != -1) {
            if (r > 0) {
                out.write(cbuf, 0, r);
            }
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

    private void postReload(Request req, Response resp) {
        log.error("#### TODO: POST RELOAD");
        resp.setStatus(200);
    }

}
