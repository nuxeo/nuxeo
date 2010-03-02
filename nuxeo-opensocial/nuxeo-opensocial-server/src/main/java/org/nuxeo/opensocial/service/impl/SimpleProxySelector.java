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

import org.nuxeo.runtime.api.Framework;

public class SimpleProxySelector extends ProxySelector {

    private static final String SHINDIG_PROXY_PROXY_PORT = "shindig.proxy.proxyPort";

    private static final String SHINDIG_PROXY_PROXY_HOST = "shindig.proxy.proxyHost";

    private static final String SHINDIG_PROXY_PROXY_SET = "shindig.proxy.proxySet";

    private static final String SHINDIG_PROXY_PASSWORD = "shindig.proxy.password";

    private static final String SHINDIG_PROXY_USER = "shindig.proxy.user";

    private Proxy proxySettings = null;

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        // Do nothing

    }

    @Override
    public List<Proxy> select(URI uri) {
        List<Proxy> proxies = new ArrayList<Proxy>();
        if ("localhost".equals(uri.getHost())) {
            proxies.add(Proxy.NO_PROXY);
        } else {
            proxies.add(getProxySettings());
        }
        return proxies;

    }

    private Proxy getProxySettings() {
        if (isProxySet()) {
            if (proxySettings == null) {
                setAuthenticator();
                proxySettings = new Proxy(
                        Proxy.Type.HTTP,
                        new InetSocketAddress(
                                Framework.getProperty(SHINDIG_PROXY_PROXY_HOST),
                                Integer.parseInt(Framework.getProperty(SHINDIG_PROXY_PROXY_PORT))));
            }

            return proxySettings;
        } else {
            return Proxy.NO_PROXY;
        }
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
        return Framework.getProperty(SHINDIG_PROXY_PROXY_SET) != null
                && Framework.getProperty(SHINDIG_PROXY_PROXY_SET).equals("true");
    }

}
