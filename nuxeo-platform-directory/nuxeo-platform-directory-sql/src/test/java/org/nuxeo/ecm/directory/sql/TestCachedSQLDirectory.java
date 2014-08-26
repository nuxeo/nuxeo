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

package org.nuxeo.ecm.directory.sql;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.DirectoryCache;

public class TestCachedSQLDirectory extends TestSQLDirectory {

    protected final static String CACHE_CONTRIB = "sql-directory-cache-config.xml";

    protected final static String ENTRY_CACHE_NAME = "sql-entry-cache";

    protected final static String ENTRY_CACHE_WITHOUT_REFERENCES_NAME = "sql-entry-cache-without-references";

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.cache");
        deployTestContrib("org.nuxeo.ecm.directory.sql.tests", CACHE_CONTRIB);

        AbstractDirectory dir = getSQLDirectory();

        DirectoryCache cache = dir.getCache();
        cache.setEntryCacheName(ENTRY_CACHE_NAME);
        cache.setEntryCacheWithoutReferencesName(ENTRY_CACHE_WITHOUT_REFERENCES_NAME);

    }

}
