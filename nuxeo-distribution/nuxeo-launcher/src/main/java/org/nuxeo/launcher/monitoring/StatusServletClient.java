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
 * @since 5.5
 */
public class StatusServletClient {

    private static final Log log = LogFactory.getLog(StatusServletClient.class);

    protected static final String URL_PATTERN = "runningstatus";

    protected static final String POST_PARAM = "info";

    protected static final String POST_PARAM_STARTED = "started";

    protected static final String POST_PARAM_SUMMARY = "summary";

    private static final int TIMEOUT = 1000;

    private static final int SUMMARY_TIMEOUT = 2000;

    private URL url;

    private HttpURLConnection server;

    private int timeout;

    private boolean startupFine = false;

    private String key;

    /**
     * Set secure key used for connection
     *
     * @param key server.status.key configured on Server
     */
    public void setKey(String key) {
        this.key = key;
    }

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
        timeout = TIMEOUT;
        return post(POST_PARAM, POST_PARAM_STARTED);
    }

    /**
     * @param postParam
     * @param postParamStarted
     * @return
     * @throws SocketTimeoutException
     */
    private boolean post(String postParam, String postParamStarted)
            throws SocketTimeoutException {
        return post(postParam, postParamStarted, null);
    }

    /**
     * @return true if succeed to connect on StatusServlet
     * @throws SocketTimeoutException
     */
    public boolean init() throws SocketTimeoutException {
        try {
            timeout = TIMEOUT;
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

    protected synchronized void disconnect() {
        if (server != null) {
            server.disconnect();
            server = null;
        }
        notifyAll();
    }

    protected synchronized void connect(String method) throws IOException {
        while (server != null) {
            try {
                wait();
            } catch (InterruptedException e) {
                // do nothing
            }
        }
        server = (HttpURLConnection) url.openConnection();
        server.setConnectTimeout(timeout);
        server.setReadTimeout(timeout);
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
        timeout = SUMMARY_TIMEOUT;
        StringBuilder sb = new StringBuilder();
        startupFine = post(POST_PARAM, POST_PARAM_SUMMARY, sb);
        return sb.toString();
    }

    protected boolean post(String param, String value, StringBuilder response)
            throws SocketTimeoutException {
        String post = param + "=" + value;
        post += "&key=" + key;
        try {
            connect("POST");
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    server.getOutputStream()));
            bw.write(post, 0, post.length());
            bw.close();
            return getResponse(response);
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (IOException e) {
            return false;
        } finally {
            disconnect();
        }
    }

    protected boolean getResponse(StringBuilder response) throws IOException {
        String line;
        boolean answer;
        BufferedReader s = null;
        try {
            s = new BufferedReader(new InputStreamReader(
                    server.getInputStream()));
            // First line is a status (true or false)
            answer = Boolean.parseBoolean(s.readLine());
            // Next (if exists) is a response body
            while ((line = s.readLine()) != null) {
                response.append(line + "\n");
            }
        } catch (IOException e) {
            throw e;
        } finally {
            IOUtils.closeQuietly(s);
        }
        return answer;
    }

    /**
     * Return detected status of Nuxeo server by last call to
     * {@link #getStartupSummary()}
     *
     * @return true if everything is fine; false is there was any error or
     *         status is unknown
     */
    public boolean isStartupFine() {
        return startupFine;
    }
}
