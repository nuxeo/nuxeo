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

package org.nuxeo.ecm.platform.site.tests.security;

import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.site.ObjectDescriptor;
import org.nuxeo.ecm.platform.site.SiteManager;
import org.nuxeo.ecm.platform.site.actions.ActionDescriptor;
import org.nuxeo.ecm.platform.site.security.Guard;
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
        deployContrib("OSGI-INF/site-manager-framework.xml");
        deployContrib("OSGI-INF/test-security-guards.xml");
    }

    public void testGuardRegistration() throws Exception {

        SiteManager mgr = Framework.getLocalService(SiteManager.class);
        assertNotNull(mgr);
        ObjectDescriptor od = mgr.getObject("siteFolder");
        assertNotNull(od);
        ActionDescriptor ad = od.getAction("view");
        assertNotNull(ad);
        assertNotNull(ad.getGuard());
        ad = od.getAction("myAction1");
        Guard g = ad.getGuard();
        assertNotNull(g);
        DocumentModelImpl doc = new DocumentModelImpl("/", "test", "Folder");
        assertTrue(g.check(null, doc));
        doc = new DocumentModelImpl("/", "test", "File");
        assertFalse(g.check(null, doc));
        doc = new DocumentModelImpl("/", "test", "Workspace");
        assertTrue(g.check(null, doc));
        //PermissionService.getInstance().getGuard("");

    }

}
