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
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebObjectDescriptor;
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class GuardTest extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("nuxeo-runtime-scripting");
        deployBundle("nuxeo-core-schema");
        deployBundle("nuxeo-core-query");
        deployBundle("nuxeo-core-api");
        deployBundle("nuxeo-core");
        deployBundle("nuxeo-webengine-core");
        deployContrib("OSGI-INF/DemoRepository.xml");
        deployContrib("OSGI-INF/test-security-guards.xml");
    }

    public void testGuardRegistration() throws Exception {

        WebEngine mgr = Framework.getLocalService(WebEngine.class);
        assertNotNull(mgr);

        WebObjectDescriptor od = mgr.getObject("siteFolder2");
        assertNotNull(od);

        ActionDescriptor ad = od.getAction("view");
        assertNotNull(ad);
        assertNotNull(ad.getGuard());

        ad = od.getAction("myAction1");
        Guard g = ad.getGuard();
        assertNotNull(g);
        LocalSession session = new LocalSession();
        DocumentModelImpl doc = new DocumentModelImpl("/", "test", "Folder");
        SecurityContext context = new SecurityContext(session, doc);
        assertTrue(g.check(context));

        doc = new DocumentModelImpl("/", "test", "File");
        context = new SecurityContext(session, doc);
        assertFalse(g.check(context));

        doc = new DocumentModelImpl("/", "test", "Workspace");
        context = new SecurityContext(session, doc);
        assertTrue(g.check(context));

        ad = od.getAction("myAction2");
        g = ad.getGuard();
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        NuxeoPrincipal principal = new UserPrincipal("bogdan");
        ctx.put("principal", principal);
        session.connect("demo", ctx);
        context = new SecurityContext(session, doc);
        assertTrue(g.check(context));

        ad = od.getAction("myAction3");
        g = ad.getGuard();
        doc.setProperty("dublincore", "title", "test");
        assertEquals("test", doc.getTitle());
        context = new SecurityContext(session, doc);
        assertTrue(g.check(context));
        doc.setProperty("dublincore", "title", "test3");
        context = new SecurityContext(session, doc);
        assertFalse(g.check(context));

        ad = od.getAction("myAction4");
        g = ad.getGuard();
        context = new SecurityContext(session, doc);
        assertFalse(g.check(context));
        doc.setProperty("dublincore", "title", "test.py");
        assertEquals("test.py", doc.getTitle());
        context = new SecurityContext(session, doc);
        assertTrue(g.check(context));
    }

}
