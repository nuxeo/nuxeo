/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.launcher.monitoring;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.launcher.config.ConfigurationGenerator;

/**
 * HTTP client monitoring Nuxeo server starting status
 *
 * @see org.nuxeo.ecm.core.management.statuses.StatusServlet
 * @since 5.4.3
 */
public class StatusServletClient {

    private static final Log log = LogFactory.getLog(StatusServletClient.class);

    protected static final String URL_PATTERN = "runningstatus";

    protected static final String POST_PARAM = "info";

    protected static final String POST_PARAM_STARTED = "started";

    protected static final String POST_PARAM_SUMMARY = "summary";

    private static final int TIMEOUT = 1000;

    private URL url;

    private HttpURLConnection server;

    private boolean isFine = false;

    /**
     * @param configurationGenerator
     */
    public StatusServletClient(ConfigurationGenerator configurationGenerator) {
        final String servletURL = configurationGenerator.getUserConfig().getProperty(
                ConfigurationGenerator.PARAM_LOOPBACK_URL)
                + "/" + URL_PATTERN;
        try {
            url = new URL(servletURL);
        } catch (MalformedURLException e) {
            log.error("Malformed URL: " + servletURL, e);
        }
    }

    /**
     * @return true if Nuxeo finished starting
     * @throws SocketTimeoutException
     */
    public boolean isStarted() throws SocketTimeoutException {
        post(POST_PARAM, POST_PARAM_STARTED);
        return isFine;
    }

    /**
     * @return true if succeed to connect on StatusServlet
     * @throws SocketTimeoutException
     */
    public boolean init() throws SocketTimeoutException {
        try {
            connect("GET");
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException e) {
            return false;
        } finally {
            disconnect();
        }
        return true;
    }

    protected void disconnect() {
        if (server != null) {
            server.disconnect();
        }
    }

    protected void connect(String method) throws IOException {
        server = (HttpURLConnection) url.openConnection();
        server.setConnectTimeout(TIMEOUT);
        server.setReadTimeout(TIMEOUT);
        server.setDoInput(true);
        server.setDoOutput(true);
        server.setRequestMethod(method);
        server.setRequestProperty("Content-type",
                "application/x-www-form-urlencoded");
        server.connect();
    }

    /**
     * @return Nuxeo server startup summary (components loading status)
     * @throws SocketTimeoutException
     */
    public String getStartupSummary() throws SocketTimeoutException {
        return post(POST_PARAM, POST_PARAM_SUMMARY);
    }

    protected String post(String param, String value)
            throws SocketTimeoutException {
        String post = param + "=" + value;
        try {
            connect("POST");
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    server.getOutputStream()));
            bw.write(post, 0, post.length());
            bw.close();
            return getResponse();
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException e) {
            return null;
        } finally {
            disconnect();
        }
    }

    protected String getResponse() throws IOException {
        String line;
        StringBuffer sb = new StringBuffer();
        BufferedReader s = null;
        try {
            s = new BufferedReader(new InputStreamReader(
                    server.getInputStream()));
            // First line is a status (true or false)
            isFine = Boolean.parseBoolean(s.readLine());
            while ((line = s.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            throw e;
        } finally {
            IOUtils.closeQuietly(s);
        }
        return sb.toString();
    }

    /**
     * Return last detected status of Nuxeo server
     *
     * @return true if everything is fine; false is there was any error
     */
    public boolean isFine() {
        return isFine;
    }
}
