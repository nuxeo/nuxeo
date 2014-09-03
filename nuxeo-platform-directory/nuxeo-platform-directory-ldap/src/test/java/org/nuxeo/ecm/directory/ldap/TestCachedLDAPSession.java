/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.ldap;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.core.redis.RedisFeature;
import org.nuxeo.ecm.directory.DirectoryCache;

/**
 * Test class for LDAP directory that use cache
 */
public class TestCachedLDAPSession extends TestLDAPSession {

    protected final static String CACHE_CONFIG = "ldap-directory-cache-config.xml";

    protected final static String REDIS_CACHE_CONFIG = "ldap-directory-redis-cache-config.xml";

    protected final static String ENTRY_CACHE_NAME = "ldap-entry-cache";

    protected final static String ENTRY_CACHE_WITHOUT_REFERENCES_NAME = "ldap-entry-cache-without-references";

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.cache");

        if (!RedisFeature.getMode().equals(RedisFeature.Mode.disabled)) {
            RedisFeature.setup(this);
            deployTestContrib(TEST_BUNDLE, REDIS_CACHE_CONFIG);

        } else {
            deployTestContrib(TEST_BUNDLE, CACHE_CONFIG);
        }
        fireFrameworkStarted();
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
