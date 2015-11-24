/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva <nelson.silva@inevo.pt> - initial API and implementation
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.oauth2.openid;

import java.util.Collection;

import org.nuxeo.runtime.api.Framework;

public class OpenIDConnectHelper {

    public static Collection<OpenIDConnectProvider> getProviders() {
        OpenIDConnectProviderRegistry registry = Framework.getLocalService(OpenIDConnectProviderRegistry.class);
        if (registry != null) {
            return registry.getProviders();
        }
        return null;
    }

    public static Collection<OpenIDConnectProvider> getEnabledProviders() {
        OpenIDConnectProviderRegistry registry = Framework.getLocalService(OpenIDConnectProviderRegistry.class);
        if (registry != null) {
            return registry.getEnabledProviders();
        }
        return null;
    }

    private OpenIDConnectHelper() {}
}
