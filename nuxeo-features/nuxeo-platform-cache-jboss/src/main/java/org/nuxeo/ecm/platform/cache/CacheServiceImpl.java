/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.cache;

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.AbstractTreeCacheListener;
import org.jboss.cache.CacheException;
import org.jboss.cache.ConfigureException;
import org.jboss.cache.Fqn;
import org.jboss.cache.PropertyConfigurator;
import org.jboss.cache.TreeCacheListener;
import org.jboss.cache.aop.PojoCache;
import org.jboss.cache.aop.PojoCacheMBean;
import org.jboss.mx.util.MBeanProxyExt;
import org.jboss.mx.util.MBeanServerLocator;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * A wrapper class for the actual JBoss PojoCache implementation. The client
 * code doesn't need to know what Cache system will use.
 *
 * @author DM
 *
 */
public class CacheServiceImpl implements CacheService {

    private static final Log log = LogFactory.getLog(CacheServiceImpl.class);

    private PojoCacheMBean pojoCache;

    private MBeanServer server;

    private static boolean initialized;

    // TreeCacheMBean cache;

    /**
     * Initialises the cache as a MBean service.
     */
    public void init(final String pojoCacheServiceName) throws CacheServiceException {

        try {
            server = MBeanServerLocator.locateJBoss();
            pojoCache = (PojoCacheMBean) MBeanProxyExt.create(
                    PojoCacheMBean.class, pojoCacheServiceName,
                    server);
            if (pojoCache == null) {
                throw new CacheServiceException("PojoCacheMBean not found. Service not bound: "
                        + pojoCacheServiceName);
            }
        } catch (Exception ex) {
            // handle exception
            ex.printStackTrace();

            throw new CacheServiceException("Error initializing pojoCache", ex);
        }

        // register debug listener
        pojoCache.addTreeCacheListener(new TreeCacheAdaptor());
    }

    /**
     * Initialises and configures the cache.
     *
     * @param cfg
     *            if the cache is deployed as a JBoss service this param should
     *            be null (the cache configuration is performed by JBossCache
     *            service in JBoss)
     *
     * @throws CacheServiceException
     */
    public synchronized void initCache(CacheConfiguration.Config cfg)
            throws CacheServiceException {
        if (initialized) {

            // TODO : Should initialize one time only
            // FIXME : Should initialize one time only
            // throw new IllegalStateException("already initialized");
            // return;
        }

        initialized = true;

        try {
            pojoCache = new PojoCache();
        } catch (Exception e) {
            // e.printStackTrace();
            final String errMsg = "Error initialising PojoCache";
            log.error(errMsg, e);
            throw new CacheServiceException(errMsg, e);
        }

        if (cfg != null) {
            log.info("configure local cache");
            final PropertyConfigurator config = new PropertyConfigurator();
            try {
                final CacheConfiguration cconf = CacheConfigurationFactory
                        .getCacheConfiguration();
                config.configure(pojoCache, cconf.getConfigAsStream(cfg));
            } catch (ConfigureException e) {
                // e.printStackTrace();
                final String errMsg = "Error configuring PojoCache";
                log.error(errMsg, e);
                throw new CacheServiceException(errMsg, e);
            }
        }

        // register debug listener
        pojoCache.addTreeCacheListener(new TreeCacheAdaptor());

        log.info("=============================== Cache system initialized == Client");
    }

    public void startService() throws CacheServiceException {
        log.info("Starting cache service...");
        try {
            pojoCache.startService();
        } catch (Exception e) {
            // e.printStackTrace();
            final String errMsg = "Error starting PojoCache";
            log.error(errMsg, e);
            throw new CacheServiceException(errMsg, e);
        }
    }

    /**
     * Puts an object into the cache.
     *
     * @param fqn the fqn instance to associate with the object in the cache.
     * @param obj AOP-enabled object to be inerted into the cache. If null, it
     *            will nullify the fqn node.
     * @throws CacheServiceException
     */
    public void putObject(String fqn, Object obj) throws CacheServiceException {

        log.debug("putObject at " + fqn + "; type: " + obj.getClass().getName());
        try {
            pojoCache.putObject(fqn, obj);
        } catch (CacheException e) {
            // e.printStackTrace();
            final String errMsg = "Error adding Object to cache";
            log.error(errMsg, e);
            throw new CacheServiceException(errMsg, e);
        }
    }

    /**
     * Retrieves an object from the cache.
     *
     * @param fqn the fqn instance to associate with the object in the cache.
     * @return obj AOP-enabled object from the cache or <code>null</code> if
     *         there is no object identified by the given fqdn
     *
     * @throws CacheServiceException
     *             if some internal cache system error occur
     */
    public Object getObject(String fqn) throws CacheServiceException {
        try {
            return pojoCache.getObject(fqn);
        } catch (CacheException e) {
            // e.printStackTrace();
            final String errMsg = "Error getting Object from cache (fqn: "
                    + fqn + ')';
            log.error(errMsg, e);
            throw new CacheServiceException(errMsg, e);
        }
    }

