/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.Calendar;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.helpers.RetentionHelper;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.5
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
public class TestRetentionHelper {

    public static final String JOHN = "John";

    @Inject
    protected CoreSession session;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected ConfigurationService configurationService;

    @Test
    public void iCanDeleteDocumentNotUnderRetentionOrLegalHold() {
        DocumentModel documentModel = createDocument(session);
        RetentionHelper.checkMainContentDeletion(getDocument(documentModel), session.getPrincipal());
        RetentionHelper.checkDeletion(getDocument(documentModel), session.getPrincipal());

        // Give the permission to another user
        ACP acp = documentModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE(JOHN, SecurityConstants.READ, true));
        acl.add(new ACE(JOHN, SecurityConstants.REMOVE, true));
        session.setACP(documentModel.getRef(), acp, false);
        session.save();
        CoreSession notAdminSession = coreFeature.getCoreSession(JOHN);
        RetentionHelper.checkMainContentDeletion(getDocument(documentModel), notAdminSession.getPrincipal());
        RetentionHelper.checkDeletion(getDocument(documentModel), notAdminSession.getPrincipal());
    }

    @Test
    public void iCannotDeleteDocumentUnderRetentionOrLegalHold() {
        // Make a document under legal hold
        DocumentModel documentModel = createDocument(session);
        session.makeRecord(documentModel.getRef());
        session.setLegalHold(documentModel.getRef(), true, null);

        try {
            RetentionHelper.checkMainContentDeletion(getDocument(documentModel), session.getPrincipal());
            fail();
        } catch (DocumentSecurityException e) {
            assertEquals(String.format("Cannot delete blob from document %s, it is under retention / hold",
                    documentModel.getRef()), e.getMessage());
        }

        try {
            RetentionHelper.checkDeletion(getDocument(documentModel), session.getPrincipal());
            fail();
        } catch (DocumentSecurityException e) {
            assertEquals(String.format("Cannot remove %s, it is under retention / hold", documentModel.getRef()),
                    e.getMessage());
        }

        // Make a document under retention
        documentModel = createDocument(session);
        Calendar retainUntil = Calendar.getInstance();
        retainUntil.add(Calendar.DAY_OF_MONTH, 5);
        session.makeRecord(documentModel.getRef());
        session.setRetainUntil(documentModel.getRef(), retainUntil, "any comment");
        try {
            RetentionHelper.checkMainContentDeletion(getDocument(documentModel), session.getPrincipal());
            fail();
        } catch (DocumentSecurityException e) {
            assertEquals(String.format("Cannot delete blob from document %s, it is under retention / hold",
                    documentModel.getRef()), e.getMessage());
        }

        try {
            RetentionHelper.checkDeletion(getDocument(documentModel), session.getPrincipal());
            fail();
        } catch (DocumentSecurityException e) {
            assertEquals(String.format("Cannot remove %s, it is under retention / hold", documentModel.getRef()),
                    e.getMessage());
        }

        // Give the permission to another user
        ACP acp = documentModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE(JOHN, SecurityConstants.READ, true));
        acl.add(new ACE(JOHN, SecurityConstants.REMOVE, true));
        session.setACP(documentModel.getRef(), acp, false);
        session.save();
        CoreSession notAdminSession = coreFeature.getCoreSession(JOHN);
        try {
            RetentionHelper.checkMainContentDeletion(getDocument(documentModel), notAdminSession.getPrincipal());
            fail();
        } catch (DocumentSecurityException e) {
            assertEquals(String.format("Cannot delete blob from document %s, it is under retention / hold",
                    documentModel.getRef()), e.getMessage());
        }

        try {
            RetentionHelper.checkDeletion(getDocument(documentModel), notAdminSession.getPrincipal());
            fail();
        } catch (DocumentSecurityException e) {
            assertEquals(String.format("Cannot remove %s, it is under retention / hold", documentModel.getRef()),
                    e.getMessage());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/retention-enable-governance-mode.xml")
    public void iCanDeleteDocumentUnderRetentionOrLegalHold() {
        // Make a document under legal hold
        DocumentModel documentModel = createDocument(session);
        session.makeRecord(documentModel.getRef());
        session.setLegalHold(documentModel.getRef(), true, null);
        RetentionHelper.checkMainContentDeletion(getDocument(documentModel), session.getPrincipal());
        RetentionHelper.checkDeletion(getDocument(documentModel), session.getPrincipal());

        // Make a document under retention
        documentModel = createDocument(session);
        Calendar retainUntil = Calendar.getInstance();
        retainUntil.add(Calendar.DAY_OF_MONTH, 8);
        session.makeRecord(documentModel.getRef());
        session.setRetainUntil(documentModel.getRef(), retainUntil, "any comment");
        RetentionHelper.checkMainContentDeletion(getDocument(documentModel), session.getPrincipal());
        RetentionHelper.checkDeletion(getDocument(documentModel), session.getPrincipal());

        // Not Admin user without the right permissions, shouldn't remove the document
        ACP acp = documentModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE(JOHN, SecurityConstants.READ, true));
        acl.add(new ACE(JOHN, SecurityConstants.REMOVE, true));
        session.setACP(documentModel.getRef(), acp, false);
        session.save();
        CoreSession notAdminSession = coreFeature.getCoreSession(JOHN);
        try {
            RetentionHelper.checkMainContentDeletion(getDocument(documentModel), notAdminSession.getPrincipal());
            fail();
        } catch (DocumentSecurityException e) {
            assertEquals(String.format("Cannot delete blob from document %s, it is under retention / hold",
                    documentModel.getRef()), e.getMessage());
        }

        try {
            RetentionHelper.checkDeletion(getDocument(documentModel), notAdminSession.getPrincipal());
            fail();
        } catch (DocumentSecurityException e) {
            assertEquals(String.format("Cannot remove %s, it is under retention / hold", documentModel.getRef()),
                    e.getMessage());
        }

        // Give the needed permission for document deletion under retention/legal hold
        acl.add(new ACE(JOHN, RetentionHelper.REMOVE_RECORD_PERMISSION, true));
        session.setACP(documentModel.getRef(), acp, false);
        session.save();
        RetentionHelper.checkMainContentDeletion(getDocument(documentModel), notAdminSession.getPrincipal());
        RetentionHelper.checkDeletion(getDocument(documentModel), notAdminSession.getPrincipal());
    }

    protected DocumentModel createDocument(CoreSession session) {
        DocumentModel documentModel = session.createDocumentModel("/", "myDocument", "File");
        Blob blob = Blobs.createBlob("Any Content", "text/plain");
        documentModel.setPropertyValue("file:content", (Serializable) blob);
        documentModel = session.createDocument(documentModel);
        documentModel = session.saveDocument(documentModel);
        return documentModel;
    }

    protected Document getDocument(DocumentModel documentModel) {
        return ((AbstractSession) session).getSession().getDocumentByUUID(documentModel.getId());
    }

}
