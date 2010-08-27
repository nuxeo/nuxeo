/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.opensocial.service.impl;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

import edu.emory.mathcs.backport.java.util.Arrays;

public class SimpleProxySelector extends ProxySelector {

    private static final String SHINDIG_PROXY_PROXY_PORT = "shindig.proxy.proxyPort";

    private static final String SHINDIG_PROXY_PROXY_HOST = "shindig.proxy.proxyHost";

    private static final String SHINDIG_PROXY_PROXY_SET = "shindig.proxy.proxySet";

    private static final String SHINDIG_PROXY_PASSWORD = "shindig.proxy.password";

    private static final String SHINDIG_PROXY_USER = "shindig.proxy.user";

    private static final String SHINDIG_PROXY_EXCLUDE = "shindig.proxy.excludeHost";

    private static final Log log = LogFactory.getLog(SimpleProxySelector.class);

    private Proxy proxySettings = null;

    List<String> excludedHosts = new ArrayList<String>();

    @SuppressWarnings("unchecked")
    public SimpleProxySelector() {
        String excludedHostsProperty = Framework.getProperty(SHINDIG_PROXY_EXCLUDE);
        if (excludedHostsProperty != null) {
            String[] hosts = excludedHostsProperty.split(",");
            if (hosts.length > 0) {
                excludedHosts.addAll(Arrays.asList(hosts));
            }
        }
        excludedHosts.add("localhost");
        excludedHosts.add("127.0.0.1");

    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        // Do nothing

    }

    @Override
    public List<Proxy> select(URI uri) {
        List<Proxy> proxies = new ArrayList<Proxy>();

        boolean proxy = true;

        for (String host : excludedHosts) {
            if (uri.getHost().endsWith(host)) {
                proxy = false;
            }
        }

        if (!proxy) {
            proxies.add(Proxy.NO_PROXY);
        } else {
            proxies.add(getProxySettings());
        }
        return proxies;

    }

    private Proxy getProxySettings() {
        try {
            if (Framework.isInitialized() && isProxySet()) {
                if (proxySettings == null) {
                    setAuthenticator();
                    proxySettings = new Proxy(
                            Proxy.Type.HTTP,
                            new InetSocketAddress(
                                    Framework.getProperty(SHINDIG_PROXY_PROXY_HOST),
                                    Integer.parseInt(Framework.getProperty(SHINDIG_PROXY_PROXY_PORT))));
                }
                return proxySettings;
            }

        } catch (Exception e) {
            log.error("Unable to get Proxy settings ", e);
            return Proxy.NO_PROXY;
        }
        return Proxy.NO_PROXY;
    }

    private static void setAuthenticator() {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {

                String password = Framework.getProperty(SHINDIG_PROXY_PASSWORD);
                if (password != null) {
                    return new PasswordAuthentication(
                            Framework.getProperty(SHINDIG_PROXY_USER),
                            password.toCharArray());
                }
                return null;

            }
        });
    }

    private static boolean isProxySet() {
        return ((Framework.getProperty(SHINDIG_PROXY_PROXY_SET) != null) && (Framework.getProperty(SHINDIG_PROXY_PROXY_SET).equals("true")));
    }

}
