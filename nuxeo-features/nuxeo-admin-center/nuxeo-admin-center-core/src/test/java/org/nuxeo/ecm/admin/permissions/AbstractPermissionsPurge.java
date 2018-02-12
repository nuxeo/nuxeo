/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */

package org.nuxeo.ecm.admin.permissions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.transientstore.test.TransientStoreFeature;

/**
 * @since 9.1
 */
@Features({ TransientStoreFeature.class, PlatformFeature.class })
@Deploy("org.nuxeo.admin.center:OSGI-INF/core-types-contrib.xml")
@Deploy("org.nuxeo.admin.center:OSGI-INF/pageproviders-contrib.xml")
public abstract class AbstractPermissionsPurge {

    @Inject
    protected CoreSession session;

    @Inject
    protected WorkManager workManager;

    public abstract void scheduleWork(List<String> username) throws Exception;

    @Test
    public void shouldArchiveACEs() throws Exception {
        DocumentModel doc = session.createDocumentModel("/", "afile", "File");
        doc = session.createDocument(doc);
        ACP acp = doc.getACP();
        acp.addACE(ACL.LOCAL_ACL, ACE.builder("leela", "Read").build());
        doc.setACP(acp, true);

        acp = doc.getACP();
        ACL acl = acp.getACL(ACL.LOCAL_ACL);
        assertEquals(1, acl.getACEs().length);
        ACE ace = acl.get(0);
        assertEquals("leela", ace.getUsername());
        assertEquals("Read", ace.getPermission());
        assertTrue(ace.isEffective());

        TransactionHelper.commitOrRollbackTransaction();

        scheduleWork(Collections.singletonList("leela"));

        workManager.awaitCompletion(10000, TimeUnit.SECONDS);

        TransactionHelper.startTransaction();

        doc = session.getDocument(doc.getRef());
        acp = doc.getACP();
        acl = acp.getACL(ACL.LOCAL_ACL);
        assertEquals(1, acl.getACEs().length);
        ace = acl.get(0);
        assertEquals("leela", ace.getUsername());
        assertEquals("Read", ace.getPermission());
        assertTrue(ace.isArchived());
    }
}
