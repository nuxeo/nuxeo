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

package org.nuxeo.ecm.platform.comment.impl;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.schema.FacetNames.FOLDERISH;
import static org.nuxeo.ecm.core.schema.FacetNames.HIDDEN_IN_NAVIGATION;
import static org.nuxeo.ecm.core.storage.BaseDocument.RELATED_TEXT;
import static org.nuxeo.ecm.core.storage.BaseDocument.RELATED_TEXT_ID;
import static org.nuxeo.ecm.core.storage.BaseDocument.RELATED_TEXT_RESOURCES;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newComment;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newExternalComment;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_ROOT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_TEXT_PROPERTY;
import static org.nuxeo.ecm.platform.comment.impl.AbstractCommentManager.COMMENTS_DIRECTORY;
import static org.nuxeo.ecm.platform.comment.impl.TreeCommentManager.COMMENT_NAME;
import static org.nuxeo.ecm.platform.comment.impl.TreeCommentManager.COMMENT_RELATED_TEXT_ID;
import static org.nuxeo.ecm.platform.comment.impl.TreeCommentManager.GET_COMMENT_PAGE_PROVIDER_NAME;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.document.CopyDocument;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.platform.comment.AbstractTestCommentManager;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * This test class shouldn't override abstract because abstract should reflect this implementation.
 * 
 * @since 11.1
 */
@Deploy("org.nuxeo.ecm.automation.core")
public class TestTreeCommentManager extends AbstractTestCommentManager {

    @Inject
    protected AutomationService automationService;

    public TestTreeCommentManager() {
        super(TreeCommentManager.class);
    }

    // ----------------
    // Structure tests
    // ----------------

    @Test
    public void _testCommentsStructure() {
        Comment c1 = commentManager.createComment(session, newComment(commentedDocModel.getId(), "I am a comment!"));
        Comment c2 = commentManager.createComment(session, newComment(c1.getId(), "I am a reply!"));
        Comment c3 = commentManager.createComment(session, newComment(c2.getId(), "Me too!"));

        // in TreeCommentManager: commentedDocModel > Comments (container) > c1 > c2 > c3
        DocumentModel commentContainerDocModel = session.getChild(commentedDocModel.getRef(), COMMENTS_DIRECTORY);
        DocumentRef[] c3ParentDocRefs = session.getParentDocumentRefs(c3.getDocument().getRef());
        assertEquals(c2.getDocument().getRef(), c3ParentDocRefs[0]);
        assertEquals(c1.getDocument().getRef(), c3ParentDocRefs[1]);
        assertEquals(commentContainerDocModel.getRef(), c3ParentDocRefs[2]);
        assertEquals(commentedDocModel.getRef(), c3ParentDocRefs[3]);

        // check paths
        assertEquals("/domain/test", commentedDocModel.getPathAsString());
        assertEquals("/domain/test/Comments", commentContainerDocModel.getPathAsString());
        assertEquals("/domain/test/Comments/comment", c1.getDocument().getPathAsString());
        assertEquals("/domain/test/Comments/comment/comment", c2.getDocument().getPathAsString());
        assertEquals("/domain/test/Comments/comment/comment/comment", c3.getDocument().getPathAsString());

        // check container
        assertEquals(COMMENT_ROOT_DOC_TYPE, commentContainerDocModel.getType());
        assertTrue(commentContainerDocModel.hasFacet(FOLDERISH));
        assertTrue(commentContainerDocModel.hasFacet(HIDDEN_IN_NAVIGATION));
        assertEquals(commentedDocModel.getRef(), commentContainerDocModel.getParentRef());
    }

