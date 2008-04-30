/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.tests.security;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.webengine.ObjectDescriptor;
import org.nuxeo.ecm.webengine.SiteManager;
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class GuardTest extends NXRuntimeTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("nuxeo-runtime-scripting");
        deployBundle("nuxeo-core-schema");
        deployBundle("nuxeo-core-query");
        deployBundle("nuxeo-core-api");
        deployBundle("nuxeo-core");
        deployBundle("nuxeo-platform-site");
        deployContrib("OSGI-INF/DemoRepository.xml");
        deployContrib("OSGI-INF/site-manager-framework.xml");
        deployContrib("OSGI-INF/site-manager-framework.xml");
        deployContrib("OSGI-INF/test-security-guards.xml");
    }

    public void testGuardRegistration() throws Exception {

        SiteManager mgr = Framework.getLocalService(SiteManager.class);
        assertNotNull(mgr);

        ObjectDescriptor od = mgr.getObject("siteFolder2");
        assertNotNull(od);

        ActionDescriptor ad = od.getAction("view");
        assertNotNull(ad);
        assertNotNull(ad.getGuard());

        ad = od.getAction("myAction1");
        Guard g = ad.getGuard();
        assertNotNull(g);
        LocalSession session = new LocalSession();
        DocumentModelImpl doc = new DocumentModelImpl("/", "test", "Folder");
        assertTrue(g.check(null, doc));

        doc = new DocumentModelImpl("/", "test", "File");
        assertFalse(g.check(null, doc));

        doc = new DocumentModelImpl("/", "test", "Workspace");
        assertTrue(g.check(null, doc));

        ad = od.getAction("myAction2");
        g = ad.getGuard();
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        NuxeoPrincipal principal = new UserPrincipal("bogdan");
        ctx.put("principal", principal);
        session.connect("demo", ctx);
        assertTrue(g.check(session, doc));

        ad = od.getAction("myAction3");
        g = ad.getGuard();
        doc.setProperty("dublincore", "title", "test");
        assertEquals("test", doc.getTitle());
        assertTrue(g.check(session, doc));
        doc.setProperty("dublincore", "title", "test3");
        assertFalse(g.check(session, doc));

        ad = od.getAction("myAction4");
        g = ad.getGuard();
        assertFalse(g.check(session, doc));
        doc.setProperty("dublincore", "title", "test.py");
        assertEquals("test.py", doc.getTitle());
        assertTrue(g.check(session, doc));
    }

}
