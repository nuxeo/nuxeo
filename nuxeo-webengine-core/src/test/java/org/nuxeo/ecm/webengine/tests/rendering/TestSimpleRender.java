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

package org.nuxeo.ecm.webengine.tests.rendering;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.tests.BaseSiteRequestTestCase;
import org.nuxeo.ecm.webengine.tests.fake.FakeResponse;

public class TestSimpleRender extends BaseSiteRequestTestCase {

    DocumentModel site;

    public void testEmpty() {
    }

    /**
     * TODO register test templates to be able to test rendering
     */
    public void xxx_testStaticTemplate() throws Exception {
        FakeResponse response = execSiteRequest("GET", "/testSite/testFile");
        assertEquals(200, response.getStatus());

        String output = response.getOutput();

        System.out.println(output);
        assertTrue(output.contains("My Path: /default-domain/workspaces/testSite/testFile"));
        assertTrue(
                output.contains("My Super Context Doc Path: /default-domain/workspaces/testSite"));
    }

}
