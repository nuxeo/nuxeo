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

package org.nuxeo.runtime.remoting;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.jboss.remoting.InvokerLocator;

/**
 * Helps to create locators for nuxeo runtime nodes.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LocatorHelper {

    public static final int DEFAULT_PORT = 62474;
    private static final String ANY = "0.0.0.0";
    private static final String SERVER_BIND_ADDRESS = "jboss.bind.address";

    // Utility class.
    private LocatorHelper() {
    }

    public static InvokerLocator getLocator(String protocol, String host,
            int port, String path, Map<String, String> parameters) {
        if (protocol == null) {
            protocol = "socket";
        }
        if (port <= 0) {
            port = DEFAULT_PORT;
        }
        if (parameters == null) {
            parameters = new HashMap<String, String>();
            parameters.put(InvokerLocator.DATATYPE, "nuxeo");
        } else if (!"nuxeo".equals(parameters.get(InvokerLocator.DATATYPE))) {
            parameters.put(InvokerLocator.DATATYPE, "nuxeo");
        }
        return new InvokerLocator(protocol, host, port, path, parameters);
    }

    public static InvokerLocator getLocator(String protocol, String host, int port) {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(InvokerLocator.DATATYPE, "nuxeo");
        return new InvokerLocator(protocol, host, port, "/", parameters);
    }

    public static InvokerLocator getLocator(String host, int port) {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(InvokerLocator.DATATYPE, "nuxeo");
        return new InvokerLocator("socket", host, port, "/", parameters);
    }

    public static InvokerLocator getLocator(String host) {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put(InvokerLocator.DATATYPE, "nuxeo");
        return new InvokerLocator("socket", host, 62474, "/", parameters);
    }

    /**
     * Constructs the object used to identify a remoting server via simple uri
     * format string (e.g. socket://myhost:7000).
     * <p>
     * Note: the uri passed may not always be the one returned via call to
     * getLocatorURI() as may need to change if port not specified, host is
     * 0.0.0.0, etc. If need original uri that is passed to this constructor,
     * need to call getOriginalURI().
     */
    public static InvokerLocator parse(String uri) throws MalformedURLException {
        String protocol;
        String host;
        String path;
        int port;
        Map<String, String> parameters = null;

        int i = uri.indexOf("://");
        if (i < 0) {
            throw new MalformedURLException("Invalid url " + uri);
        }
        String tmp = uri.substring(i + 3);
        protocol = uri.substring(0, i);
        i = tmp.indexOf("/");
        int p = tmp.lastIndexOf(":");
        if (p != -1) {
            host = resolveHost(tmp.substring(0, p).trim());
            if (i > -1) {
                port = Integer.parseInt(tmp.substring(p + 1, i));
            } else {
                port = Integer.parseInt(tmp.substring(p + 1));
            }
        } else {
            if (i > -1) {
                host = resolveHost(tmp.substring(0, i).trim());
            } else {
                host = resolveHost(tmp.substring(0).trim());
            }
            port = -1;
        }

        // now look for any path
        p = tmp.indexOf("?");
        if (p != -1) {
            path = tmp.substring(i + 1, p);
            String args = tmp.substring(p + 1);
            StringTokenizer tok = new StringTokenizer(args, "&");
            parameters = new HashMap<String, String>();
            while (tok.hasMoreTokens()) {
                String token = tok.nextToken();
                int eq = token.indexOf("=");
                String name = (eq > -1) ? token.substring(0, eq) : token;
                String value = (eq > -1) ? token.substring(eq + 1) : "";
                parameters.put(name, value);
            }
        } else {
            p = tmp.indexOf("/");
            if (p != -1) {
                path = tmp.substring(p + 1);
            } else {
                path = "";
            }
        }

        // add nuxeo parameters if none
        if (parameters == null) {
            parameters = new HashMap<String, String>();
            parameters.put(InvokerLocator.DATATYPE, "nuxeo");
        } else if (!"nuxeo".equals(parameters.get(InvokerLocator.DATATYPE))) {
            parameters.put(InvokerLocator.DATATYPE, "nuxeo");
        }
        return new InvokerLocator(protocol, host, port, path, parameters);
    }

    public static String resolveHost(String host) {
        if (host.contains("0.0.0.0")) {
            if (System.getProperty(SERVER_BIND_ADDRESS, "0.0.0.0").equals(
                    "0.0.0.0")) {
                host = fixRemoteAddress(host);
            } else {
                host = host.replaceAll("0\\.0\\.0\\.0",
                        System.getProperty(SERVER_BIND_ADDRESS));
            }
        }
        try {
            return InetAddress.getByName(host).getHostAddress();
        } catch (Exception ex) {
            return host;
        }
    }

    public static String fixRemoteAddress(String address) {
        try {
            if (address == null || ANY.equals(address)) {
                boolean byHost = true;
                String bindByHost = System.getProperty(
                        InvokerLocator.BIND_BY_HOST, "True");
                try {
                    byHost = Boolean.getBoolean(bindByHost);
                } catch (Exception e) {
                }
                if (byHost) {
                    return InetAddress.getLocalHost().getHostName();
                } else {
                    return InetAddress.getLocalHost().getHostAddress();
                }
            }
        } catch (UnknownHostException ignored) {
        }
        return address;
    }

}
