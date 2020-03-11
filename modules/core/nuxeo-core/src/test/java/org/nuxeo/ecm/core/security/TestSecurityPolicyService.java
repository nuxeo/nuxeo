/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertSame;
import static org.nuxeo.ecm.core.api.security.Access.DENY;
import static org.nuxeo.ecm.core.api.security.Access.GRANT;
import static org.nuxeo.ecm.core.api.security.Access.UNKNOWN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE_PROPERTIES;

import java.util.ArrayList;
import java.util.GregorianCalendar;

import javax.inject.Inject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core:OSGI-INF/SecurityService.xml")
@Deploy("org.nuxeo.ecm.core:OSGI-INF/permissions-contrib.xml")
@Deploy("org.nuxeo.ecm.core:OSGI-INF/security-policy-contrib.xml")
public class TestSecurityPolicyService {

    static final String creator = "Bodie";

    static final String user = "Bubbles";

    static final NuxeoPrincipal creatorPrincipal = new UserPrincipal("Bodie", new ArrayList<>(), false, false);

    static final NuxeoPrincipal userPrincipal = new UserPrincipal("Bubbles", new ArrayList<>(), false, false);

    @Inject
    public SecurityPolicyService service;

    @Inject
    protected HotDeployer hotDeployer;

    protected Mockery mockery = new JUnit4Mockery();

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
        hotDeployer.deploy("org.nuxeo.ecm.core.tests:test-security-policy-contrib.xml");

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

        hotDeployer.deploy("org.nuxeo.ecm.core.tests:test-security-policy2-contrib.xml");

        assertSame(DENY, service.checkPermission(doc2, null, creatorPrincipal, permission, permissions, null));
    }

}
