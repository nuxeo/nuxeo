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
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.cache.CacheFeature;
import org.nuxeo.ecm.core.redis.RedisFeature;
import org.nuxeo.ecm.directory.DirectoryCache;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

@Features({ RedisFeature.class, CacheFeature.class})
@Deploy("org.nuxeo.ecm.directory:sql-directory-cache-config.xml")
public class TestCachedSQLDirectory extends TestSQLDirectory {

    protected final static String CACHE_CONTRIB = "sql-directory-cache-config.xml";

    protected final static String REDIS_CACHE_CONFIG = "sql-directory-redis-cache-config.xml";

    protected final static String ENTRY_CACHE_NAME = "sql-entry-cache";

    protected final static String ENTRY_CACHE_WITHOUT_REFERENCES_NAME = "sql-entry-cache-without-references";

    @Before
    public void setUp()  {

        DirectoryCache cache = userDir.getCache();
        cache.initialize(ENTRY_CACHE_NAME,ENTRY_CACHE_WITHOUT_REFERENCES_NAME);

    }

    @Test
    public void testGetFromCache() throws DirectoryException, Exception {
        Session sqlSession = userDirSession;

        // First call will update cache
        DocumentModel entry = sqlSession.getEntry("user_1");
        Assert.isNotNull(entry);

        // Second call will use the cache
        entry = sqlSession.getEntry("user_1");
        Assert.isNotNull(entry);
    }

}