    /*
     * NXP-28719
     */
    @Test
    public void _testCommentsStructureOnPlaceless() {
        DocumentModel placeless = session.createDocumentModel(null, "placeless", "File");
        placeless = session.createDocument(placeless);
        transactionalFeature.nextTransaction();

        Comment c1 = commentManager.createComment(session, newComment(placeless.getId(), "I am a comment!"));
        Comment c2 = commentManager.createComment(session, newComment(c1.getId(), "I am a reply!"));
        Comment c3 = commentManager.createComment(session, newComment(c2.getId(), "Me too!"));

        // in TreeCommentManager: placeless > Comments (container) > c1 > c2 > c3
        DocumentModel commentContainerDocModel = session.getChild(placeless.getRef(), COMMENTS_DIRECTORY);
        DocumentRef[] c3ParentDocRefs = session.getParentDocumentRefs(c3.getDocument().getRef());
        assertEquals(c2.getDocument().getRef(), c3ParentDocRefs[0]);
        assertEquals(c1.getDocument().getRef(), c3ParentDocRefs[1]);
        assertEquals(commentContainerDocModel.getRef(), c3ParentDocRefs[2]);
        assertEquals(placeless.getRef(), c3ParentDocRefs[3]);

        // check paths
        assertEquals("placeless", placeless.getPathAsString());
        assertEquals("placeless/Comments", commentContainerDocModel.getPathAsString());
        assertEquals("placeless/Comments/comment", c1.getDocument().getPathAsString());
        assertEquals("placeless/Comments/comment/comment", c2.getDocument().getPathAsString());
        assertEquals("placeless/Comments/comment/comment/comment", c3.getDocument().getPathAsString());
    }

    // --------------
    // Feature tests
    // --------------

    @Test
    public void _testCommentsExcludedFromCopy() throws OperationException {
        // create a regular child to check it is copied
        DocumentModel regularChildDoc = session.createDocumentModel(commentedDocModel.getPathAsString(), "regularChild",
                "File");
        session.createDocument(regularChildDoc);
        // document type Comments are considered as special children
        commentManager.createComment(session, newComment(commentedDocModel.getId()));

        assertEquals(2, session.getChildren(commentedDocModel.getRef()).size());

        try (OperationContext context = new OperationContext(session)) {
            context.setInput(commentedDocModel);
            Map<String, Serializable> params = new HashMap<>();
            params.put("target", "/");
            params.put("name", "CopyDoc");
            DocumentModel copyDocModel = (DocumentModel) automationService.run(context, CopyDocument.ID, params);
            copyDocModel = session.getDocument(copyDocModel.getRef());

            assertNotEquals(commentedDocModel.getId(), copyDocModel.getId());
            assertEquals("CopyDoc", copyDocModel.getName());
            // special children shall not be copied
            assertEquals(1, session.getChildren(copyDocModel.getRef()).size());
            DocumentModel copiedChild = session.getChild(copyDocModel.getRef(), "regularChild");
            assertNotEquals(regularChildDoc.getRef(), copiedChild.getRef());
        }
    }

    @Test
    public void _testCommentsWithCheckInAndRestore() {
        // create a regular child to check it is not copied during checkin
        DocumentModel regularChildDoc = session.createDocumentModel(commentedDocModel.getPathAsString(), "regularChild",
                "File");
        session.createDocument(regularChildDoc);
        // document type Comments are considered as special children and they should be copied during checkin
        commentManager.createComment(session, newComment(commentedDocModel.getId(), "I am a comment !"));

        assertEquals(2, session.getChildren(commentedDocModel.getRef()).size());

        DocumentModel commentsDirectory = session.getChild(commentedDocModel.getRef(), COMMENTS_DIRECTORY);

        // check only special children are copied
        DocumentRef checkedIn = commentedDocModel.checkIn(VersioningOption.MAJOR, "JustForFun");
        assertEquals(1, session.getChildren(checkedIn).size());
        DocumentModel versionedChild = session.getChild(checkedIn, COMMENTS_DIRECTORY);
        assertEquals(COMMENT_ROOT_DOC_TYPE, versionedChild.getType());
        assertNotEquals(commentsDirectory.getRef(), versionedChild.getRef());

        // Check the snapshot comment
        assertEquals(1, session.getChildren(versionedChild.getRef()).size());
        DocumentModel retrievedComment = session.getChild(versionedChild.getRef(), COMMENT_NAME);
        assertEquals("I am a comment !", retrievedComment.getPropertyValue(COMMENT_TEXT_PROPERTY));

        // test restore copy. Live document shall keep both special and regular children.
        // No version children shall be added during restore
        DocumentModel restored = session.restoreToVersion(commentedDocModel.getRef(), checkedIn);
        assertEquals(2, session.getChildren(restored.getRef()).size());
    }

