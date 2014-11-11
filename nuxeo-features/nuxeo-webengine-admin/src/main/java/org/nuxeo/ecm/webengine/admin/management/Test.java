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
package org.nuxeo.ecm.webengine.admin.management;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

import org.nuxeo.common.utils.FileUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Test {

    public static String encoding;

    public static void main(String[] args) throws Exception {
        File file = new File("/Users/bstefanescu/testc.txt");
        File cfile = new File("/Users/bstefanescu/MyListener.class");

        String userPassword = "Administrator" + ":" + "Administrator";
        encoding = new sun.misc.BASE64Encoder().encode (userPassword.getBytes());


        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication ("Administrator", "Administrator".toCharArray());
            }
        });

        //deleteData("http://localhost:8080/server/components/MyListener");

        //postBinary("http://localhost:8080/server/resources?file=org/nuxeo/runtime/MyListener.class", new FileInputStream(cfile));
        //postXml("http://localhost:8080/server/components", FileUtils.readFile(file));

        deleteData("http://localhost:8080/server/components/test-listener");
        postBinary("http://localhost:8080/server/resources?file=script/listener.groovy",
                new FileInputStream(new File("/Users/bstefanescu/listener.groovy")));
        postXml("http://localhost:8080/server/components",
                FileUtils.readFile(new File("/Users/bstefanescu/test-listeners.xml")));
    }

    public static void deleteData(String url) throws Exception {
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection)u.openConnection();
        conn.setRequestProperty ("Authorization", "Basic " + encoding);
        conn.setDoOutput(false);
        conn.setRequestMethod("DELETE");
        int r = conn.getResponseCode();
        System.out.println("response: "+r);
    }

    public static void postXml(String url, String data) throws Exception {
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection)u.openConnection();
        conn.setRequestProperty ("Authorization", "Basic " + encoding);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "text/xml");
        conn.setRequestProperty("Content-Length", ""+data.length());
        conn.setRequestMethod("POST");
        conn.getOutputStream().write(data.getBytes());
        conn.getOutputStream().close();
        int r = conn.getResponseCode();
        System.out.println("response: "+r);
    }

    public static void postBinary(String url, InputStream in) throws Exception {
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection)u.openConnection();
        conn.setRequestProperty ("Authorization", "Basic " + encoding);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setRequestMethod("POST");
        FileUtils.copy(in, conn.getOutputStream());
        conn.getOutputStream().close();
        int r = conn.getResponseCode();
        System.out.println("response: "+r);
        if (r >= 400) {
            FileUtils.copy(conn.getInputStream(), System.out);
        }
    }

}
