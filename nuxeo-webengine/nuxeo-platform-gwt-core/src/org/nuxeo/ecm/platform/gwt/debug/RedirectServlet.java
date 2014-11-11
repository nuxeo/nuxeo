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

package org.nuxeo.ecm.platform.gwt.debug;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
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

    protected static Pattern HTTP_RESP = Pattern.compile("HTTP/1\\..\\s+([0-9]+)\\s+(.*)");
    
    protected String  redirectPrefix = REDIRECT_PREFIX;
    protected String  redirectHost = REDIRECT_HOST;
    protected int  redirectPort = REDIRECT_PORT;
    protected String redirectPattern = REDIRECT_PATTERN;
    protected String redirectReplacement = REDIRECT_REPLACEMENT;
    protected boolean trace = REDIRECT_TRACE;
    protected boolean traceContent = REDIRECT_TRACE_CONTENT;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String val = null;
        val = System.getProperty("redirect.prefix");
        if (val != null) {
            redirectPrefix = val;
        }
        val = System.getProperty("redirect.host");
        if (val != null) {
            redirectHost = val;
        }        
        val = System.getProperty("redirect.port");
        if (val != null) {
            redirectPort = Integer.parseInt(val);
        }        
        val = System.getProperty("redirect.pattern");
        if (val != null) {
            redirectPattern = val;
        }        
        val = System.getProperty("redirect.replacement");
        if (val != null) {
            redirectReplacement = val;
        }
        val = System.getProperty("redirect.trace");
        if (val != null) {
            trace = Boolean.parseBoolean(val);
        }
        val = System.getProperty("redirect.trace.content");
        if (val != null) {
            traceContent = Boolean.parseBoolean(val);
        }
        System.out.println("----------------------------------------------------------");
        System.out.println("Redirect Servlet Enabled: ");
        System.out.println("redirect.prefix: "+redirectPrefix);
        System.out.println("redirect.host: "+redirectHost);
        System.out.println("redirect.port: "+redirectPort);
        System.out.println("redirect.pattern: "+redirectPattern);
        System.out.println("redirect.replacement: "+redirectReplacement);
        System.out.println("redirect.trace: "+trace);
        System.out.println("----------------------------------------------------------");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {        
// TODO used to test loading... progress bar
//        if (true) {
//            try {Thread.sleep(3000);} catch (Exception e) {}
//        }
        
        StringBuilder buf = new StringBuilder(); // getRequestURL(req);        
        String urlPath = req.getRequestURI();
        if (redirectPrefix.equals(urlPath)) {
            urlPath = "/";
        } else {
            urlPath = urlPath.replaceAll(redirectPattern, redirectReplacement);
        }
        buf.append(req.getMethod()).append(" ").append(urlPath).append(" HTTP/1.0\r\n");

        Enumeration<String> keys = req.getHeaderNames();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            Enumeration<String> vals = req.getHeaders(key);
            while (vals.hasMoreElements()) {
                if (!"connection".equalsIgnoreCase(key)) { // skip Connection: Keep-Alive
                    buf.append(key).append(": ").append(vals.nextElement()).append("\r\n");
                } else {
                    vals.nextElement();
                }
            }
        }
        buf.append("\r\n");

        InputStream rin = null;
        OutputStream rout =null;
        InputStream in = null;

        try {
            Socket socket = new Socket(redirectHost, redirectPort);
            rout = socket.getOutputStream();
            rout.write(buf.toString().getBytes());
            in =  req.getInputStream();
            if (trace) {
                traceln("========== HTTP REQUEST ===========");
                traceln(buf.toString());
                traceln("");
                copyDebug(in, rout);
                traceln("===================================");
            } else {
                copy(in, rout);
            }
            rout.flush();
            rin = new BufferedInputStream(socket.getInputStream());
            if (trace) {
                traceln("========= HTTP RESPONSE ===========");
            }
            transferResponse(rin, resp);
            if (trace) {
                traceln("===================================");
            }
        } finally {
            if (rout != null) rout.close();
            if (rin != null) rin.close();
            if (in != null) in.close();
        }
    }

    
    public void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024*64];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public void copyDebug(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024*64];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
            if (traceContent) {
                trace(new String(buffer, 0, read));
            }
        }
        traceln("");
    }


    public int getStatusCode(String line) {
        Matcher matcher = HTTP_RESP.matcher(line.trim());
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return -1;
    }
        

    public void transferResponse(InputStream in, HttpServletResponse resp) throws IOException {
        int cnt = 0;
        StringBuilder buf = new StringBuilder(); 
        while (true) {
            int c = in.read();
            if (c == -1) {
                break;
            }
            if (c == '\r') {
                continue;
            }
            if (c == '\n') {
                if (buf.length() > 0) {
                    if (cnt == 0) { // the first header
                        int status = getStatusCode(buf.toString().trim());
                        if (status == -1) {
                            throw new IOException("Bug in RedirectServlet?");
                        }          
                        if (trace) {
                            traceln(buf.toString());
                        }
                        if (status >= 400) {
                            resp.sendError(status);
                            return;
                        } 
                        resp.setStatus(status);                        
                    } else {
                        setHeader(buf.toString().trim(), resp);
                    }
                    buf.setLength(0);
                    cnt++;
                } else {
                    break;
                }
            } else {
                buf.append((char)c);
            }
        }
                
        OutputStream out = resp.getOutputStream();
        try {
            if (trace) {
                traceln("");
                copyDebug(in, out);
            } else {
                copy(in, out);
            }
        } finally {
            out.flush();
            out.close();
        }
    }
    
    protected void setHeader(String header, HttpServletResponse resp) {
        if (trace) {
            traceln(header);
        }
        int p = header.indexOf(':');
        if (p > -1) {
            resp.setHeader(header.substring(0, p), header.substring(p+1).trim());
        }
    }

    public static void traceln(String str) {
        System.out.println(str);
    }
    public static void trace(String str) {
        System.out.print(str);
    }
    
    public static void main(String[] args) {
        Matcher m = HTTP_RESP.matcher("HTTP/1.1 404 Not Found");
        if (m.matches()) {
            System.out.println(m.group(1));
            System.out.println(m.group(2));
        }
    }
    

}
