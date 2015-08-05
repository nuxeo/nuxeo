/*******************************************************************************
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 ******************************************************************************/
package org.nuxeo.ecm.core.cache;

import org.nuxeo.common.xmap.XMap;

public class InMemoryCacheFactory implements CacheFactory {

    final XMap xmap = configureXMap();

    @Override
    public XMap xmap(CacheDescriptor config) {
        return xmap;
    }

    @Override
    public void merge(CacheDescriptor src, CacheDescriptor dst) {
        ((InMemoryCacheDescriptor) dst).maxSize = ((InMemoryCacheDescriptor) src).maxSize;
        ((InMemoryCacheDescriptor) dst).concurrencyLevel = ((InMemoryCacheDescriptor) src).concurrencyLevel;
    }

    @Override
    public InMemoryCacheDescriptor createConfig(String name) {
        InMemoryCacheDescriptor config = new InMemoryCacheDescriptor(); // should get default params
        config.name = name;
        config.ttl = 20;
        config.maxSize = -1;
        config.concurrencyLevel = -1;
        return config;
    }

    @Override
    public Cache createCache(CacheDescriptor config) {
        return new InMemoryCache((InMemoryCacheDescriptor) config);
    }

    XMap configureXMap() {
        XMap xmap = new XMap();
        xmap.register(InMemoryCacheDescriptor.class);
        return xmap;
    }

    @Override
    public void destroyCache(Cache cache) {
        ;
    }

    @Override
    public boolean isInstanceType(Class<? extends Cache> type) {
        return InMemoryCache.class.isAssignableFrom(type);
    }


    @Override
    public boolean isConfigType(Class<? extends CacheDescriptor> type) {
        return InMemoryCacheDescriptor.class.isAssignableFrom(type);
    }

}
