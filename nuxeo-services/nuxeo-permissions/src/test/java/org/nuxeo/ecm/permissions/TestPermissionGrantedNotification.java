/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.security.UpdateACEStatusListener;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 8.1
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.permissions")
@Deploy("org.nuxeo.ecm.permissions:test-listeners-contrib.xml")
public class TestPermissionGrantedNotification {

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Before
    public void before() {
        DummyPermissionGrantedNotificationListener.processedACEs.clear();
    }

    protected DocumentModel createTestDocument() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        return session.createDocument(doc);
    }

    @Test
    public void shouldTriggerPermissionNotificationListener() {
        DocumentModel doc = createTestDocument();

        ACE fryACE = ACE.builder("fry", WRITE).build();
        ACP acp = doc.getACP();
        acp.addACE(ACL.LOCAL_ACL, fryACE);
        doc.setACP(acp, true);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        assertEquals(0, DummyPermissionGrantedNotificationListener.processedACEs.size());
        TransactionHelper.startTransaction();

        Map<String, Serializable> contextData = new HashMap<>();
        contextData.put(Constants.NOTIFY_KEY, true);
        ACE leelaACE = ACE.builder("leela", WRITE).contextData(contextData).build();
        acp = doc.getACP();
        acp.addACE(ACL.LOCAL_ACL, leelaACE);
        doc.setACP(acp, true);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();

        assertEquals(1, DummyPermissionGrantedNotificationListener.processedACEs.size());
    }

    @Test
    public void shouldTriggerOnlyOnceForAnACE() throws InterruptedException {
        DocumentModel doc = createTestDocument();

        Map<String, Serializable> contextData = new HashMap<>();
        contextData.put(Constants.NOTIFY_KEY, true);
        ACE leelaACE = ACE.builder("leela", WRITE).contextData(contextData).build();
        ACP acp = doc.getACP();
        acp.addACE(ACL.LOCAL_ACL, leelaACE);

        Calendar begin = new GregorianCalendar();
        begin.setTimeInMillis(Instant.now().plus(5, ChronoUnit.SECONDS).toEpochMilli());
        ACE fryACE = ACE.builder("fry", WRITE).begin(begin).contextData(contextData).build();
        acp.addACE(ACL.LOCAL_ACL, fryACE);
        ACE benderACE = ACE.builder("bender", WRITE).begin(begin).contextData(contextData).build();
        acp.addACE(ACL.LOCAL_ACL, benderACE);
        doc.setACP(acp, true);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
        // leela ACE which is permanent
        assertEquals(1, DummyPermissionGrantedNotificationListener.processedACEs.size());
        assertEquals("leela", DummyPermissionGrantedNotificationListener.processedACEs.get(0).getUsername());

        Thread.sleep(10000);

        eventService.fireEvent(UpdateACEStatusListener.UPDATE_ACE_STATUS_EVENT, new EventContextImpl());
        eventService.waitForAsyncCompletion();
        DummyPermissionGrantedNotificationListener.processedACEs.sort((o1, o2) -> o1.getUsername().compareTo(
                o2.getUsername()));
        assertEquals(3, DummyPermissionGrantedNotificationListener.processedACEs.size());
        assertEquals("bender", DummyPermissionGrantedNotificationListener.processedACEs.get(0).getUsername());
        assertEquals("fry", DummyPermissionGrantedNotificationListener.processedACEs.get(1).getUsername());
        assertEquals("leela", DummyPermissionGrantedNotificationListener.processedACEs.get(2).getUsername());
    }
}
