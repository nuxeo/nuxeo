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
 * $Id$
 */

package org.nuxeo.ecm.directory.ldap;

import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.redis.RedisConfigurationDescriptor;
import org.nuxeo.ecm.core.redis.RedisService;
import org.nuxeo.ecm.core.redis.RedisServiceImpl;
import org.nuxeo.ecm.core.redis.RedisTestHelper;
import org.nuxeo.ecm.directory.DirectoryCache;
import org.nuxeo.runtime.api.Framework;

/**
 * Test class for LDAP directory that use cache
 */
public class TestCachedLDAPSession extends TestLDAPSession {

    protected final static String CACHE_CONFIG = "ldap-directory-cache-config.xml";

    protected final static String REDIS_CACHE_CONFIG = "ldap-directory-cache-config.xml";

    protected final static String ENTRY_CACHE_NAME = "ldap-entry-cache";

    protected final static String ENTRY_CACHE_WITHOUT_REFERENCES_NAME = "ldap-entry-cache-without-references";

    // Set to true to use redis cache implementation for unit test
    // Make sure you have started your redis server and that you compile the
    // RedisTestHelper class
    // with the server parameters pointing to your configuration
    protected final static boolean USE_REDIS = false;

    private RedisConfigurationDescriptor redisConfigurationDescriptor;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.cache");

        if (USE_REDIS) {
            
            //deployBundle("org.nuxeo.ecm.core.redis");
            deployTestContrib(TEST_BUNDLE, "OSGI-INF/redis-service.xml");
            redisConfigurationDescriptor = RedisTestHelper.getRedisConfigurationDescriptor();
            boolean enabled = redisConfigurationDescriptor != null;
            assumeTrue(enabled);
            RedisServiceImpl redisService = (RedisServiceImpl) Framework.getLocalService(RedisService.class);
            redisService.registerConfiguration(redisConfigurationDescriptor);
            RedisTestHelper.clearRedis(redisService);
            deployTestContrib(TEST_BUNDLE, REDIS_CACHE_CONFIG);

        } else {
            deployTestContrib(TEST_BUNDLE, CACHE_CONFIG);
        }

        List<String> directories = Arrays.asList("userDirectory",
                "groupDirectory");
        for (String directoryName : directories) {
            LDAPDirectory dir = getLDAPDirectory(directoryName);
            DirectoryCache cache = dir.getCache();
            cache.setEntryCacheName(ENTRY_CACHE_NAME);
            cache.setEntryCacheWithoutReferencesName(ENTRY_CACHE_WITHOUT_REFERENCES_NAME);
        }
    }

}
