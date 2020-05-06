/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.comment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newAnnotation;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newExternalAnnotation;

import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationService;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentSecurityException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features(CommentFeature.class)
public class TestAnnotationService {

    protected static final String JDOE = "jdoe";

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected CoreFeature coreFeature;

    /** The system session. */
    protected CloseableCoreSession systemSession;

    /** The jdoe session. */
    protected CloseableCoreSession session;

    @Inject
    protected AnnotationService annotationService;

    protected DocumentModel annotatedDocModel;

    @Before
    public void setup() {
        // create a domain with permissions for jdoe
        systemSession = coreFeature.openCoreSessionSystem();
        DocumentModel domain = systemSession.createDocumentModel("/", "domain", "Domain");
        systemSession.createDocument(domain);
        // create document to annotate by jdoe
        annotatedDocModel = systemSession.createDocumentModel("/domain", "test", "File");
        annotatedDocModel = systemSession.createDocument(annotatedDocModel);

        // give permission to annotate to jdoe
        ACP acp = annotatedDocModel.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("jdoe", SecurityConstants.READ, true));
        systemSession.setACP(annotatedDocModel.getRef(), acp, false);
        systemSession.save();

        session = coreFeature.openCoreSession(JDOE);
    }

    @After
    public void cleanUp() {
        systemSession.close();
        session.close();
    }

    @Test
    public void testCreateAnnotation() {
        String entityId = "foo";
        String xpathToAnnotate = "files:files/0/file";
        String comment = "test comment";
        String entity = "<entity><annotation>bar</annotation></entity>";

        Annotation annotation = newExternalAnnotation(annotatedDocModel.getId(), xpathToAnnotate, entityId, entity,
                comment);
        annotation = annotationService.createAnnotation(session, annotation);
        session.save();

        assertEquals(JDOE, annotation.getAuthor());
        assertEquals(comment, annotation.getText());
        assertEquals(annotatedDocModel.getId(), annotation.getParentId());
        assertTrue(annotation.getAncestorIds().contains(annotatedDocModel.getId()));
        assertNotNull(annotation.getCreationDate());
        assertNotNull(annotation.getModificationDate());
        assertEquals(xpathToAnnotate, annotation.getXpath());
        assertEquals(entityId, ((ExternalEntity) annotation).getEntityId());
        assertEquals("Test", ((ExternalEntity) annotation).getOrigin());

        try (CloseableCoreSession bobSession = coreFeature.openCoreSession("bob")) {
            annotationService.createAnnotation(bobSession, annotation);
            fail("bob should not be able to create annotation");
        } catch (CommentSecurityException e) {
            assertEquals("The user bob can not create comments on document " + annotatedDocModel.getId(),
                    e.getMessage());
        }
    }

    @Test
    public void testGetAnnotation() {
        String xpathToAnnotate = "files:files/0/file";

        String annotationId;
        // create annotation with admin session for permission check
        try (CloseableCoreSession adminSession = coreFeature.openCoreSessionSystem()) {
            Annotation annotation = newAnnotation(annotatedDocModel.getId(), xpathToAnnotate);
            annotationId = annotationService.createAnnotation(adminSession, annotation).getId();
        }

        annotationService.getAnnotation(session, annotationId);

        try (CloseableCoreSession bobSession = coreFeature.openCoreSession("bob")) {
            annotationService.getAnnotation(bobSession, annotationId);
            fail("bob should not be able to get annotation");
        } catch (CommentSecurityException e) {
            assertEquals("The user bob does not have access to the comments of document " + annotatedDocModel.getId(),
                    e.getMessage());
        }
    }

    @Test
    public void testUpdateAnnotation() {
        String xpathToAnnotate = "files:files/0/file";

        Annotation annotation = newAnnotation(annotatedDocModel.getId(), xpathToAnnotate);
        annotation = annotationService.createAnnotation(session, annotation);
        session.save();

        // now assert there's no entity and update the annotation with one
        assertNull(((ExternalEntity) annotation).getEntity());

        ((ExternalEntity) annotation).setEntityId("entityId");
        ((ExternalEntity) annotation).setEntity("Entity");
        annotationService.updateAnnotation(session, annotation.getId(), annotation);

        assertEquals("Entity",
                ((ExternalEntity) annotationService.getAnnotation(session, annotation.getId())).getEntity());

        try (CloseableCoreSession bobSession = coreFeature.openCoreSession("bob")) {
            annotationService.updateAnnotation(bobSession, annotation.getId(), annotation);
            fail("bob should not be able to edit annotation");
        } catch (CommentSecurityException e) {
            assertEquals("The user bob cannot edit comment " + annotation.getId(), e.getMessage());
        }
    }

    @Test
    public void testDeleteAnnotation() {
        String xpathToAnnotate = "files:files/0/file";

        Annotation annotation = newAnnotation(annotatedDocModel.getId(), xpathToAnnotate);
        annotation = annotationService.createAnnotation(session, annotation);
        session.save();

        assertTrue(systemSession.exists(new IdRef(annotation.getId())));

        try {
            annotationService.deleteAnnotation(session, "toto");
            fail("Deleting an unknown annotation should have failed");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        try (CloseableCoreSession bobSession = coreFeature.openCoreSession("bob")) {
            annotationService.deleteAnnotation(bobSession, annotation.getId());
            fail("bob should not be able to delete annotation");
        } catch (CommentSecurityException e) {
            assertEquals("The user bob can not delete comments of document " + annotatedDocModel.getId(),
                    e.getMessage());
        }

        annotationService.deleteAnnotation(session, annotation.getId());
        assertFalse(systemSession.exists(new IdRef(annotation.getId())));

    }

    @Test
    public void testGetAnnotationsForDocument() {
        String xpathToAnnotate = "files:files/0/file";

        List<Annotation> annotations = annotationService.getAnnotations(session, annotatedDocModel.getId(),
                xpathToAnnotate);
        assertTrue(annotations.isEmpty());

        int nbAnnotations = 99;
        for (int i = 0; i < nbAnnotations; i++) {
            annotationService.createAnnotation(session, newAnnotation(annotatedDocModel.getId(), xpathToAnnotate));
        }
        session.save();

        assertEquals(nbAnnotations,
                annotationService.getAnnotations(session, annotatedDocModel.getId(), xpathToAnnotate).size());

        try (CloseableCoreSession bobSession = coreFeature.openCoreSession("bob")) {
            annotationService.getAnnotations(bobSession, annotatedDocModel.getId(), xpathToAnnotate);
            fail("bob should not be able to get annotations");
        } catch (CommentSecurityException e) {
            assertEquals(
                    "The user bob does not have access to the annotations of document " + annotatedDocModel.getId(),
                    e.getMessage());
        }

    }

    @Test
    public void testGetExternalAnnotation() {
        String entityId = "foo";
        String xpathToAnnotate = "files:files/0/file";

        Annotation annotation = newExternalAnnotation(annotatedDocModel.getId(), xpathToAnnotate, entityId);
        annotationService.createAnnotation(session, annotation);
        session.save();
        // external comment uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        annotation = annotationService.getExternalAnnotation(session, annotatedDocModel.getId(), entityId);
        assertEquals(entityId, ((ExternalEntity) annotation).getEntityId());

        try (CloseableCoreSession bobSession = coreFeature.openCoreSession("bob")) {
            annotationService.getExternalAnnotation(bobSession, annotatedDocModel.getId(), entityId);
            fail("bob should not be able to get annotation");
        } catch (CommentSecurityException e) {
            assertEquals("The user bob does not have access to the comments of document " + annotatedDocModel.getId(),
                    e.getMessage());
        }
    }

    @Test
    public void testUpdateExternalAnnotation() {
        String xpathToAnnotate = "files:files/0/file";
        String entityId = "foo";

        Annotation annotation = newExternalAnnotation(annotatedDocModel.getId(), xpathToAnnotate, entityId);
        annotationService.createAnnotation(session, annotation);
        session.save();
        // external comment uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        String entity = "<entity></entity>";
        assertNull(((ExternalEntity) annotation).getEntity());

        ((ExternalEntity) annotation).setEntity(entity);
        try {
            annotationService.updateExternalAnnotation(session, "fakeId", entityId, annotation);
            fail("The external annotation should not exist");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }
        try {
            annotationService.updateExternalAnnotation(session, annotatedDocModel.getId(), "fakeId", annotation);
            fail("The external annotation should not exist");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        annotation = annotationService.updateExternalAnnotation(session, annotatedDocModel.getId(), entityId,
                annotation);
        assertEquals(entityId, ((ExternalEntity) annotation).getEntityId());

        try (CloseableCoreSession bobSession = coreFeature.openCoreSession("bob")) {
            annotationService.updateExternalAnnotation(bobSession, annotatedDocModel.getId(), entityId, annotation);
            fail("bob should not be able to edit annotation");
        } catch (CommentSecurityException e) {
            assertEquals("The user bob can not edit comments of document " + annotatedDocModel.getId(), e.getMessage());
        }
    }

    @Test
    public void testDeleteExternalAnnotation() {
        String xpathToAnnotate = "files:files/0/file";
        String entityId = "foo";

        Annotation annotation = newExternalAnnotation(annotatedDocModel.getId(), xpathToAnnotate, entityId);
        annotation = annotationService.createAnnotation(session, annotation);
        session.save();
        // external comment uses a page provider -> wait indexation
        transactionalFeature.nextTransaction();

        assertTrue(systemSession.exists(new IdRef(annotation.getId())));

        try {
            annotationService.deleteExternalAnnotation(session, "fakeId", entityId);
            fail("Deleting an unknown annotation should have failed");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        try {
            annotationService.deleteExternalAnnotation(session, annotatedDocModel.getId(), "fakeId");
            fail("Deleting an unknown annotation should have failed");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        try (CloseableCoreSession bobSession = coreFeature.openCoreSession("bob")) {
            annotationService.deleteExternalAnnotation(bobSession, annotatedDocModel.getId(), entityId);
            fail("bob should not be able to delete annotation");
        } catch (CommentSecurityException e) {
            assertEquals("The user bob does not have access to the comments of document " + annotatedDocModel.getId(),
                    e.getMessage());
        }

        annotationService.deleteExternalAnnotation(session, annotatedDocModel.getId(), entityId);
        assertFalse(systemSession.exists(new IdRef(annotation.getId())));
    }

    @Test
    public void testGetTopLevelAnnotationAncestor() {
        String xpathToAnnotate = "files:files/0/file";

        Annotation annotation = newAnnotation(annotatedDocModel.getId(), xpathToAnnotate);
        annotation = annotationService.createAnnotation(session, annotation);

        CommentManager commentManager = Framework.getService(CommentManager.class);

        // allow james to see annotation
        DocumentRef docRefToAnnotate = new IdRef(annotatedDocModel.getId());
        try (CloseableCoreSession systemSession = coreFeature.openCoreSessionSystem()) {
            ACP acp = session.getACP(docRefToAnnotate);
            ACL acl = acp.getOrCreateACL();
            acl.add(new ACE("james", SecurityConstants.READ, true));
            systemSession.setACP(docRefToAnnotate, acp, false);
            systemSession.save();

            assertEquals(docRefToAnnotate,
                    commentManager.getTopLevelDocumentRef(systemSession, new IdRef(annotation.getId())));
        }
        try (CloseableCoreSession jamesSession = coreFeature.openCoreSession("james")) {
            assertEquals(docRefToAnnotate,
                    commentManager.getTopLevelDocumentRef(jamesSession, new IdRef(annotation.getId())));
        }

        try (CloseableCoreSession janeSession = coreFeature.openCoreSession("jane")) {
            assertEquals(docRefToAnnotate,
                    commentManager.getTopLevelDocumentRef(janeSession, new IdRef(annotation.getId())));
            fail("jane should not be able to get the top level annotation ancestor");
        } catch (CommentSecurityException cse) {
            assertNotNull(cse);
            assertEquals(String.format("The user jane does not have access to the comments of document %s",
                    annotatedDocModel.getId()), cse.getMessage());
        }
    }
}
