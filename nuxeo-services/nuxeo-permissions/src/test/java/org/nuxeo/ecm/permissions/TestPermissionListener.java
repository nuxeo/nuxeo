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

package org.nuxeo.ecm.permissions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.WRITE;
import static org.nuxeo.ecm.permissions.Constants.ACE_INFO_DIRECTORY;
import static org.nuxeo.ecm.permissions.Constants.COMMENT_KEY;
import static org.nuxeo.ecm.permissions.Constants.NOTIFY_KEY;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.permissions" })
public class TestPermissionListener {

    @Inject
    protected CoreSession session;

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void shouldFillDirectory() {
        DocumentModel doc = createTestDocument();

        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL();
        ACE fryACE = new ACE("fry", WRITE, true);
        ACE leelaACE = new ACE("leela", READ, true);
        acl.add(fryACE);
        acl.add(leelaACE);
        acp.addACL(acl);
        doc.setACP(acp, true);

        Session session = null;
        try {
            session = directoryService.open(ACE_INFO_DIRECTORY);
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("docId", doc.getId());
            DocumentModelList entries = session.query(filter);
            assertEquals(2, entries.size());

            DocumentModel entry = entries.get(0);
            assertEquals(doc.getRepositoryName(), entry.getPropertyValue("aceinfo:repositoryName"));
            assertEquals("local", entry.getPropertyValue("aceinfo:aclName"));
            assertEquals(fryACE.getId(), entry.getPropertyValue("aceinfo:aceId"));

            entry = entries.get(1);
            assertEquals(doc.getRepositoryName(), entry.getPropertyValue("aceinfo:repositoryName"));
            assertEquals("local", entry.getPropertyValue("aceinfo:aclName"));
            assertEquals(leelaACE.getId(), entry.getPropertyValue("aceinfo:aceId"));
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    protected DocumentModel createTestDocument() {
        DocumentModel doc = session.createDocumentModel("/", "file", "File");
        return session.createDocument(doc);
    }

    @Test
    public void shouldUpdateDirectory() {
        DocumentModel doc = createTestDocument();

        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("fry", WRITE, true));
        acl.add(new ACE("leela", READ, true));
        acp.addACL(acl);
        doc.setACP(acp, true);

        acp = doc.getACP();
        acl = acp.getOrCreateACL();
        acl.clear();

        ACE fryACE = new ACE("fry", READ, true);
        acl.add(fryACE);
        acp.addACL(acl);
        doc.setACP(acp, true);
        Session session = null;
        try {
            session = directoryService.open(ACE_INFO_DIRECTORY);
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("docId", doc.getId());
            DocumentModelList entries = session.query(filter);
            assertEquals(1, entries.size());

            DocumentModel entry = entries.get(0);
            assertEquals(doc.getRepositoryName(), entry.getPropertyValue("aceinfo:repositoryName"));
            assertEquals("local", entry.getPropertyValue("aceinfo:aclName"));
            assertEquals(fryACE.getId(), entry.getPropertyValue("aceinfo:aceId"));

            acp = doc.getACP();
            acl = acp.getOrCreateACL();
            acl.clear();
            acp.addACL(acl);
            doc.setACP(acp, true);
            entries = session.query(filter);
            assertTrue(entries.isEmpty());
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Test
    public void shouldStoreNotifyAndComment() {
        DocumentModel doc = createTestDocument();

        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL();
        ACE fryACE = new ACE("fry", WRITE, true);
        fryACE.putContextData(NOTIFY_KEY, true);
        fryACE.putContextData(COMMENT_KEY, "fry comment");
        ACE leelaACE = new ACE("leela", READ, true);
        acl.add(fryACE);
        acl.add(leelaACE);
        acp.addACL(acl);
        doc.setACP(acp, true);

        Session session = null;
        try {
            session = directoryService.open(ACE_INFO_DIRECTORY);
            Map<String, Serializable> filter = new HashMap<>();
            filter.put("docId", doc.getId());
            DocumentModelList entries = session.query(filter);
            assertEquals(2, entries.size());

            DocumentModel entry = entries.get(0);
            assertEquals(fryACE.getId(), entry.getPropertyValue("aceinfo:aceId"));
            assertTrue((Boolean) entry.getPropertyValue("aceinfo:notify"));
            assertEquals("fry comment", entry.getPropertyValue("aceinfo:comment"));

            entry = entries.get(1);
            assertEquals(leelaACE.getId(), entry.getPropertyValue("aceinfo:aceId"));
            assertNull(entry.getPropertyValue("aceinfo:notify"));
            assertNull(entry.getPropertyValue("aceinfo:comment"));
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
