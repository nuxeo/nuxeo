/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.platform.query.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.9.6
 */
public class TestPageProviderClassReplacer extends SQLRepositoryTestCase {

    protected PageProviderService pps;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        openSession();
        pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);
    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    @Override
    protected void deployRepositoryContrib() throws Exception {
        super.deployRepositoryContrib();
        deployContrib("org.nuxeo.ecm.platform.query.api",
                "OSGI-INF/pageprovider-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.query.api.test",
                "test-pageprovider-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.query.api.test",
                "test-pageprovider-classreplacer-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.query.api.test",
                "test-schemas-contrib.xml");
    }

    @Test
    public void testReplacer() throws Exception {
        PageProviderService pps = Framework
                .getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppd = pps
                .getPageProviderDefinition("CURRENT_DOCUMENT_CHILDREN");
        PageProvider<?> pp = pps.getPageProvider("CURRENT_DOCUMENT_CHILDREN",
                ppd, null, null, 1L, 0L, null);
        assertNotNull(pp);
        assertTrue("wrong class " + pp,
                pp instanceof CoreQueryAndFetchPageProvider);

        pp = pps.getPageProvider("foo", ppd, null, null, 1L, 0L, null);
        assertNotNull(pp);
        assertTrue("wrong class " + pp,
                pp instanceof CoreQueryAndFetchPageProvider);

        pp = pps.getPageProvider("bar", ppd, null, null, 1L, 0L, null);
        assertNotNull(pp);
        assertTrue("wrong class " + pp,
                pp instanceof CoreQueryDocumentPageProvider);
    }

}
