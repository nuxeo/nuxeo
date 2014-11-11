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

package org.nuxeo.ecm.platform.cache.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.cache.CacheConfiguration;
import org.nuxeo.ecm.platform.cache.CacheServiceException;

/**
 * Different cache configurations resides in xml files referred by constants in
 * this class.
 *
 * @author DM
 *
 */
public class CacheConfigurationTestImpl implements CacheConfiguration {

    public static final String SERVICE_PROP_INVALIDATION_SYNC = "jboss-cache-conf/invalidationSync-service.xml";

    public static final String SERVICE_PROP_REPLICATE_SYNC = "jboss-cache-conf/replSync-service.xml";

    public static final String SERVICE_PROP_DEFAULT = SERVICE_PROP_REPLICATE_SYNC;

    private static final Log log = LogFactory
            .getLog(CacheConfigurationFactoryImpl.class);

    public InputStream getConfigAsStream(Config conf)
            throws CacheServiceException {
        final String cfgFileName;
        switch (conf) {
            case CFG_INVALIDATION_SYNC:
                cfgFileName = SERVICE_PROP_INVALIDATION_SYNC;
                break;
            case CFG_REPL_SYNC:
                cfgFileName = SERVICE_PROP_REPLICATE_SYNC;
                break;
            default:
                throw new CacheServiceException("Unsupported configuration: "
                        + conf);
        }

        final InputStream cfgIS;

        try {
            final URL resource = getClass().getClassLoader().getResource(
                    cfgFileName);
            if (resource == null) {
                throw new CacheServiceException(
                        "Cannot find a configuration resource with name: "
                                + cfgFileName);
            }
            log.info("cache configuration URL: " + resource);
            cfgIS = resource.openStream();
        } catch (IOException e) {
            //e.printStackTrace();
            throw new CacheServiceException(
                    "Error loading cache configuration from resource: "
                            + cfgFileName);
        }
        return cfgIS;
    }

    public String getConfigFile(Config conf) throws CacheServiceException {
        final String cfgFile;
        switch (conf) {
            case CFG_INVALIDATION_SYNC:
                cfgFile = SERVICE_PROP_INVALIDATION_SYNC;
                break;
            default:
                throw new CacheServiceException("Unsupported configuration: "
                        + conf);
        }
        return cfgFile;
    }

}
