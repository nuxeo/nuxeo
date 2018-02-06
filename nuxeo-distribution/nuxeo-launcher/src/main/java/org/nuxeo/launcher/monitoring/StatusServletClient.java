/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * @param key any {@link String}
     */
    public void setKey(String key) {
        this.key = key;
    }

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
     */
    public boolean isStarted() throws SocketTimeoutException {
        timeout = TIMEOUT;
        return post(POST_PARAM, POST_PARAM_STARTED);
    }

    private boolean post(String postParam, String postParamStarted) throws SocketTimeoutException {
        return post(postParam, postParamStarted, null);
    }

    /**
     * @return true if succeed to connect on StatusServlet
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
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        server = (HttpURLConnection) url.openConnection();
        server.setConnectTimeout(timeout);
        server.setReadTimeout(timeout);
        server.setDoInput(true);
        server.setDoOutput(true);
        server.setRequestMethod(method);
        server.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
        server.connect();
    }

    /**
     * @return Nuxeo server startup summary (components loading status)
     */
    public String getStartupSummary() throws SocketTimeoutException {
        timeout = SUMMARY_TIMEOUT;
        StringBuilder sb = new StringBuilder();
        startupFine = post(POST_PARAM, POST_PARAM_SUMMARY, sb);
        return sb.toString();
    }

    protected boolean post(String param, String value, StringBuilder response) throws SocketTimeoutException {
        String post = param + "=" + value;
        post += "&key=" + key;
        try {
            connect("POST");
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()))) {
                bw.write(post, 0, post.length());
            }
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
        try (BufferedReader s = new BufferedReader(new InputStreamReader(server.getInputStream()))) {
            // First line is a status (true or false)
            boolean answer = Boolean.parseBoolean(s.readLine());
            String line;
            // Next (if exists) is a response body
            while ((line = s.readLine()) != null) {
                if (response != null) {
                    response.append(line).append('\n');
                }
            }
            return answer;
        }
    }

    /**
     * Return detected status of Nuxeo server by last call to {@link #getStartupSummary()}
     *
     * @return true if everything is fine; false is there was any error or status is unknown
     */
    public boolean isStartupFine() {
        return startupFine;
    }
}
