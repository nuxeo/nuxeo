/*
 * (C) Copyright 2019-2020 Nuxeo (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.storage.BaseDocument.RELATED_TEXT;
import static org.nuxeo.ecm.core.storage.BaseDocument.RELATED_TEXT_ID;
import static org.nuxeo.ecm.core.storage.BaseDocument.RELATED_TEXT_RESOURCES;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newAnnotation;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newExternalAnnotation;
import static org.nuxeo.ecm.platform.comment.impl.TreeCommentManager.COMMENT_RELATED_TEXT_ID;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.local.WithUser;
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
@WithUser(TestAnnotationService.JDOE)
public class TestAnnotationService {

    protected static final String JDOE = "jdoe";

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected CoreFeature coreFeature;

    /** The jdoe session. */
    @Inject
    protected CoreSession session;

    @Inject
    protected AnnotationService annotationService;

    protected DocumentModel annotatedDocModel;

    @Before
    public void setup() {
        // create a domain with permissions for jdoe
        CoreSession systemSession = coreFeature.getCoreSessionSystem();
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

        try {
            CoreSession bobSession = coreFeature.getCoreSession("bob");
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

        // create annotation with admin session for permission check
        CoreSession adminSession = coreFeature.getCoreSessionSystem();
        Annotation annotation = newAnnotation(annotatedDocModel.getId(), xpathToAnnotate);
        String annotationId = annotationService.createAnnotation(adminSession, annotation).getId();

        annotation = annotationService.getAnnotation(session, annotationId);

        try {
            CoreSession bobSession = coreFeature.getCoreSession("bob");
            annotation = annotationService.getAnnotation(bobSession, annotationId);
            fail("bob should not be able to get annotation");
        } catch (CommentSecurityException e) {
            assertEquals("The user bob does not have access to the comment " + annotationId, e.getMessage());
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

        try {
            CoreSession bobSession = coreFeature.getCoreSession("bob");
            annotationService.updateAnnotation(bobSession, annotation.getId(), annotation);
            fail("bob should not be able to edit annotation");
        } catch (CommentSecurityException e) {
            assertEquals("The user bob does not have access to the comment " + annotation.getId(), e.getMessage());
        }
    }

    @Test
    public void testDeleteAnnotation() {
        String xpathToAnnotate = "files:files/0/file";

        Annotation annotation = newAnnotation(annotatedDocModel.getId(), xpathToAnnotate);
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

        try {
            CoreSession bobSession = coreFeature.getCoreSession("bob");
            annotationService.deleteAnnotation(bobSession, annotation.getId());
            fail("bob should not be able to delete annotation");
        } catch (CommentSecurityException e) {
            assertEquals("The user bob cannot delete comments of the document " + annotatedDocModel.getId(),
                    e.getMessage());
        }

        annotationService.deleteAnnotation(session, annotation.getId());
        assertFalse(session.exists(new IdRef(annotation.getId())));

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

        try {
            CoreSession bobSession = coreFeature.getCoreSession("bob");
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

        try {
            CoreSession bobSession = coreFeature.getCoreSession("bob");
            annotationService.getExternalAnnotation(bobSession, annotatedDocModel.getId(), entityId);
            fail("bob should not be able to get annotation");
        } catch (CommentNotFoundException e) {
            assertEquals(String.format("The external comment %s does not exist.", entityId), e.getMessage());
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

        try {
            CoreSession bobSession = coreFeature.getCoreSession("bob");
            annotationService.updateExternalAnnotation(bobSession, annotatedDocModel.getId(), entityId, annotation);
            fail("bob should not be able to edit annotation");
        } catch (CommentNotFoundException e) {
            assertEquals("The external comment foo does not exist.", e.getMessage());
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

        assertTrue(session.exists(new IdRef(annotation.getId())));

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

        try {
            CoreSession bobSession = coreFeature.getCoreSession("bob");
            annotationService.deleteExternalAnnotation(bobSession, annotatedDocModel.getId(), entityId);
            fail("bob should not be able to delete annotation");
        } catch (CommentNotFoundException e) {
            assertEquals("The external comment foo does not exist.", e.getMessage());
        }

        annotationService.deleteExternalAnnotation(session, annotatedDocModel.getId(), entityId);
        assertFalse(session.exists(new IdRef(annotation.getId())));
    }

    @Test
    public void testGetTopLevelAnnotationAncestor() {
        String xpathToAnnotate = "files:files/0/file";

        Annotation annotation = newAnnotation(annotatedDocModel.getId(), xpathToAnnotate);
        annotation = annotationService.createAnnotation(session, annotation);

        // allow james to see annotation
        DocumentRef docRefToAnnotate = new IdRef(annotatedDocModel.getId());
        CoreSession systemSession = coreFeature.getCoreSessionSystem();
        ACP acp = session.getACP(docRefToAnnotate);
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("james", SecurityConstants.READ, true));
        systemSession.setACP(docRefToAnnotate, acp, false);
        systemSession.save();

        CommentManager commentManager = Framework.getService(CommentManager.class);
        assertEquals(docRefToAnnotate,
                commentManager.getTopLevelDocumentRef(systemSession, new IdRef(annotation.getId())));

        CoreSession jamesSession = coreFeature.getCoreSession("james");
        assertEquals(docRefToAnnotate,
                commentManager.getTopLevelDocumentRef(jamesSession, new IdRef(annotation.getId())));

        try {
            CoreSession janeSession = coreFeature.getCoreSession("jane");
            assertEquals(docRefToAnnotate,
                    commentManager.getTopLevelDocumentRef(janeSession, new IdRef(annotation.getId())));
            fail("jane should not be able to get the top level annotation ancestor");
        } catch (CommentSecurityException cse) {
            assertNotNull(cse);
            assertEquals(String.format("The user jane does not have access to the comments of document %s",
                    annotatedDocModel.getId()), cse.getMessage());
        }
    }

    @Test
    public void shouldFindAnnotatedFileByFullTextSearch() {
        assumeTrue("fulltext search not supported", coreFeature.getStorageConfiguration().supportsFulltextSearch());

        DocumentModel firstDocToAnnotation = createDocumentModel("anotherFile1");
        DocumentModel secondDocToAnnotation = createDocumentModel("anotherFile2");
        Map<DocumentRef, List<Annotation>> mapAnnotationsByDocRef = createAnnotationsAndRepliesForFullTextSearch(
                firstDocToAnnotation, secondDocToAnnotation);

        // One annotation and 3 replies
        checkRelatedTextResource(firstDocToAnnotation.getRef(),
                mapAnnotationsByDocRef.get(firstDocToAnnotation.getRef()));

        // One annotation and no replies
        checkRelatedTextResource(secondDocToAnnotation.getRef(),
                mapAnnotationsByDocRef.get(secondDocToAnnotation.getRef()));

        // We make a fulltext query to find the 2 annotated files
        makeAndVerifyFullTextSearch("first annotation", firstDocToAnnotation, secondDocToAnnotation);

        // We make a fulltext query to find the second annotated file
        makeAndVerifyFullTextSearch("secondFile", secondDocToAnnotation);

        // We make a fulltext query to find the first annotated file by any reply
        makeAndVerifyFullTextSearch("reply", firstDocToAnnotation);

        // There is no annotated file with the provided text
        makeAndVerifyFullTextSearch("UpdatedReply");

        // Get the second reply and update his text
        Annotation secondReply = mapAnnotationsByDocRef.get(firstDocToAnnotation.getRef()).get(2);
        secondReply.setText("I am an UpdatedReply");
        annotationService.updateAnnotation(session, secondReply.getId(), secondReply);
        transactionalFeature.nextTransaction();

        // Now we should find the document with this updated reply text
        makeAndVerifyFullTextSearch("UpdatedReply", firstDocToAnnotation);

        // Now let's remove this second reply
        annotationService.deleteAnnotation(session, secondReply.getId());
        transactionalFeature.nextTransaction();
        makeAndVerifyFullTextSearch("UpdatedReply");

        List<Annotation> annotations = mapAnnotationsByDocRef.get(firstDocToAnnotation.getRef())
                                                             .stream()
                                                             .filter(c -> !c.getId().equals(secondReply.getId()))
                                                             .collect(Collectors.toList());
        checkRelatedTextResource(firstDocToAnnotation.getRef(), annotations);
    }

    protected Map<DocumentRef, List<Annotation>> createAnnotationsAndRepliesForFullTextSearch(
            DocumentModel firstDocToAnnotate, DocumentModel secondDocToAnnotate) {
        String xpathToAnnotate = "files:files/0/file";

        // Create 2 annotations on the two files
        Annotation annotationOfFile1 = createAnnotation(
                newAnnotation(firstDocToAnnotate.getId(), xpathToAnnotate, "I am the first annotation of firstFile"));

        Annotation annotationOfFile2 = createAnnotation(
                newAnnotation(secondDocToAnnotate.getId(), xpathToAnnotate, "I am the first annotation of secondFile"));

        // Create first reply on first annotation of first file
        Annotation firstReply = createAnnotation(
                newAnnotation(annotationOfFile1.getId(), xpathToAnnotate, "I am the first reply of first annotation"));

        // Create second reply
        Annotation secondReply = createAnnotation(
                newAnnotation(firstReply.getId(), xpathToAnnotate, "I am the second reply of first annotation"));

        // Create third reply
        Annotation thirdReply = createAnnotation(
                newAnnotation(secondReply.getId(), xpathToAnnotate, "I am the third reply of first annotation"));

        return Map.of( //
                new IdRef(firstDocToAnnotate.getId()), List.of(annotationOfFile1, firstReply, secondReply, thirdReply), //
                new IdRef(secondDocToAnnotate.getId()), List.of(annotationOfFile2) //
        );
    }

    protected void makeAndVerifyFullTextSearch(String ecmFullText, DocumentModel... expectedDocs) {
        String query = String.format(
                "SELECT * FROM Document WHERE ecm:fulltext = '%s' AND ecm:mixinType != 'HiddenInNavigation'",
                ecmFullText);

        DocumentModelList documents = session.query(query);

        Arrays.sort(expectedDocs, Comparator.comparing(DocumentModel::getId));
        documents.sort(Comparator.comparing(DocumentModel::getId));
        assertArrayEquals(expectedDocs, documents.toArray(new DocumentModel[0]));
    }

    @SuppressWarnings("unchecked")
    protected void checkRelatedTextResource(DocumentRef documentRef, List<Annotation> annotations) {
        DocumentModel doc = session.getDocument(documentRef);

        List<Map<String, String>> resources = (List<Map<String, String>>) doc.getPropertyValue(RELATED_TEXT_RESOURCES);

        List<String> relatedTextIds = annotations.stream()
                                                 .map(a -> String.format(COMMENT_RELATED_TEXT_ID, a.getId()))
                                                 .sorted()
                                                 .collect(Collectors.toList());

        List<String> relatedTextValues = annotations.stream()
                                                    .map(Annotation::getText)
                                                    .sorted()
                                                    .collect(Collectors.toList());

        assertEquals(relatedTextIds,
                resources.stream().map(m -> m.get(RELATED_TEXT_ID)).sorted().collect(Collectors.toList()));
        assertEquals(relatedTextValues,
                resources.stream().map(m -> m.get(RELATED_TEXT)).sorted().collect(Collectors.toList()));
    }

    protected Annotation createAnnotation(Annotation annotation) {
        Annotation createdAnnotation = annotationService.createAnnotation(session, annotation);
        transactionalFeature.nextTransaction();
        return createdAnnotation;
    }

    protected DocumentModel createDocumentModel(String fileName) {
        CoreSession systemSession = coreFeature.getCoreSessionSystem();
        DocumentModel docToAnnotate = systemSession.createDocumentModel("/domain", fileName, "File");
        docToAnnotate = systemSession.createDocument(docToAnnotate);
        // give permission to comment to jdoe
        ACP acp = docToAnnotate.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("jdoe", SecurityConstants.READ, true));
        systemSession.setACP(docToAnnotate.getRef(), acp, false);
        systemSession.save();
        transactionalFeature.nextTransaction();
        return docToAnnotate;
    }
}