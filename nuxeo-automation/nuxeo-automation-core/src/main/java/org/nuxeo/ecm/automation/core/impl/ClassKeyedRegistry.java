/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.impl;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
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