    /**
     * @param fqn
     * @return whether or not an object exist for the given key
     */
    public boolean exists(String fqn) {
        return pojoCache.exists(fqn);
    }

    public void stopService() {
        pojoCache.stopService();
        // remove this object from the map
        /*
        final Set<Entry<String, CacheService>> entries = instances.entrySet();
        String key = null;
        for (Entry<String, CacheService> entry : entries) {
            if (entry.getValue() == this) {
                key = entry.getKey();
                break;
            }
        }
        if (key != null) {
            instances.remove(key);
        }
        */
    }

    /**
     * Removes the object with the specified FQN if exists.
     *
     * @param fqn
     * @throws CacheServiceException if some cache error occures
     */
    public void removeObject(String fqn) throws CacheServiceException {
        log.debug("Remove document from cache. FQN: " + fqn);
        try {
            pojoCache.removeObject(fqn);
        } catch (CacheException e) {
            // e.printStackTrace();
            final String errMsg = "Error removing Object from cache (fqn: "
                    + fqn + ')';
            log.error(errMsg, e);
            throw new CacheServiceException(errMsg, e);
        }
    }

    private final Map<CacheListener, TreeCacheListener> cacheListeners
            = new HashMap<CacheListener, TreeCacheListener>();

    /**
     * Adds a CacheListener that will be notified on cache objects changes.
     *
     * @param cacheListener
     */
    public void addCacheListener(final CacheListener cacheListener) {
        if (cacheListeners.containsKey(cacheListener)) {
            // already there
            return;
        }

        TreeCacheListener treeCacheListener = new AbstractTreeCacheListener() {

            public void nodeRemove(Fqn fqn, boolean pre, boolean isLocal) {
                log.debug("Cache notification received: nodeRemove(" + fqn
                                + ')');
                if (pre) {
                    final Object obj;
                    try {
                        obj = pojoCache.getObject(fqn);
                    } catch (CacheException e) {
                        // e.printStackTrace();
                        final String errMsg = "Error getting Object from cache (fqn: "
                                + fqn + ')';
                        log.error(errMsg, e);
                        //throw  new CacheServiceException(errMsg, e);
                        return;
                    }
                    if (obj instanceof DocumentModel) {
                        final DocumentModel docModel = (DocumentModel) obj;
                        cacheListener.documentRemove(docModel);
                    } else {
                        // TODO we should know exactly what kind of
                        // objects are in the cache
                        log.error("Cache notification not handled (nodeRemove). Obj: "
                                + obj + "; Fqn: " + fqn);
                    }
                } else {
                    // the object has already been removed so we can send
                    // an event only with a fqn
                    cacheListener.documentRemoved(fqn.toString());
                }
            }

            public void nodeModify(Fqn fqn, boolean pre, boolean isLocal) {
                log.debug("Cache notification received: nodeModify(" + fqn
                        + ')');
                final Object obj;
                try {
                    obj = pojoCache.getObject(fqn);
                } catch (CacheException e) {
                    // e.printStackTrace();
                    final String errMsg = "Error getting Object from cache (fqn: "
                            + fqn + ')';
                    log.error(errMsg, e);
                    //throw  new CacheServiceException(errMsg, e);
                    return;
                }
                if (obj instanceof DocumentModel) {
                    final DocumentModel docModel = (DocumentModel) obj;
                    cacheListener.documentUpdate(docModel, pre);
                } else {
                    // TODO we should know exactly what kind of
                    // objects are in the cache
                    log.error("Cache notification not handled (nodeModify). Obj: "
                            + obj + "; Fqn: " + fqn);
                }
            }
        };

        pojoCache.addTreeCacheListener(treeCacheListener);
        cacheListeners.put(cacheListener, treeCacheListener);

        log.debug("Cache listener registered");
    }

    /**
     * Removes the given CacheListener.
     *
     * @param cacheListener
     */
    public void removeCacheListener(final CacheListener cacheListener) {
        if (cacheListener == null) {
            return;
        }

        final String logPrefix = "<removeCacheListener> ";
        if (!cacheListeners.containsKey(cacheListener)) {
            log.warn(logPrefix + "Cache listener not registered: " + cacheListener);
            return;
        }

        final TreeCacheListener treeCacheListener = cacheListeners
                .get(cacheListener);
        pojoCache.removeTreeCacheListener(treeCacheListener);
        cacheListeners.remove(cacheListener);

        log.debug(logPrefix + "Cache listener removed");
    }

}
