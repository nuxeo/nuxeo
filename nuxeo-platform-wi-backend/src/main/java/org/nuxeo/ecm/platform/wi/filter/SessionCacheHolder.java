package org.nuxeo.ecm.platform.wi.filter;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class SessionCacheHolder {

    private static SessionCacheHolder instance = new SessionCacheHolder();
    private SessionCache cache;

    public static SessionCacheHolder getInstance() {
        return instance;
    }

    private SessionCacheHolder() {
        cache = new SessionCache();
    }

    public SessionCache getCache() {
        return cache;
    }
}
