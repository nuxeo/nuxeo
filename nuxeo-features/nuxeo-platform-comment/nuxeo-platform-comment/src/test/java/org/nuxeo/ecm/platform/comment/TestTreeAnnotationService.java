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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.storage.BaseDocument.RELATED_TEXT;
import static org.nuxeo.ecm.core.storage.BaseDocument.RELATED_TEXT_ID;
import static org.nuxeo.ecm.core.storage.BaseDocument.RELATED_TEXT_RESOURCES;
import static org.nuxeo.ecm.platform.comment.impl.TreeCommentManager.COMMENT_RELATED_TEXT_ID;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;
import org.nuxeo.ecm.platform.comment.impl.TreeCommentManager;

/**
 * @since 11.1
 */
public class TestTreeAnnotationService extends AbstractTestAnnotationService {

    @Override
    protected Class<? extends CommentManager> getCommentManager() {
        return TreeCommentManager.class;
    }

    @Test
    public void shouldFindAnnotatedFileByFullTextSearch() {
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

        // Create 2 annotations on the two files
        Annotation annotationOfFile1 = createAnnotation(
                createSampleAnnotation(firstDocToAnnotate.getId(), "I am the first annotation of firstFile"));

        Annotation annotationOfFile2 = createAnnotation(
                createSampleAnnotation(secondDocToAnnotate.getId(), "I am the first annotation of secondFile"));

        // Create first reply on first annotation of first file
        Annotation firstReply = createAnnotation(
                createSampleAnnotation(annotationOfFile1.getId(), "I am the first reply of first annotation"));

        // Create second reply
        Annotation secondReply = createAnnotation(
                createSampleAnnotation(firstReply.getId(), "I am the second reply of first annotation"));

        // Create third reply
        Annotation thirdReply = createAnnotation(
                createSampleAnnotation(secondReply.getId(), "I am the third reply of first annotation"));

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

    protected Annotation createSampleAnnotation(String annotateDocId, String text) {
        String entityId = "foo";
        String xpathToAnnotate = "files:files/0/file";
        String comment = text;
        String origin = "Test";
        String entity = "<entity><annotation>bar</annotation></entity>";

        Annotation annotation = new AnnotationImpl();
        annotation.setAuthor("jdoe");
        annotation.setText(comment);
        annotation.setParentId(annotateDocId);
        annotation.setXpath(xpathToAnnotate);
        annotation.setCreationDate(Instant.now());
        annotation.setModificationDate(Instant.now());
        ((ExternalEntity) annotation).setEntityId(entityId);
        ((ExternalEntity) annotation).setOrigin(origin);
        ((ExternalEntity) annotation).setEntity(entity);

        return annotation;
    }

    protected DocumentModel createDocumentModel(String fileName) {
        DocumentModel docToAnnotate = session.createDocumentModel("/testDomain", fileName, "File");
        docToAnnotate = session.createDocument(docToAnnotate);
        transactionalFeature.nextTransaction();
        return docToAnnotate;
    }
}