/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.shindig;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;

import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;

public class ProxyModule extends AbstractModule {
    private static final String SHINDIG_PROXY_PROXY_PORT = "shindig.proxy.proxyPort";
    private static final String SHINDIG_PROXY_PROXY_HOST = "shindig.proxy.proxyHost";

    private static final String SHINDIG_PROXY_PASSWORD = "shindig.proxy.password";
    private static final String SHINDIG_PROXY_USER = "shindig.proxy.user";

    @Override
    protected void configure() {
        bind(Proxy.class).toProvider(ProxyProvider.class);
    }

    public static class ProxyProvider implements Provider<Proxy> {
        public ProxyProvider() {
        }


        public Proxy get() {
            try {
            OpenSocialService os = Framework.getService(OpenSocialService.class);
            return os.getProxySettings();
            } catch (Exception e) {
                return null;
            }

        }
    }
}
