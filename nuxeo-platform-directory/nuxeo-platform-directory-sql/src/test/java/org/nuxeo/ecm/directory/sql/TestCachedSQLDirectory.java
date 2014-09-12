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

package org.nuxeo.ecm.directory.sql;

import org.eclipse.jdt.internal.core.Assert;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.redis.RedisFeature;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.DirectoryCache;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;

public class TestCachedSQLDirectory extends TestSQLDirectory {

    protected final static String CACHE_CONTRIB = "sql-directory-cache-config.xml";

    protected final static String REDIS_CACHE_CONFIG = "sql-directory-redis-cache-config.xml";

    protected final static String ENTRY_CACHE_NAME = "sql-entry-cache";

    protected final static String ENTRY_CACHE_WITHOUT_REFERENCES_NAME = "sql-entry-cache-without-references";

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.cache");

        if (!RedisFeature.getMode().equals(RedisFeature.Mode.disabled)) {
            RedisFeature.setup(this);
            deployTestContrib("org.nuxeo.ecm.directory.sql.tests",
                    REDIS_CACHE_CONFIG);

        } else {
            deployTestContrib("org.nuxeo.ecm.directory.sql.tests",
                    CACHE_CONTRIB);
        }
        fireFrameworkStarted();
        AbstractDirectory dir = getSQLDirectory();

        DirectoryCache cache = dir.getCache();
        cache.setEntryCacheName(ENTRY_CACHE_NAME);
        cache.setEntryCacheWithoutReferencesName(ENTRY_CACHE_WITHOUT_REFERENCES_NAME);

    }

    @Test
    public void testGetFromCache() throws DirectoryException, Exception {
        Session sqlSession = getSQLDirectory().getSession();

        // First call will update cache
        DocumentModel entry = sqlSession.getEntry("user_1");
        Assert.isNotNull(entry);

        // Second call will use the cache
        entry = sqlSession.getEntry("user_1");
        Assert.isNotNull(entry);
    }

}
