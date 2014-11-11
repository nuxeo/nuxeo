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
 */
package org.nuxeo.ecm.webengine.client.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.nuxeo.ecm.client.Path;
import org.nuxeo.ecm.webengine.client.Client;
import org.nuxeo.ecm.webengine.client.Console;
import org.nuxeo.ecm.webengine.client.Result;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JdkHttpClient extends Client {

    public JdkHttpClient(String baseUrl) throws IOException {
        super (baseUrl);
    }

    public JdkHttpClient(URL baseUrl) throws IOException {
        super (baseUrl);
    }

    public JdkHttpClient(String baseUrl, String username, String password) throws IOException {
        super (baseUrl, username, password);
    }

    public JdkHttpClient(String baseUrl, String path, String username, String password) throws IOException {
        super (baseUrl, path, username, password);
    }

    public JdkHttpClient(URL baseUrl, String path, String username, String password) throws IOException {
        super (baseUrl, path, username, password);
    }



    public Result execute(HttpURLConnection conn, String cmdId) throws Exception {
        InputStream in = null;
        Result res = new Result(cmdId, conn);
        //res.
        int ret = conn.getResponseCode();
        if (ret == 200) {
            in = conn.getInputStream();
            Console.getDefault().println(in);
        }
        return res;
    }

    @Override
    public Result get(Path path, Map<String,Object> args) {
//        path = getPath(path);
//        URL url = null;
//        if ("GET".equals("GET")) {
//            url = buildUrl(path, args);
//        }
//        InputStream in = null;
//        try {
//            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
//            conn.setRequestMethod("GET");
//            conn.setDoInput(true);
//            //TODO if we need to upload files ...
//            //conn.setDoOutput(true);
//            if (username != null) {
//                String authorizationString = "Basic " + Base64.encode(username+":"+password);
//                conn.setRequestProperty ("Authorization", authorizationString);
//            }
//            conn.setRequestProperty("Accept", "text/plain");
//            return execute(conn, cmdId);
//            int ret = conn.getResponseCode();
//            if (ret == 200) {
//                in = conn.getInputStream();
//                Console.getDefault().println(in);
//            }
//            return ret;
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (in != null) { try { in.close(); } catch (IOException e) {e.printStackTrace();} }
//        }
        return null; // internal error
    }

    @Override
    public void onConnect() {

    }

    @Override
    public void onDisconnect() {

    }


    public static URL getURL(URL url, String args) {
        return  null;
    }

    @Override
    public Result doDelete(Path path, Map<String, Object> args) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result doGet(Path path, Map<String, Object> args) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result doHead(Path path, Map<String, Object> args) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result doPost(Path path, Map<String, Object> args) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Result doPut(Path path, Map<String, Object> args) {
        // TODO Auto-generated method stub
        return null;
    }



}
