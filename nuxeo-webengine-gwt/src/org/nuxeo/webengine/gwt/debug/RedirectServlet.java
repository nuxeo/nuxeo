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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.webengine.gwt.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RedirectServlet extends HttpServlet implements Debug {

    private static final long serialVersionUID = 1L;


    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {

        StringBuilder buf = new StringBuilder(); // getRequestURL(req);        
        String urlPath = req.getRequestURI();
        if (REDIRECT_PREFIX.equals(urlPath)) {
            urlPath = "/";
        } else {
            urlPath = urlPath.replaceAll(Debug.PATH_PATTERN, Debug.PATH_REPLACEMENT);
        }
        buf.append(req.getMethod()).append(" ").append(urlPath).append(" HTTP/1.0\r\n");

        Enumeration<String> keys = req.getHeaderNames();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            Enumeration<String> vals = req.getHeaders(key);
            while (vals.hasMoreElements()) {
                buf.append(key).append(": ").append(vals.nextElement()).append("\r\n");
            }
        }
        buf.append("\r\n");

        //System.out.println("==============================");
        //System.out.println(buf.toString());

        InputStream rin = null;
        OutputStream rout =null;
        InputStream in = null;
        OutputStream out = null;

        try {
            Socket socket = new Socket(Debug.REDIRECT_HOST, Debug.REDIRECT_PORT);
            rout = socket.getOutputStream();
            rout.write(buf.toString().getBytes());
            in =  req.getInputStream();        
            copy(in, rout);

            rin = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() == 0) {
                    break;
                }
                int p = line.indexOf(':');
                if (p > -1) {
                    resp.setHeader(line.substring(0, p), line.substring(p+1).trim());
                }
            }
            out = resp.getOutputStream();
            copy(rin, out);
        } finally {
            if (rout != null) rout.close();
            if (rin != null) rin.close();
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    
    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024*64];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }


}
