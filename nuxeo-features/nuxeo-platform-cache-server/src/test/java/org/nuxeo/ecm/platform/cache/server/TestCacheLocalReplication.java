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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.cache.CacheService;
import org.nuxeo.ecm.platform.cache.CacheServiceException;
import org.nuxeo.ecm.platform.cache.CacheServiceFactory;
import org.nuxeo.ecm.platform.cache.CacheConfiguration.Config;

/**
 * These tests are using 2 caches seeking to replicate DocumentModel objects
 * between them. First cache is initialized by the super class.
 *
 * Both caches are initialised in this class
 *
 * @author DM
 */
public class TestCacheLocalReplication extends TestServerCacheBase {

    private static final Log log = LogFactory.getLog(TestCacheLocalReplication.class);

    private CacheService cacheServer;

    private CacheService cacheClient;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // initialize cache system
        // cache server is created and initialized by super class
        cacheServer = cache;

        // init the 'client' (second) instance of the cache
        cacheClient = CacheServiceFactory.getCacheService(Config.CFG_REPL_SYNC);
        // :FIXME: false ?
        //cacheClient.initCache(Config.CFG_REPL_SYNC);
    }

    protected void startClientCache() throws CacheServiceException {
        cacheClient.startService();
    }

    @Override
    protected void tearDown() throws Exception {
        if (cacheClient != null) {
            cacheClient.stopService();
        }
        super.tearDown();
    }

    public void testCacheReplication() throws ClientException,
            CacheServiceException {

        final DocumentModel root = coreSession.getRootDocument();

        // create a folder
        DocumentModel dm1 = new DocumentModelImpl(root.getPathAsString(),
                "folder", "Folder");
        dm1 = coreSession.createDocument(dm1);
        final String dmPath1 = dm1.getPathAsString();

        // create a file
        DocumentModel dm2 = new DocumentModelImpl(root.getPathAsString(),
                "file", "File");
        dm2 = coreSession.createDocument(dm2);
        final String dmPath2 = dm2.getPathAsString();

        // set business object onto cache

        cacheServer.putObject(dmPath1, dm1);

        log.info("========================================================");
        log.info("Compare DocumentModel with object on local cache ");
        log.info("local       : " + dm1);
        Object dm1Cached = cacheServer.getObject(dmPath1);
        log.info("local cache : " + dm1Cached);
        log.info("========================================================");
        assertEquals(dm1, dm1Cached);

        cacheServer.putObject(dmPath2, dm2);
        assertEquals(dm2, cacheServer.getObject(dmPath2));

        // find the same objects onto the second cache
        final Object dm1Replica = cacheClient.getObject(dmPath1);
        //System.out.println(dm1Replica);

        log.info("========================================================");
        log.info("Compare DocumentModel with object on client cache ");
        log.info("local        : " + dm1);
        log.info("client cache : " + dm1Replica);
        log.info("========================================================");
        assertEquals(dm1, dm1Replica);
    }

}
