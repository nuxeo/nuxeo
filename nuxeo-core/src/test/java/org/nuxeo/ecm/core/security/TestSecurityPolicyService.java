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
 * $Id$
 */

package org.nuxeo.ecm.core.security;

import java.security.Principal;

import org.nuxeo.ecm.core.CoreUTConstants;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.security.Access;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Anahide Tchertchian
 */
public class TestSecurityPolicyService extends NXRuntimeTestCase {

    static final String creator = "Bodie";

    static final String user = "Bubbles";

    static final Principal creatorPrincipal = new UserPrincipal("Bodie");

    static final Principal userPrincipal = new UserPrincipal("Bubbles");

    private SecurityPolicyService service;

    private Document doc;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib(CoreUTConstants.CORE_BUNDLE,
                "OSGI-INF/SecurityService.xml");
        deployContrib(CoreUTConstants.CORE_BUNDLE,
                "OSGI-INF/permissions-contrib.xml");
        deployContrib(CoreUTConstants.CORE_BUNDLE,
                "OSGI-INF/security-policy-contrib.xml");
        service = Framework.getService(SecurityPolicyService.class);
        assertNotNull(service);

        doc = new MockDocument("Test", creator);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        service = null;
        doc = null;
    }

    public void testPolicies() throws Exception {
        String permission = SecurityConstants.WRITE;
        String[] permissions = { SecurityConstants.WRITE };

        // without lock
        assertEquals(Access.UNKNOWN, service.checkPermission(doc, null,
                creatorPrincipal, permission, permissions, null));
        assertEquals(Access.UNKNOWN, service.checkPermission(doc, null,
                userPrincipal, permission, permissions, null));

        // with lock
        doc.setLock(user + ':');
        assertEquals(Access.DENY, service.checkPermission(doc, null,
                creatorPrincipal, permission, permissions, null));
        assertEquals(Access.UNKNOWN, service.checkPermission(doc, null,
                userPrincipal, permission, permissions, null));

        // test creator policy takes over lock
        deployContrib(CoreUTConstants.CORE_TESTS_BUNDLE,
                "test-security-policy-contrib.xml");
        assertEquals(Access.GRANT, service.checkPermission(doc, null,
                creatorPrincipal, permission, permissions, null));
        assertEquals(Access.UNKNOWN, service.checkPermission(doc, null,
                userPrincipal, permission, permissions, null));
    }

}
