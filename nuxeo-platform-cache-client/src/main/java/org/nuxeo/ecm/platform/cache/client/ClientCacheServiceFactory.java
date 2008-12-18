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
 * $Id: LogEntryCallbackListener.java 16046 2007-04-12 14:34:58Z fguillaume $
 */

package org.nuxeo.ecm.platform.cache.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.cache.CacheService;
import org.nuxeo.ecm.platform.cache.CacheServiceFactory;

/**
 *
 * @author <a href="mailto:dms@nuxeo.com">Dragos Mihalache</a>
 */
public class ClientCacheServiceFactory {

    private static final Log log = LogFactory.getLog(ClientCacheServiceFactory.class);

    private static CacheService cacheService;

    private ClientCacheServiceFactory() {
    }

    /**
     *
     * @return an initialized CacheService object
     */
    public static CacheService getCacheService() {
        if (cacheService == null) {
            log.info("Initialising Client Cache for NUXEO");

            // initialize cache system
            //cacheService = new CacheService();
            try {
                cacheService = CacheServiceFactory.getCacheService(
                        ClientCacheServiceFactory.class.getName());
                //cacheService.init();
            } catch (Exception e) {
                final String errMsg = "In "
                        + ClientCacheServiceFactory.class.getSimpleName() + ": "
                        + e.getMessage();
                log.error(errMsg, e);

                // TODO maybe throw a checked exception
                throw new RuntimeException(errMsg, e);
            }
        }

        return cacheService;
    }

}
