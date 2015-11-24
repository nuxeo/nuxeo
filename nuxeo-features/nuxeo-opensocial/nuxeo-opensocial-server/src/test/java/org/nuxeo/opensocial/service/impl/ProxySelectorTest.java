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

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.Proxy;
import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@Features(RuntimeFeature.class)
@RunWith(FeaturesRunner.class)
public class ProxySelectorTest {

    private static final String SHINDIG_PROXY_SET = "shindig.proxy.proxySet";
    private static final String SHINDIG_PROXY_PORT = "shindig.proxy.proxyPort";
    private static final String SHINDIG_PROXY_HOST = "shindig.proxy.proxyHost";
    private static final String SHINDIG_PROXY_PASSWORD = "shindig.proxy.password";
    private static final String SHINDIG_PROXY_USER = "shindig.proxy.user";
    private static final String SHINDIG_PROXY_EXCLUDE = "shindig.proxy.excludeHost";

    private static final OSGiRuntimeService osgi = (OSGiRuntimeService) Framework.getRuntime();

    public ProxySelectorTest() {
        osgi.setProperty(SHINDIG_PROXY_SET  , "true");
        osgi.setProperty(SHINDIG_PROXY_HOST  , "proxyhost.com");
        osgi.setProperty(SHINDIG_PROXY_PORT  , "8080");
        osgi.setProperty(SHINDIG_PROXY_USER  , "10034234");
        osgi.setProperty(SHINDIG_PROXY_PASSWORD  , "trucmuche");
        osgi.setProperty(SHINDIG_PROXY_EXCLUDE  , "in.nuxeo.com");
    }

    @Test
    public void canExcludeHostForProxy() throws Exception {
        SimpleProxySelector proxySelector = new SimpleProxySelector();
        Proxy noproxy = proxySelector.select(new URI("http://truc.in.nuxeo.com/")).get(0);
        assertEquals(Proxy.NO_PROXY, noproxy);

        Proxy proxy = proxySelector.select(new URI("http://google.fr/")).get(0);
        assertTrue(Proxy.NO_PROXY != proxy);
    }

    @Test
    public void badConfReturnsNoProxy() throws Exception {
        osgi.setProperty(SHINDIG_PROXY_PORT  , "notanint");
        SimpleProxySelector proxySelector = new SimpleProxySelector();
        Proxy noproxy = proxySelector.select(new URI("http://google.com/")).get(0);
        assertEquals(Proxy.NO_PROXY, noproxy);


    }
}
