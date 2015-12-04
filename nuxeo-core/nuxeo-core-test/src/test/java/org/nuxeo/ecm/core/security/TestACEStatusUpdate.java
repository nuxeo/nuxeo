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

package org.nuxeo.ecm.core.security;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestACEStatusUpdate {

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Test
    public void shouldUpdateStatusToEffective() throws InterruptedException {
        DocumentModel doc = session.createDocumentModel("/", "folder", "Folder");
        doc = session.createDocument(doc);

        Calendar begin = new GregorianCalendar();
        begin.setTimeInMillis(Instant.now().plus(5, ChronoUnit.SECONDS).toEpochMilli());
        ACP acp = doc.getACP();
        ACE ace = ACE.builder("leela", "Read").creator("Administrator").begin(begin).build();
        acp.addACE(ACL.LOCAL_ACL, ace);
        doc.setACP(acp, true);

        ACE leelaACE = getACEFor(acp, "leela");
        assertNotNull(leelaACE);
        assertTrue(leelaACE.isPending());

        fireUpdateACEStatusEventAndWait();

        doc = session.getDocument(doc.getRef());
        acp = doc.getACP();
        leelaACE = getACEFor(acp, "leela");
        assertNotNull(leelaACE);
        assertTrue(leelaACE.isPending());

        // wait for the ACE to be effective
        Thread.sleep(10000);
        fireUpdateACEStatusEventAndWait();

        doc = session.getDocument(doc.getRef());
        acp = doc.getACP();
        leelaACE = getACEFor(acp, "leela");
        assertNotNull(leelaACE);
        assertTrue(leelaACE.isEffective());
    }

    protected ACE getACEFor(ACP acp, String username) {
        ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
        ACE foundACE = null;
        for (ACE ace : acl) {
            if (username.equals(ace.getUsername())) {
                foundACE = ace;
            }
        }
        return foundACE;
    }

    protected void fireUpdateACEStatusEventAndWait() {
        eventService.fireEvent(UpdateACEStatusListener.UPDATE_ACE_STATUS_EVENT, new EventContextImpl());
        eventService.waitForAsyncCompletion();
    }

    @Test
    public void shouldUpdateStatusToArchived() throws InterruptedException {
        DocumentModel doc = session.createDocumentModel("/", "folder", "Folder");
        doc = session.createDocument(doc);

        Date now = new Date();
        Calendar begin = new GregorianCalendar();
        begin.setTimeInMillis(now.toInstant().minus(5, ChronoUnit.DAYS).toEpochMilli());
        Calendar end = new GregorianCalendar();
        end.setTimeInMillis(Instant.now().plus(5, ChronoUnit.SECONDS).toEpochMilli());
        ACP acp = doc.getACP();
        ACE ace = ACE.builder("leela", "Read").creator("Administrator").begin(begin).end(end).build();
        acp.addACE(ACL.LOCAL_ACL, ace);
        doc.setACP(acp, true);

        ACE leelaACE = getACEFor(acp, "leela");
        assertNotNull(leelaACE);
        assertTrue(leelaACE.isEffective());

        fireUpdateACEStatusEventAndWait();

        doc = session.getDocument(doc.getRef());
        acp = doc.getACP();
        leelaACE = getACEFor(acp, "leela");
        assertNotNull(leelaACE);
        assertTrue(leelaACE.isEffective());

        // wait for the ACE to be effective
        Thread.sleep(10000);
        fireUpdateACEStatusEventAndWait();

        doc = session.getDocument(doc.getRef());
        acp = doc.getACP();
        leelaACE = getACEFor(acp, "leela");
        assertNotNull(leelaACE);
        assertTrue(leelaACE.isArchived());
    }
}
