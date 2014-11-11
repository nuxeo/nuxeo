/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.query.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class TestPageProviderService extends SQLRepositoryTestCase {

    private static final String CURRENT_DOCUMENT_CHILDREN = "CURRENT_DOCUMENT_CHILDREN";

    private static final String FOO = "foo";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        openSession();
    }

    @Override
    protected void deployRepositoryContrib() throws Exception {
        super.deployRepositoryContrib();
        deployContrib("org.nuxeo.ecm.platform.query.api",
                "OSGI-INF/pageprovider-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.query.api.test",
                "test-pageprovider-contrib.xml");
    }

    public void testRegistration() throws Exception {
        PageProviderService service = Framework.getService(PageProviderService.class);
        assertNotNull(service);

        assertNull(service.getPageProviderDefinition(FOO));

        PageProviderDefinition def = service.getPageProviderDefinition(CURRENT_DOCUMENT_CHILDREN);
        assertNotNull(def);
        assertEquals(CURRENT_DOCUMENT_CHILDREN, def.getName());
        // TODO: test given provider information
    }

    public void testQuery() throws Exception {
        PageProviderService pps = Framework.getService(PageProviderService.class);
        assertNotNull(pps);

        PageProviderDefinition ppd = pps.getPageProviderDefinition(CURRENT_DOCUMENT_CHILDREN);
        ppd.setPattern("SELECT * FROM Document");
        HashMap<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (AbstractSession) session);
        PageProvider<?> pp = pps.getPageProvider(CURRENT_DOCUMENT_CHILDREN,
                ppd, null, Long.valueOf(1), Long.valueOf(0), props);

        assertNotNull(pp);
        List<?> p = pp.getCurrentPage();
        assertNotNull(p);
        assertEquals(p.size(), 0);
    }
}
