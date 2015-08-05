package org.nuxeo.ecm.core.cache;

import org.nuxeo.common.xmap.XMap;

class NoCacheFactory implements CacheFactory {
    XMap xmap = createXMap();

    XMap createXMap() {
        XMap xmap = new XMap();
        xmap.register(CacheDescriptor.class);
        return xmap;
    }

    @Override
    public XMap xmap(CacheDescriptor config) {
        return xmap;
    }

    @Override
    public void merge(CacheDescriptor src, CacheDescriptor dst) {
        dst.remove = src.remove;
        dst.ttl = src.ttl;
    }

    @Override
    public boolean isInstanceType(Class<? extends Cache> type) {
        return false;
    }

    @Override
    public boolean isConfigType(Class<? extends CacheDescriptor> type) {
        return type == CacheDescriptor.class;
    }

    @Override
    public void destroyCache(Cache cache) {

    }

    @Override
    public CacheDescriptor createConfig(String name) {
        CacheDescriptor config = new CacheDescriptor();
        config.name = name;
        config.ttl = 20;
        return config;
    }

    @Override
    public Cache createCache(CacheDescriptor config) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CacheDescriptor clone(CacheDescriptor config) {
        CacheDescriptor clone = createConfig(config.name);
        merge(config, clone);
        return clone;
    }
}