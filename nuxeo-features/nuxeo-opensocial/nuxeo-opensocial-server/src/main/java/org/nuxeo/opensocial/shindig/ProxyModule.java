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

import java.net.ProxySelector;

import org.apache.shindig.gadgets.http.HttpFetcher;
import org.nuxeo.opensocial.service.impl.SimpleProxySelector;
import org.nuxeo.opensocial.shindig.gadgets.ProxySelectorHttpFetcher;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class ProxyModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ProxySelector.class).to(SimpleProxySelector.class);
        bind(HttpFetcher.class).to(ProxySelectorHttpFetcher.class).in(Scopes.SINGLETON);
    }

}
