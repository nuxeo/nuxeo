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
package org.nuxeo.ecm.webengine.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;

import org.nuxeo.ecm.webengine.client.util.FileUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Result {

    protected String cmdId;

    protected int status;

    protected String contentType;

    protected int contentLength;

    protected InputStream in;


    public Result(String cmdId, HttpURLConnection conn) throws IOException {
        this (cmdId, conn, true);
    }
    public Result(String cmdId, HttpURLConnection conn, boolean hasContent) throws IOException {
        this.cmdId = cmdId;
        status = conn.getResponseCode();
        if (hasContent) {
            contentType = conn.getContentType();
            contentLength = conn.getContentLength();
            in = conn.getInputStream();
        }
    }

    public boolean hasContent() {
        return in != null;
    }

    public void close() {
        try {
            in.close();
            in = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getType() {
        return contentType;
    }

    public int getStatus() {
        return status;
    }

    public boolean isOk() {
        return status == 200;
    }


    public String getContentAsString() throws IOException {
        return FileUtils.read(in);
    }

    public List<String> getContentLines() throws IOException {
        return FileUtils.readLines(in);
    }

}
