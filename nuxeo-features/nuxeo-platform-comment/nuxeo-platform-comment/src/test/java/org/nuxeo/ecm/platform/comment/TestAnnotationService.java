/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *     Nuno Cunha <ncunha@nuxeo.com>
 */

package org.nuxeo.ecm.platform.comment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.List;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationImpl;
import org.nuxeo.ecm.platform.comment.api.AnnotationService;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.1
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.comment")
public class TestAnnotationService {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected AnnotationService annotationService;

    protected CloseableCoreSession session;

    @Before
    public void setup() {

        session = coreFeature.openCoreSession();
        DocumentModel domain = session.createDocumentModel("/", "testDomain", "Domain");
        session.createDocument(domain);
        session.close();

        // open a session as a regular user
        session = coreFeature.openCoreSession("jdoe");
        // give permission to him
        ACLImpl acl = new ACLImpl();
        acl.add(new ACE("jdoe", SecurityConstants.READ));
        acl.add(new ACE("jdoe", SecurityConstants.WRITE));
        ACPImpl acp = new ACPImpl();
        acp.addACL(acl);
        coreFeature.getCoreSession().setACP(new PathRef("/"), acp, true);
    }

    @After
    public void tearDown() {
        session.close();
    }

    @Test
    public void testCreateAnnotation() {

        DocumentModel docToAnnotate = session.createDocumentModel("/testDomain", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String entityId = "foo";
        String docIdToAnnotate = docToAnnotate.getId();
        String xpathToAnnotate = "files:files/0/file";
        String comment = "test comment";
        String origin = "Test";
        String entity = "<entity><annotation>bar</annotation></entity>";

        Annotation annotation = new AnnotationImpl();
        annotation.setAuthor("jdoe");
        annotation.setText(comment);
        annotation.setParentId(docIdToAnnotate);
        annotation.setXpath(xpathToAnnotate);
        annotation.setCreationDate(Instant.now());
        annotation.setModificationDate(Instant.now());
        ((ExternalEntity) annotation).setEntityId(entityId);
        ((ExternalEntity) annotation).setOrigin(origin);
        ((ExternalEntity) annotation).setEntity(entity);
        annotation = annotationService.createAnnotation(session, annotation);
        session.save();

        assertEquals("jdoe", annotation.getAuthor());
        assertEquals(comment, annotation.getText());
        assertEquals(docIdToAnnotate, annotation.getParentId());
        assertTrue(annotation.getAncestorIds().contains(docIdToAnnotate));
        assertNotNull(annotation.getCreationDate());
        assertNotNull(annotation.getModificationDate());
        assertEquals(xpathToAnnotate, annotation.getXpath());
        assertEquals(entityId, ((ExternalEntity) annotation).getEntityId());
        assertEquals(origin, ((ExternalEntity) annotation).getOrigin());

    }

    @Test
    public void testGetAnnotation() {

        DocumentModel docToAnnotate = session.createDocumentModel("/testDomain", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String entityId = "foo";
        String docIdToAnnotate = docToAnnotate.getId();
        String xpathToAnnotate = "files:files/0/file";

        String annotationId;
        try (CloseableCoreSession adminSession = CoreInstance.openCoreSessionSystem(coreFeature.getRepositoryName())) {
            Annotation annotation = new AnnotationImpl();
            annotation.setParentId(docIdToAnnotate);
            annotation.setXpath(xpathToAnnotate);
            ((ExternalEntity) annotation).setEntityId(entityId);
            annotationId = annotationService.createAnnotation(adminSession, annotation).getId();
        }

        Annotation annotation = annotationService.getAnnotation(session, annotationId);
        assertEquals(entityId, ((ExternalEntity) annotation).getEntityId());
    }

    @Test
    public void testUpdateAnnotation() {

        DocumentModel docToAnnotate = session.createDocumentModel("/testDomain", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String xpathToAnnotate = "files:files/0/file";

        Annotation annotation = new AnnotationImpl();
        annotation.setParentId(docToAnnotate.getId());
        annotation.setXpath(xpathToAnnotate);
        annotation = annotationService.createAnnotation(session, annotation);
        session.save();

        assertNull(((ExternalEntity) annotation).getEntity());

        ((ExternalEntity) annotation).setEntityId("entityId");
        ((ExternalEntity) annotation).setEntity("Entity");
        annotationService.updateAnnotation(session, annotation.getId(), annotation);

        assertEquals("Entity",
                ((ExternalEntity) annotationService.getAnnotation(session, annotation.getId())).getEntity());
    }

    @Test
    public void testDeleteAnnotation() {

        DocumentModel docToAnnotate = session.createDocumentModel("/testDomain", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String xpathToAnnotate = "files:files/0/file";

        Annotation annotation = new AnnotationImpl();
        annotation.setParentId(docToAnnotate.getId());
        annotation.setXpath(xpathToAnnotate);
        annotation = annotationService.createAnnotation(session, annotation);
        session.save();

        assertTrue(session.exists(new IdRef(annotation.getId())));

        try {
            annotationService.deleteAnnotation(session, "toto");
            fail("Deleting an unknown annotation should have failed");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }
        annotationService.deleteAnnotation(session, annotation.getId());
        assertFalse(session.exists(new IdRef(annotation.getId())));

    }

    @Test
    public void testGetAnnotationsForDocument() {

        DocumentModel docToAnnotate = session.createDocumentModel("/testDomain", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String xpathToAnnotate = "files:files/0/file";

        List<Annotation> annotations = annotationService.getAnnotations(session, docToAnnotate.getId(),
                xpathToAnnotate);
        assertTrue(annotations.isEmpty());

        DocumentModel docToAnnotate1 = session.createDocumentModel("/testDomain", "testDoc1", "File");
        docToAnnotate1 = session.createDocument(docToAnnotate1);
        int nbAnnotations1 = 99;
        Annotation annotation1 = new AnnotationImpl();
        annotation1.setParentId(docToAnnotate1.getId());
        annotation1.setXpath(xpathToAnnotate);
        for (int i = 0; i < nbAnnotations1; i++) {
            annotationService.createAnnotation(session, annotation1);
        }
        session.save();

        DocumentModel docToAnnotate2 = session.createDocumentModel("/testDomain", "testDoc2", "File");
        docToAnnotate2 = session.createDocument(docToAnnotate2);
        int nbAnnotations2 = 74;
        Annotation annotation2 = new AnnotationImpl();
        annotation2.setParentId(docToAnnotate2.getId());
        annotation2.setXpath(xpathToAnnotate);
        for (int i = 0; i < nbAnnotations2; i++) {
            annotationService.createAnnotation(session, annotation2);
        }
        session.save();
        assertEquals(nbAnnotations1,
                annotationService.getAnnotations(session, docToAnnotate1.getId(), xpathToAnnotate).size());
        assertEquals(nbAnnotations2,
                annotationService.getAnnotations(session, docToAnnotate2.getId(), xpathToAnnotate).size());

    }

    @Test
    public void testGetExternalAnnotation() {

        DocumentModel docToAnnotate = session.createDocumentModel("/testDomain", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String entityId = "foo";
        String docIdToAnnotate = docToAnnotate.getId();
        String xpathToAnnotate = "files:files/0/file";

        Annotation annotation = new AnnotationImpl();
        ((ExternalEntity) annotation).setEntityId(entityId);
        annotation.setParentId(docIdToAnnotate);
        annotation.setXpath(xpathToAnnotate);
        annotationService.createAnnotation(session, annotation);
        session.save();

        annotation = annotationService.getExternalAnnotation(session, entityId);
        assertEquals(entityId, ((ExternalEntity) annotation).getEntityId());
    }

    @Test
    public void testUpdateExternalAnnotation() {

        DocumentModel docToAnnotate = session.createDocumentModel("/testDomain", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String xpathToAnnotate = "files:files/0/file";
        String entityId = "foo";
        String entity = "<entity></entity>";

        Annotation annotation = new AnnotationImpl();
        ((ExternalEntity) annotation).setEntityId(entityId);
        annotation.setParentId(docToAnnotate.getId());
        annotation.setXpath(xpathToAnnotate);
        annotationService.createAnnotation(session, annotation);
        session.save();

        assertNull(((ExternalEntity) annotation).getEntity());

        ((ExternalEntity) annotation).setEntity(entity);
        try {
            annotationService.updateExternalAnnotation(session, "fakeId", annotation);
            fail("The external annotation should not exist");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        annotationService.updateExternalAnnotation(session, entityId, annotation);
        annotation = annotationService.getExternalAnnotation(session, entityId);
        assertEquals(entityId, ((ExternalEntity) annotation).getEntityId());

    }

    @Test
    public void testDeleteExternalAnnotation() {

        DocumentModel docToAnnotate = session.createDocumentModel("/testDomain", "testDoc", "File");
        docToAnnotate = session.createDocument(docToAnnotate);

        String xpathToAnnotate = "files:files/0/file";
        String entityId = "foo";

        Annotation annotation = new AnnotationImpl();
        ((ExternalEntity) annotation).setEntityId(entityId);
        annotation.setParentId(docToAnnotate.getId());
        annotation.setXpath(xpathToAnnotate);
        annotation = annotationService.createAnnotation(session, annotation);
        session.save();

        assertTrue(session.exists(new IdRef(annotation.getId())));

        try {
            annotationService.deleteExternalAnnotation(session, "toto");
            fail("Deleting an unknown annotation should have failed");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }
        annotationService.deleteExternalAnnotation(session, entityId);
        assertFalse(session.exists(new IdRef(annotation.getId())));

    }

}
