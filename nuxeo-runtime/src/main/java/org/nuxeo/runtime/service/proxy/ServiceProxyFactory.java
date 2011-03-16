/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.service.proxy;

import org.nuxeo.runtime.service.AdapterManager;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ServiceProxyFactory {

    @SuppressWarnings("unchecked")
    public static <T> T getProxy(T service) {
        ServiceProxy<T> prx = AdapterManager.getInstance().getAdapter(service, ServiceProxy.class);
        return prx != null ? (T) prx : service;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAdapterProxy(T adapter) {
        AdapterProxy<T> prx = AdapterManager.getInstance().getAdapter(adapter, AdapterProxy.class);
        return prx != null ? (T) prx : adapter;
    }

}
