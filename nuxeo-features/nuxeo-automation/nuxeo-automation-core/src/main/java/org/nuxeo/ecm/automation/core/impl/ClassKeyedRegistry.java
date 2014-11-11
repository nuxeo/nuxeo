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
 */
package org.nuxeo.ecm.automation.core.impl;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ClassKeyedRegistry<V> extends SuperKeyedRegistry<Class<?>, V> {

    @Override
    protected boolean isRoot(Class<?> key) {
        return key == Object.class;
    }

    @Override
    protected List<Class<?>> getSuperKeys(Class<?> key) {
        List<Class<?>> result = new ArrayList<Class<?>>();
        Class<?> cl = key.getSuperclass();
        if (cl != null) {
            result.add(cl);
        }
        for (Class<?> itf : key.getInterfaces()) {
            result.add(itf);
        }
        return result;
    }

    @Override
    protected boolean isCachingEnabled(Class<?> key) {
        return !Proxy.isProxyClass(key);
    }

}
