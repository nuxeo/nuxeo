/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Sun Seng David TAN
 *     Florent Guillaume
 *     Benoit Delbosc
 *     Antoine Taillefer
 *     Anahide Tchertchian
 *     Guillaume Renard
 *     Mathieu Guillaume
 *     Julien Carsique
 */
package org.nuxeo.functionaltests.proxy;

import java.io.File;

import org.browsermob.proxy.ProxyServer;
import org.nuxeo.common.Environment;
import org.openqa.selenium.Proxy;

/**
 * Proxy server manager.
 *
 * @since 8.2
 */
public class ProxyManager {

    private static final int PROXY_PORT = 4444;

    private static final String HAR_NAME = "http-headers.json";

    protected ProxyServer proxyServer = null;

    public Proxy startProxy() throws Exception {
        if (Boolean.TRUE.equals(Boolean.valueOf(System.getProperty("useProxy", "false")))) {
            proxyServer = new ProxyServer(PROXY_PORT);
            proxyServer.start();
            proxyServer.setCaptureHeaders(true);
            // Block access to tracking sites
            proxyServer.blacklistRequests("https?://www\\.nuxeo\\.com/embedded/wizard.*", 410);
            proxyServer.blacklistRequests("https?://.*\\.mktoresp\\.com/.*", 410);
            proxyServer.blacklistRequests(".*_mchId.*", 410);
            proxyServer.blacklistRequests("https?://.*\\.google-analytics\\.com/.*", 410);
            proxyServer.newHar("webdriver-test");
            Proxy proxy = proxyServer.seleniumProxy();
            return proxy;
        } else {
            return null;
        }
    }

    public void stopProxy() throws Exception {
        if (proxyServer != null) {
            String target = System.getProperty(Environment.NUXEO_LOG_DIR);
            File harFile;
            if (target == null) {
                harFile = new File(HAR_NAME);
            } else {
                harFile = new File(target, HAR_NAME);
            }
            proxyServer.getHar().writeTo(harFile);
            proxyServer.stop();
        }
    }

}
