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

package org.nuxeo.ecm.webengine.client.http;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Request {

    protected String method;
    protected String schema;
    protected String username;
    protected String password;
    protected String host;
    protected int port = 80;
    protected String path;
    protected StringBuilder query;
    protected String fragment;
    protected Map<String,String> headers;

    protected String enctype;
    protected Map<String,Object> data;

    public Request method(String method) {
        this.method = method;
        return this;
    }

    public Request get() {
        return method("GET");
    }

    public Request get(String url) throws MalformedURLException {
        return get(new URL(url));
    }

    public Request get(URL url) {
        return get().url(url);
    }

    public Request post() {
        return method("POST");
    }

    public Request post(String url) throws MalformedURLException {
        return post(new URL(url));
    }

    public Request post(URL url) {
        return post().url(url);
    }

    public Request put() {
        return method("PUT");
    }

    public Request put(String url) throws MalformedURLException {
        return put(new URL(url));
    }

    public Request put(URL url) {
        return put().url(url);
    }

    public Request delete() {
        return method("DELETE");
    }

    public Request delete(String url) throws MalformedURLException {
        return delete(new URL(url));
    }

    public Request delete(URL url) {
        return delete().url(url);
    }

    public Request head() {
        return method("HEAD");
    }

    public Request head(URL url) {
        return head().url(url);
    }

    public Request head(String url) throws MalformedURLException {
        return head(new URL(url));
    }


    public Request url(String url) throws MalformedURLException {
        return url(new URL(url));
    }

    public Request url(URL url) {
        this.schema = url.getProtocol();
        this.host = url.getHost();
        this.port = url.getPort();
        if (port == -1) {
            port = url.getDefaultPort();
        }
        this.path = url.getPath();
        this.fragment = url.getRef();
        String q = url.getQuery();
        if (q != null) {
            this.query = new StringBuilder(q);
        }
        String auth = url.getUserInfo();
        if (auth != null) {
            int k = auth.indexOf(':');
            if (k > -1) {
                this.username = auth.substring(0, k);
                this.password = auth.substring(k+1);
            } else {
                this.username = auth;
            }
        }
        return this;
    }




    public static void main(String[] args) throws Exception {
        //URL url = new URL("http://u:p@localhost:8080/the/path?k1=v1&k2=v2#anchor");
        URL url = new URL("http://u:p@localhost/the/path?k1=v1&k2=v2#anchor");
        System.out.println("> schema: "+url.getProtocol());
        System.out.println("> host: "+url.getHost());
        System.out.println("> port: "+url.getPort()+" - "+url.getDefaultPort());
        System.out.println("> query: "+url.getQuery());
        System.out.println("> fragment: "+url.getRef());
        System.out.println("> user: "+url.getUserInfo());
    }
}