    /*
     * NXP-28700
     */
    @Test
    public void _testGetExternalCommentPageProviderReturnsRightCommentAndNotVersionOnes() {
        commentManager.createComment(session,
                newExternalComment(commentedDocModel.getId(), "foo", "<entity/>", "I am a comment!"));

        commentedDocModel.checkIn(VersioningOption.MINOR, "checkin comment");
        // we now have two external entities with id foo in repository
        assertEquals(2, session.query("SELECT * FROM Comment where externalEntity:entityId='foo'").size());

        // test external entity retrieval with comment manager
        var externalComment = commentManager.getExternalComment(session, "foo");
        assertEquals(commentedDocModel.getId(), externalComment.getParentId());
        assertEquals("I am a comment!", externalComment.getText());

        // now test page provider used internally by comment manager
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = singletonMap(CORE_SESSION_PROPERTY, (Serializable) session);
        var pageProvider = ppService.getPageProvider(GET_COMMENT_PAGE_PROVIDER_NAME, Collections.emptyList(), 10L, 0L,
                props, "foo");
        assertEquals(1, pageProvider.getCurrentPageSize());
    }

    /*
     * NXP-28483 / NXP-28964
     */
    @Test
    public void _testRelatedTextDoesNotTriggerAutomaticVersioning() {
        CoreSession jamesSession = coreFeature.getCoreSession(JAMES);

        assertTrue(session.getVersions(commentedDocModel.getRef()).isEmpty());

        Comment comment = newComment(commentedDocModel.getId(), "I am a comment!");
        comment = commentManager.createComment(jamesSession, comment);

        assertTrue(session.getVersions(commentedDocModel.getRef()).isEmpty());

        comment.setText("Updated comment!");
        comment = commentManager.updateComment(jamesSession, comment.getId(), comment);

        assertTrue(session.getVersions(commentedDocModel.getRef()).isEmpty());
    }

    @Test
    public void _testCommentTextAreStoredInCommentedFileFullTextSearch() {
        assumeTrue("fulltext search not supported", coreFeature.getStorageConfiguration().supportsFulltextSearch());

        DocumentModel firstDocToComment = session.createDocumentModel("/", "file001", "File");
        firstDocToComment = session.createDocument(firstDocToComment);
        DocumentModel secondDocToComment = session.createDocumentModel("/", "file002", "File");
        secondDocToComment = session.createDocument(secondDocToComment);
        Map<DocumentRef, List<Comment>> mapCommentsByDocRef = createCommentsAndRepliesForFullTextSearch(
                firstDocToComment, secondDocToComment);
        transactionalFeature.nextTransaction();

        // One comment and 3 replies
        checkRelatedTextResource(firstDocToComment.getRef(), mapCommentsByDocRef.get(firstDocToComment.getRef()));

        // One comment and no replies
        checkRelatedTextResource(secondDocToComment.getRef(), mapCommentsByDocRef.get(secondDocToComment.getRef()));

        // We make a fulltext query to find the 2 commented files
        makeAndVerifyFullTextSearch("first comment", firstDocToComment, secondDocToComment);

        // We make a fulltext query to find the second commented file
        makeAndVerifyFullTextSearch("secondFile", secondDocToComment);

        // We make a fulltext query to find the first commented file by any reply
        makeAndVerifyFullTextSearch("reply", firstDocToComment);

        // There is no commented file with the provided text
        makeAndVerifyFullTextSearch("UpdatedReply");

        // Get the second reply and update his text
        Comment secondCreatedReply = mapCommentsByDocRef.get(firstDocToComment.getRef()).get(2);
        secondCreatedReply.setText("I am an UpdatedReply");
        commentManager.updateComment(session, secondCreatedReply.getId(), secondCreatedReply);
        transactionalFeature.nextTransaction();

        // Now we should find the document with this updated reply text
        makeAndVerifyFullTextSearch("UpdatedReply", firstDocToComment);

        // Now let's remove this second reply
        commentManager.deleteComment(session, secondCreatedReply.getId());
        transactionalFeature.nextTransaction();
        makeAndVerifyFullTextSearch("UpdatedReply");

        List<Comment> comments = mapCommentsByDocRef.get(firstDocToComment.getRef())
                                                    .stream()
                                                    .filter(c -> !c.getId().equals(secondCreatedReply.getId()))
                                                    .collect(Collectors.toList());
        checkRelatedTextResource(firstDocToComment.getRef(), comments);
    }

