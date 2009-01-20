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

package org.nuxeo.ecm.core.search;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.FieldConstants;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;
import org.nuxeo.ecm.core.search.api.internals.SearchServiceInternals;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Testing of generic indexable resources stuff. Independent from
 * documents.
 *
 * @author gracinet
 *
 */
public class TestIndexableResources extends NXRuntimeTestCase {

    private SearchServiceInternals service;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib(Constants.BUNDLE,
                "OSGI-INF/nxsearch-framework.xml");
        deployContrib(Constants.TEST_BUNDLE,
                "nxsearch-test-contrib.xml");

        service = (SearchServiceInternals) Framework.getService(
                SearchService.class);
    }

    public void testFramework() {
        assertNotNull(service);
    }

    public void testFieldProperty() {
        IndexableResourceConf rConf
            = service.getIndexableResourceConfByName("fake", false);
        IndexableResourceDataConf dConf = rConf.getIndexableFields().get("pipe");
        Map<String, Serializable> props = dConf.getProperties();
        assertNotNull(props);
        assertEquals("|", props.get(FieldConstants.PROPERTY_PATH_SEPARATOR));
    }

}
