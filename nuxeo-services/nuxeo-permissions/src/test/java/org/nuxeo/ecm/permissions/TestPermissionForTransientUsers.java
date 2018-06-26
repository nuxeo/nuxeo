/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.permissions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;

import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.security.UpdateACEStatusListener;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.tokenauth.TokenAuthenticationServiceFeature;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 8.1
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class, TokenAuthenticationServiceFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.permissions")
public class TestPermissionForTransientUsers {

    @Inject
    protected TokenAuthenticationService tokenAuthenticationService;

    @Inject
    protected EventService eventService;

    @Inject
    protected CoreSession session;

    protected DocumentModel doc;

    @Before
    public void createTestDocument() {
        doc = session.createDocumentModel("/", "file", "File");
        doc = session.createDocument(doc);
    }

    @Test
    public void shouldCreateTokenForTransientUser() {
        String transientUsername = NuxeoPrincipal.computeTransientUsername("leela@nuxeo.com");
        ACE leelaACE = new ACE(transientUsername, WRITE, true);
        ACP acp = doc.getACP();
        acp.addACE(ACL.LOCAL_ACL, leelaACE);
        doc.setACP(acp, true);

        String token = tokenAuthenticationService.getToken(transientUsername, doc.getRepositoryName(), doc.getId());
        assertNotNull(token);
    }

    @Test
    public void shouldRemoveTokenWhenRemovingACEForTransientUser() {
        String transientUsername = NuxeoPrincipal.computeTransientUsername("leela@nuxeo.com");
        ACE leelaACE = new ACE(transientUsername, WRITE, true);
        ACP acp = doc.getACP();
        acp.addACE(ACL.LOCAL_ACL, leelaACE);
        doc.setACP(acp, true);

        String token = tokenAuthenticationService.getToken(transientUsername, doc.getRepositoryName(), doc.getId());
        assertNotNull(token);

        acp = doc.getACP();
        acp.removeACE(ACL.LOCAL_ACL, leelaACE);
        doc.setACP(acp, true);
        token = tokenAuthenticationService.getToken(transientUsername, doc.getRepositoryName(), doc.getId());
        assertNull(token);
    }

    @Test
    public void shouldRemoveTokenWhenArchivingACEForTransientUser() throws InterruptedException {
        Date now = new Date();
        Calendar end = new GregorianCalendar();
        end.setTimeInMillis(now.toInstant().plus(5, ChronoUnit.SECONDS).toEpochMilli());
        String transientUsername = NuxeoPrincipal.computeTransientUsername("leela@nuxeo.com");
        ACE leelaACE = ACE.builder(transientUsername, WRITE).end(end).build();
        ACP acp = doc.getACP();
        acp.addACE(ACL.LOCAL_ACL, leelaACE);
        doc.setACP(acp, true);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
        String token = tokenAuthenticationService.getToken(transientUsername, doc.getRepositoryName(), doc.getId());
        assertNotNull(token);

        Thread.sleep(10000);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.fireEvent(UpdateACEStatusListener.UPDATE_ACE_STATUS_EVENT, new EventContextImpl());
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();

        token = tokenAuthenticationService.getToken(transientUsername, doc.getRepositoryName(), doc.getId());
        assertNull(token);
    }

    @Test
    public void shouldNotRemoveTokenIfUserStillHavePendingOrEffectiveACE() {
        String transientUsername = NuxeoPrincipal.computeTransientUsername("leela@nuxeo.com");
        ACE ace1 = ACE.builder(transientUsername, WRITE).build();
        ACE ace2 = ACE.builder(transientUsername, READ).build();
        ACP acp = doc.getACP();
        acp.addACE(ACL.LOCAL_ACL, ace1);
        acp.addACE(ACL.LOCAL_ACL, ace2);
        doc.setACP(acp, true);

        String token = tokenAuthenticationService.getToken(transientUsername, doc.getRepositoryName(), doc.getId());
        assertNotNull(token);

        ACE newAce1 = (ACE) ace1.clone();
        Date now = new Date();
        Calendar end = new GregorianCalendar();
        end.setTimeInMillis(now.toInstant().minus(1, ChronoUnit.DAYS).toEpochMilli());
        newAce1.setEnd(end);
        acp = doc.getACP();
        acp.replaceACE(ACL.LOCAL_ACL, ace1, newAce1);
        doc.setACP(acp, true);

        String token2 = tokenAuthenticationService.getToken(transientUsername, doc.getRepositoryName(), doc.getId());
        assertNotNull(token2);
        assertEquals(token, token2);

        acp = doc.getACP();
        acp.removeACE(ACL.LOCAL_ACL, ace2);
        doc.setACP(acp, true);

        token = tokenAuthenticationService.getToken(transientUsername, doc.getRepositoryName(), doc.getId());
        assertNull(token);
    }

}
