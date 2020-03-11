/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nelson.silva@inevo.pt> - initial API and implementation
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.oauth2.openid;

import java.util.Collection;

import org.nuxeo.runtime.api.Framework;

public class OpenIDConnectHelper {

    public static Collection<OpenIDConnectProvider> getProviders() {
        OpenIDConnectProviderRegistry registry = Framework.getService(OpenIDConnectProviderRegistry.class);
        if (registry != null) {
            return registry.getProviders();
        }
        return null;
    }

    public static Collection<OpenIDConnectProvider> getEnabledProviders() {
        OpenIDConnectProviderRegistry registry = Framework.getService(OpenIDConnectProviderRegistry.class);
        if (registry != null) {
            return registry.getEnabledProviders();
        }
        return null;
    }

    private OpenIDConnectHelper() {
    }
}