    protected Map<DocumentRef, List<Comment>> createCommentsAndRepliesForFullTextSearch(DocumentModel firstDocToComment,
            DocumentModel secondDocToComment) {

        // Create 2 comments on the two files
        Comment firstCommentOfFile1 = newComment(firstDocToComment.getId(), "I am the first comment of firstFile");
        firstCommentOfFile1 = commentManager.createComment(session, firstCommentOfFile1);

        Comment firstCommentOfFile2 = newComment(secondDocToComment.getId(), "I am the first comment of secondFile");
        firstCommentOfFile2 = commentManager.createComment(session, firstCommentOfFile2);

        // the comment container is created with the atomic CoreSession#getOrCreateDocument operation which commits the
        // transaction (and trigger async actions) - so wait for these actions to complete
        transactionalFeature.nextTransaction();

        // Create first reply on first comment of first file
        Comment firstReply = newComment(firstCommentOfFile1.getId(), "I am the first reply of first comment");
        Comment firstCreatedReply = commentManager.createComment(session, firstReply);

        // Create second reply
        Comment secondReply = newComment(firstCreatedReply.getId(), "I am the second reply of first comment");
        Comment secondCreatedReply = commentManager.createComment(session, secondReply);

        // Create third reply
        Comment thirdReply = newComment(secondCreatedReply.getId(), "I am the third reply of first comment");
        Comment thirdCreatedReply = commentManager.createComment(session, thirdReply);

        return Map.of( //
                firstDocToComment.getRef(),
                List.of(firstCommentOfFile1, firstCreatedReply, secondCreatedReply, thirdCreatedReply), //
                secondDocToComment.getRef(), List.of(firstCommentOfFile2) //
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
    protected void checkRelatedTextResource(DocumentRef documentRef, List<Comment> comments) {
        DocumentModel doc = session.getDocument(documentRef);

        List<Map<String, String>> resources = (List<Map<String, String>>) doc.getPropertyValue(RELATED_TEXT_RESOURCES);

        List<String> expectedRelatedTextIds = comments.stream()
                                                      .map(c -> String.format(COMMENT_RELATED_TEXT_ID, c.getId()))
                                                      .sorted()
                                                      .collect(Collectors.toList());

        List<String> expectedRelatedText = comments.stream()
                                                   .map(Comment::getText)
                                                   .sorted()
                                                   .collect(Collectors.toList());

        assertEquals(expectedRelatedTextIds,
                resources.stream().map(m -> m.get(RELATED_TEXT_ID)).sorted().collect(Collectors.toList()));

        assertEquals(expectedRelatedText,
                resources.stream().map(m -> m.get(RELATED_TEXT)).sorted().collect(Collectors.toList()));
    }

}
