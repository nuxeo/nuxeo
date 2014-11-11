/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Anahide Tchertchian
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.security;

import static org.nuxeo.ecm.core.CoreUTConstants.CORE_BUNDLE;
import static org.nuxeo.ecm.core.CoreUTConstants.CORE_TESTS_BUNDLE;
import static org.nuxeo.ecm.core.api.security.Access.DENY;
import static org.nuxeo.ecm.core.api.security.Access.GRANT;
import static org.nuxeo.ecm.core.api.security.Access.UNKNOWN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_PROPERTIES;

import java.security.Principal;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.MockDocument;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestSecurityPolicyService extends NXRuntimeTestCase {

    static final String creator = "Bodie";

    static final String user = "Bubbles";

    static final Principal creatorPrincipal = new UserPrincipal("Bodie",
            new ArrayList<String>(), false, false);

    static final Principal userPrincipal = new UserPrincipal("Bubbles",
            new ArrayList<String>(), false, false);

    private SecurityPolicyService service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployContrib(CORE_BUNDLE, "OSGI-INF/SecurityService.xml");
        deployContrib(CORE_BUNDLE, "OSGI-INF/permissions-contrib.xml");
        deployContrib(CORE_BUNDLE, "OSGI-INF/security-policy-contrib.xml");
        service = Framework.getService(SecurityPolicyService.class);
        assertNotNull(service);

    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        service = null;
    }

    public void testPolicies() throws Exception {
        String permission = WRITE;
        String[] permissions = { WRITE };
        Document doc = new MockDocument("Test", creator);

        // without lock
        assertSame(UNKNOWN, service.checkPermission(doc, null,
                creatorPrincipal, permission, permissions, null));
        assertSame(UNKNOWN, service.checkPermission(doc, null, userPrincipal,
                permission, permissions, null));

        // with lock
        doc.setLock(new Lock(user, new GregorianCalendar()));
        assertSame(DENY, service.checkPermission(doc, null, creatorPrincipal,
                permission, permissions, null));
        assertSame(UNKNOWN, service.checkPermission(doc, null, userPrincipal,
                permission, permissions, null));

        // test creator policy with lower order takes over lock
        deployContrib(CORE_TESTS_BUNDLE, "test-security-policy-contrib.xml");
        assertSame(GRANT, service.checkPermission(doc, null, creatorPrincipal,
                permission, permissions, null));
        assertSame(UNKNOWN, service.checkPermission(doc, null, userPrincipal,
                permission, permissions, null));
    }

    public void testCheckOutPolicy() throws Exception {
        String permission = WRITE;
        String[] permissions = { WRITE, WRITE_PROPERTIES };
        MockDocument doc = new MockDocument("uuid1", null);

        doc.checkedout = true;
        assertSame(UNKNOWN, service.checkPermission(doc, null,
                creatorPrincipal, permission, permissions, null));

        doc.checkedout = false;
        assertSame(UNKNOWN, service.checkPermission(doc, null,
                creatorPrincipal, permission, permissions, null));

        deployContrib(CORE_TESTS_BUNDLE, "test-security-policy2-contrib.xml");

        assertSame(DENY, service.checkPermission(doc, null, creatorPrincipal,
                permission, permissions, null));
    }

}
