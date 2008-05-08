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

package org.nuxeo.ecm.webengine.tests;

import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.webengine.servlet.WebServlet;
import org.nuxeo.ecm.webengine.tests.fake.FakeRequest;
import org.nuxeo.ecm.webengine.tests.fake.FakeResponse;
import org.nuxeo.ecm.webengine.tests.fake.FakeServletInputStream;

public abstract class BaseSiteRequestTestCase extends RepositoryOSGITestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("nuxeo-platform-content-template-manager");
        deployContrib("OSGI-INF/site-adapters-service-framework.xml");
        deployContrib("OSGI-INF/site-adapters-contrib.xml");
        deployContrib("OSGI-INF/site-manager-framework.xml");

        openRepository();
    }

    protected FakeResponse execSiteRequest(String method, String url)
            throws Exception {
        return execSiteRequest(method, url, null);
    }

    protected FakeResponse execSiteRequest(String method, String url, String data)
            throws Exception {
        FakeRequest fReq = new FakeRequest(method, url);
        if (data != null) {
            FakeServletInputStream in = new FakeServletInputStream(data);
            fReq.setStream(in);
        }
        return execSiteRequest(fReq);
    }

    protected FakeResponse execSiteRequest(FakeRequest fReq)
            throws Exception {
        FakeResponse fRes = new FakeResponse();
        fReq.setAttribute("TestCoreSession", getCoreSession());
        WebServlet siteServlet = new WebServlet();
        siteServlet.service(fReq, fRes);
        return fRes;
    }

}
