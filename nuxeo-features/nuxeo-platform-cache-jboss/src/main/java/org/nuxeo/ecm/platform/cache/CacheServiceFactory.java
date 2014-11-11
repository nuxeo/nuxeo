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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.cache.CacheConfiguration.Config;

public class CacheServiceFactory {

    private static final Log log = LogFactory.getLog(CacheServiceFactory.class);

    /**
     * We want to make the CacheService lifespan as long as it is potentially
     * useful. In case a SessionBean initiate a CacheService we won't distroy it
     * when SessionBean passivate, just retrieve a handle to it when
     * re-activated
     */
    private static final Map<String, CacheService> instances = new HashMap<String, CacheService>();

    // FIXME: make this configurable
    private static final String POJO_CACHE_SERVICE_NAME = "jboss.cache:service=TreeCache";

    private CacheServiceFactory() {
    }

    private static CacheService createMBeanCacheService()
            throws CacheServiceException {
        log.info("Trying to bind with MBean managed cache");
        final CacheServiceImpl cs = new CacheServiceImpl();
        cs.init(POJO_CACHE_SERVICE_NAME);
        log.info("MBean Pojo cache binded to: " + POJO_CACHE_SERVICE_NAME);
        return cs;
    }

    /**
     * To be used within JBoss when there is a MBean cache service
     * configured.
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static CacheService getCacheService(String id) {
        if (instances.containsKey(id)) {
            return instances.get(id);
        } else {
            CacheService cs;
            try {
                cs = createMBeanCacheService();
            } catch (CacheServiceException e) {
                //e.printStackTrace();
                final String errMsg = "Nuxeo Cache system initialization error. "
                        + "Cache won't be available. Error message: "
                        + e.getMessage();
                log.warn(errMsg);
                //log.debug(errMsg, e);

                cs = createDummyCacheService();
            }
            assert cs != null;
            instances.put(id, cs);
            return cs;
        }
    }

    private static CacheService createDummyCacheService() {
        log.info("Creating a Dummy Cache Service.");
        return new DummyCacheService();
    }

    public static CacheService getCacheService(Config cfg_repl_sync)
            throws CacheServiceException {
        final CacheServiceImpl cs = new CacheServiceImpl();
        cs.initCache(cfg_repl_sync);
        return cs;
    }

}
