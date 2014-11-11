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

import org.eclipse.jdt.internal.core.Assert;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.redis.RedisFeature;
import org.nuxeo.ecm.directory.DirectoryCache;
import org.nuxeo.ecm.directory.Session;

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

        if (RedisFeature.setup(this)) {
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
            cache.initialize(ENTRY_CACHE_NAME,ENTRY_CACHE_WITHOUT_REFERENCES_NAME);
        }
    }

    @Test
    public void testGetFromCache() {
        Session ldapSession = getLDAPDirectory("userDirectory").getSession();

        //First call will update cache
        DocumentModel entry = ldapSession.getEntry("user1");
        Assert.isNotNull(entry);

        //Second call will use the cache
        entry = ldapSession.getEntry("user1");
        Assert.isNotNull(entry);
    }


}
