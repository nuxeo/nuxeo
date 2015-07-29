/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.core.security;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    public void shouldUpdateStatusToEffective() {
        DocumentModel doc = session.createDocumentModel("/", "folder", "Folder");
        doc = session.createDocument(doc);

        Date now = new Date();
        Calendar begin = new GregorianCalendar();
        begin.setTimeInMillis(now.toInstant().plus(1, ChronoUnit.DAYS).toEpochMilli());
        ACP acp = doc.getACP();
        ACE ace = ACE.builder("leela", "Read").creator("Administrator").begin(begin).build();
        acp.addACE(ACL.LOCAL_ACL, ace, false);
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

        // make the ACE effective
        begin = new GregorianCalendar();
        begin.setTimeInMillis(now.toInstant().minus(1, ChronoUnit.DAYS).toEpochMilli());
        acp.updateACE(ACL.LOCAL_ACL, leelaACE.getId(), leelaACE.getUsername(), leelaACE.getPermission(), false,
                leelaACE.getCreator(), begin, null, null);
        doc.setACP(acp, true);

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
    public void shouldUpdateStatusToArchived() {
        DocumentModel doc = session.createDocumentModel("/", "folder", "Folder");
        doc = session.createDocument(doc);

        Date now = new Date();
        Calendar begin = new GregorianCalendar();
        begin.setTimeInMillis(now.toInstant().minus(5, ChronoUnit.DAYS).toEpochMilli());
        ACP acp = doc.getACP();
        ACE ace = ACE.builder("leela", "Read").creator("Administrator").begin(begin).build();
        acp.addACE(ACL.LOCAL_ACL, ace, false);
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

        // make the ACE archived
        Calendar end = new GregorianCalendar();
        end.setTimeInMillis(now.toInstant().minus(1, ChronoUnit.DAYS).toEpochMilli());
        acp.updateACE(ACL.LOCAL_ACL, leelaACE.getId(), leelaACE.getUsername(), leelaACE.getPermission(), false,
                leelaACE.getCreator(), null, end, null);
        doc.setACP(acp, true);

        fireUpdateACEStatusEventAndWait();

        doc = session.getDocument(doc.getRef());
        acp = doc.getACP();
        leelaACE = getACEFor(acp, "leela");
        assertNotNull(leelaACE);
        assertTrue(leelaACE.isArchived());
    }
}
