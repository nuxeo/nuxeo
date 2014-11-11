/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: TestSearchServiceBean.java 19066 2007-05-21 15:42:22Z sfermigier $
 */

package org.nuxeo.ecm.core.search.ejb;

import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestSearchServiceBean extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.search.ejb.tests",
                "nxsearch-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.search.ejb.tests",
                "nxsearch-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.search.ejb.tests",
                "nxsearch-platform-contrib.xml");

        SearchService service = Framework.getLocalService(SearchService.class);
        assertNotNull(service);
    }

    public void testBean() {
        //SearchService service = new SearchServiceBean();
        // :XXX: Just checking the logs here for now.
        //service.index(new IndexableResourcesImpl());
    }

}
