/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
