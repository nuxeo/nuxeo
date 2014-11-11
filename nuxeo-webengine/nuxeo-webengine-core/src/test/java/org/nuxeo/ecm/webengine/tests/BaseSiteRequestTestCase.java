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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.webengine.WebApplication;
import org.nuxeo.ecm.webengine.WebApplicationMapping;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.servlet.WebServlet;
import org.nuxeo.ecm.webengine.tests.fake.FakeRequest;
import org.nuxeo.ecm.webengine.tests.fake.FakeResponse;
import org.nuxeo.ecm.webengine.tests.fake.FakeServletConfig;
import org.nuxeo.ecm.webengine.tests.fake.FakeServletInputStream;
import org.nuxeo.runtime.api.Framework;

public abstract class BaseSiteRequestTestCase extends RepositoryOSGITestCase {

    protected WebEngine engine = null;
    protected WebServlet siteServlet = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("nuxeo-runtime-scripting");
        deployBundle("nuxeo-core-schema");
        deployBundle("nuxeo-core-query");
        deployBundle("nuxeo-core-api");
        deployBundle("nuxeo-core");
        deployBundle("nuxeo-platform-rendering");
        deployBundle("nuxeo-webengine-core");

        openRepository();
        // setup resolver
        engine = Framework.getLocalService(WebEngine.class);
        WebApplication app = engine.getApplication("default");
        app.setDefaultPage(null);
        app.setRepositoryName("demo");

        WebApplicationMapping mapping = new WebApplicationMapping("/", "default", "/");
        engine.addApplicationMapping(mapping);

        // setup repo
        CoreSession session = getCoreSession();
        DocumentModel root = session.getRootDocument();

        DocumentModel site = session.createDocumentModel(root.getPathAsString(), "site",
                "Folder");
        site.setProperty("dublincore", "title", "Site");
        site = session.createDocument(site);

        DocumentModel page = session.createDocumentModel(site.getPathAsString(), "page",
                "Note");
        page.setProperty("dublincore", "title", "Page");
        page.setProperty("note", "note", "Content");
        page = session.createDocument(page);

        session.save();

        siteServlet = new WebServlet();
        FakeServletConfig cfg = new FakeServletConfig();
        siteServlet.init(cfg);

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
//        fReq.setAttribute(DefaultWebContext.CORESESSION_KEY, getCoreSession());
        siteServlet.service(fReq, fRes);
        return fRes;
    }


    protected CoreSession getNewSession() throws Exception {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", "Administrator");
        return CoreInstance.getInstance().open(REPOSITORY_NAME, ctx);
    }
}
