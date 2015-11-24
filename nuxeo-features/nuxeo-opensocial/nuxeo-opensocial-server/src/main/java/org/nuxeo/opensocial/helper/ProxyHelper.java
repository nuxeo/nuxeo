/*
 * (C) Copyright 2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.opensocial.helper;

import static org.nuxeo.launcher.config.Environment.NUXEO_LOOPBACK_URL;
import static org.nuxeo.launcher.config.Environment.OPENSOCIAL_GADGETS_HOST;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.common.Environment;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper class to provide easy proxy configuration for OpenSocial using default
 * Nuxeo properties.
 *
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */

public class ProxyHelper {

    private static final Log log = LogFactory.getLog(ProxyHelper.class);

    protected ProxyHelper() {
        // Nothing to do, helper class
    }

    public static void fillProxy(HttpClient httpClient, String requestUri) {
        if (useProxy(requestUri)) {
            log.info(String.format("Configuring proxy for request: %s",
                    requestUri));
            String proxyHost = getProxyHost();
            int proxyPort = getProxyPort();
            log.debug(String.format("Using proxy: %s:%d", proxyHost, proxyPort));
            httpClient.getHostConfiguration().setProxy(proxyHost, proxyPort);
            if (useAuthenticated()) {
                log.debug(String.format("Using credentials: %s:%s",
                        getProxyLogin(), getProxyPassword()));
                httpClient.getState().setProxyCredentials(
                        new AuthScope(proxyHost, proxyPort, null),
                        new UsernamePasswordCredentials(getProxyLogin(),
                                getProxyPassword()));
            }
        }
    }

    protected static boolean useAuthenticated() {
        return !StringUtils.isEmpty(getProxyLogin());
    }

    protected static boolean useProxy(String requestUri) {
        Boolean useProxy = !StringUtils.isBlank(getProxyHost());
        useProxy &= !requestUri.contains(Framework.getProperty(NUXEO_LOOPBACK_URL));
        useProxy &= !requestUri.contains(Framework.getProperty(OPENSOCIAL_GADGETS_HOST));
        return useProxy;
    }

    protected static int getProxyPort() {
        return Integer.parseInt(Framework.getProperty(Environment.NUXEO_HTTP_PROXY_PORT));
    }

    protected static String getProxyHost() {
        return Framework.getProperty(Environment.NUXEO_HTTP_PROXY_HOST);
    }

    protected static String getProxyPassword() {
        return Framework.getProperty(Environment.NUXEO_HTTP_PROXY_PASSWORD);
    }

    protected static String getProxyLogin() {
        return Framework.getProperty(Environment.NUXEO_HTTP_PROXY_LOGIN);
    }
}
