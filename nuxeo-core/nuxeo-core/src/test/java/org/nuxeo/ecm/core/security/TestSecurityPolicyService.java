/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import org.junit.Before;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestSecurityPolicyService extends NXRuntimeTestCase {

    static final String creator = "Bodie";

    static final String user = "Bubbles";

    static final Principal creatorPrincipal = new UserPrincipal("Bodie", new ArrayList<String>(), false, false);

    static final Principal userPrincipal = new UserPrincipal("Bubbles", new ArrayList<String>(), false, false);

    private SecurityPolicyService service;

    protected Mockery mockery = new JUnit4Mockery();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib(CORE_BUNDLE, "OSGI-INF/SecurityService.xml");
        deployContrib(CORE_BUNDLE, "OSGI-INF/permissions-contrib.xml");
        deployContrib(CORE_BUNDLE, "OSGI-INF/security-policy-contrib.xml");
        service = Framework.getService(SecurityPolicyService.class);
        assertNotNull(service);

    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        service = null;
    }

    @Test
    public void testPolicies() throws Exception {
        String permission = WRITE;
        String[] permissions = { WRITE };

        Document doc = mockery.mock(Document.class, "document1");
        mockery.checking(new Expectations() {
            {
                allowing(doc).getLock();
                will(returnValue(null));
            }
        });

        // without lock
        assertSame(UNKNOWN, service.checkPermission(doc, null, creatorPrincipal, permission, permissions, null));
        assertSame(UNKNOWN, service.checkPermission(doc, null, userPrincipal, permission, permissions, null));

        // with lock
        Lock lock = new Lock(user, new GregorianCalendar());
        Document doc2 = mockery.mock(Document.class, "document2");
        mockery.checking(new Expectations() {
            {
                allowing(doc2).getLock();
                will(returnValue(lock));
                allowing(doc2).getPropertyValue("dc:creator");
                will(returnValue(creator));
            }
        });

        assertSame(DENY, service.checkPermission(doc2, null, creatorPrincipal, permission, permissions, null));
        assertSame(UNKNOWN, service.checkPermission(doc2, null, userPrincipal, permission, permissions, null));

        // test creator policy with lower order takes over lock
        deployContrib(CORE_TESTS_BUNDLE, "test-security-policy-contrib.xml");
        assertSame(GRANT, service.checkPermission(doc2, null, creatorPrincipal, permission, permissions, null));
        assertSame(UNKNOWN, service.checkPermission(doc2, null, userPrincipal, permission, permissions, null));
    }

    @Test
    public void testCheckOutPolicy() throws Exception {
        String permission = WRITE;
        String[] permissions = { WRITE, WRITE_PROPERTIES };

        // checked out

        Document doc = mockery.mock(Document.class, "document3");
        mockery.checking(new Expectations() {
            {
                allowing(doc).getLock();
                will(returnValue(null));
                allowing(doc).isVersion();
                will(returnValue(Boolean.FALSE));
                allowing(doc).isProxy();
                will(returnValue(Boolean.FALSE));
                allowing(doc).isCheckedOut();
                will(returnValue(Boolean.TRUE));
            }
        });

        assertSame(UNKNOWN, service.checkPermission(doc, null, creatorPrincipal, permission, permissions, null));

        // not checked out

        Document doc2 = mockery.mock(Document.class, "document4");
        mockery.checking(new Expectations() {
            {
                allowing(doc2).getLock();
                will(returnValue(null));
                allowing(doc2).isVersion();
                will(returnValue(Boolean.FALSE));
                allowing(doc2).isProxy();
                will(returnValue(Boolean.FALSE));
                allowing(doc2).isCheckedOut();
                will(returnValue(Boolean.FALSE));
            }
        });

        assertSame(UNKNOWN, service.checkPermission(doc2, null, creatorPrincipal, permission, permissions, null));

        deployContrib(CORE_TESTS_BUNDLE, "test-security-policy2-contrib.xml");

        assertSame(DENY, service.checkPermission(doc2, null, creatorPrincipal, permission, permissions, null));
    }

}
