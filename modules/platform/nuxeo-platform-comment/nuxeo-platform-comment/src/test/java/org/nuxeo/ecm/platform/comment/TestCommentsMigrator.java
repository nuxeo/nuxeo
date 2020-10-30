/*
 * (C) Copyright 2018-2020 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.comment;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.core.api.security.ACL.LOCAL_ACL;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_ANCESTORID;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_NAME;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_PRIMARYTYPE;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newComment;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_AUTHOR_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_PARENT_ID_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_ROOT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_TEXT_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_ID;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STATE_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STATE_RELATION;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STATE_SECURED;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STEP_PROPERTY_TO_SECURED;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STEP_RELATION_TO_PROPERTY;
import static org.nuxeo.ecm.platform.comment.impl.CommentsMigrator.UNMIGRATED_COMMENTS_FOLDER_NAME;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.DISABLE_NOTIFICATION_SERVICE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.logging.log4j.core.LogEvent;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.CommentManagerImpl;
import org.nuxeo.ecm.platform.comment.impl.CommentsMigrator;
import org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager;
import org.nuxeo.ecm.platform.comment.impl.TreeCommentManager;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.ecm.platform.comment.service.CommentServiceHelper;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationService;
import org.nuxeo.runtime.migration.MigrationService.MigrationContext;
import org.nuxeo.runtime.migration.MigrationService.Migrator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CommentFeature.class, LogFeature.class, LogCaptureFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.notification")
@Deploy("org.nuxeo.ecm.relations.api")
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.platform.comment.tests:OSGI-INF/comment-jena-contrib.xml")
public class TestCommentsMigrator {

    protected static final int NB_COMMENTS_BY_FILE = 50;

    protected static final int NB_COMMENT_TO_REPLY_ON_IT = 10;

    protected static final int NB_REPLY_BY_COMMENT = 5;

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected MigrationService migrationService;

    @Inject
    protected NotificationManager notificationManager;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Inject
    protected CoreFeature coreFeature;

    protected DocumentModel firstFileToComment;

    protected DocumentModel secondFileToComment;

    // This file will be the one with comments that doesn't contains the 'comment:parentId'
    protected DocumentModel fileWithCommentWithoutParent;

    // This file will be commented and then removed to simulate comments with removed parent
    protected DocumentModel fileToCommentAndRemove;

    // A placeless commented document
    protected DocumentModel placelessFileToComment;

    protected DocumentModel proxyFileToComment;

    @Before
    public void setUp() {
        DocumentModel domain = session.createDocumentModel("/", "test-domain", "Domain");
        session.createDocument(domain);

        DocumentModel anotherDomain = session.createDocumentModel("/", "another-domain", "Domain");
        session.createDocument(anotherDomain);

        // Create files which will be commented
        firstFileToComment = session.createDocumentModel(domain.getPathAsString(), "file1", "File");
        firstFileToComment = session.createDocument(firstFileToComment);

        secondFileToComment = session.createDocumentModel(domain.getPathAsString(), "file2", "File");
        secondFileToComment = session.createDocument(secondFileToComment);

        fileWithCommentWithoutParent = session.createDocumentModel(domain.getPathAsString(), "file3", "File");
        fileWithCommentWithoutParent = session.createDocument(fileWithCommentWithoutParent);

        fileToCommentAndRemove = session.createDocumentModel(domain.getPathAsString(), "file4", "File");
        fileToCommentAndRemove = session.createDocument(fileToCommentAndRemove);

        placelessFileToComment = session.createDocumentModel(null, "placelessFile", "File");
        placelessFileToComment = session.createDocument(placelessFileToComment);

        // Create a proxy file
        proxyFileToComment = session.createProxy(secondFileToComment.getRef(), anotherDomain.getRef());
    }

    @Test
    public void testMigrationFromRelationToProperty() {
        // Create comments as relations on these files
        createCommentsAsRelations();

        // First step of migrate: from 'Relation' to 'Property'
        migrateFromRelationToProperty(new CommentsMigrator());
    }

    /**
     * Test the migration in the case where all comments are correctly filled (comment:parentId is filled and exists)
     */
    @Test
    public void testMigrationFromPropertyToSecuredWithCommentsCorrectlyFilled() {
        // Create comments as property on these files and add some reply
        // Total of comments:
        // NB_COMMENTS_BY_FILE*3 (files) + NB_COMMENT_TO_REPLY_ON_IT*NB_REPLY_BY_COMMENT (5 levels of reply) = 150 + 50
        createCommentsAsProperty(false, false, false);

        // Second step of migrate: from 'Property' to 'Secured'
        migrateFromPropertyToSecured(new CommentsMigrator(), false, false, false);
    }

    /**
     * Test the migration in the case where the {@code comment:parentId} is empty.
     */
    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN")
    public void testMigrationFromPropertyToSecuredWithCommentParentEmpty() {
        // NB_COMMENTS_BY_FILE*4 + NB_COMMENT_TO_REPLY_ON_IT*NB_REPLY_BY_COMMENT (5 levels of reply) = 200 + 50
        createCommentsAsProperty(true, false, false);

        // Second step of migrate: from 'Property' to 'Secured'
        migrateFromPropertyToSecured(new CommentsMigrator(), true, false, false);

        transactionalFeature.nextTransaction();

        List<LogEvent> events = logCaptureResult.getCaughtEvents();
        int eventSize = events.size();
        assertTrue(String.valueOf(eventSize), eventSize >= NB_COMMENTS_BY_FILE + 1);

        String query = String.format("SELECT %s FROM Comment WHERE %s IS NULL ", ECM_UUID, COMMENT_PARENT_ID_PROPERTY);
        List<String> unMigratedComments = session.queryProjection(query, 0, 0)
                                                 .stream()
                                                 .map(m -> (String) m.get(ECM_UUID))
                                                 .collect(Collectors.toList());

        // The comments not migrated should be there but, not under 'fileWithCommentWithoutParent'
        assertEquals(NB_COMMENTS_BY_FILE, unMigratedComments.size());
        List<String> caughtEventMessages = logCaptureResult.getCaughtEventMessages();

        // Some comments have not been migrated (how: parentId is null), we make sure that we have the warning
        // messages
        assertTrue(caughtEventMessages.contains(String.format(
                "Some comments have not been migrated, see logs for more information. The folder containing these comments will be renamed to %s",
                UNMIGRATED_COMMENTS_FOLDER_NAME)));

        for (String docId : unMigratedComments) {
            String message = String.format(
                    "The comment document model with IdRef: %s cannot be migrated, because its 'comment:parentId' is not defined",
                    docId);
            assertTrue(caughtEventMessages.contains(message));
            assertTrue(session.exists(new IdRef(docId)));
        }

    }

    /**
     * Test the migration in the case where the {@code comment:parentId} is not empty, but it doesn't exist.
     */
    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN")
    @Deploy("org.nuxeo.ecm.platform.comment.tests:OSGI-INF/disable-removing-comment-children-listener.xml")
    public void testMigrationFromPropertyToSecuredWithRemovedParentComment() {
        // NB_COMMENTS_BY_FILE*4 + NB_COMMENT_TO_REPLY_ON_IT*NB_REPLY_BY_COMMENT (5 levels of reply) = 200 + 50
        createCommentsAsProperty(false, true, false);

        // Second step of migrate: from 'Property' to 'Secured'
        migrateFromPropertyToSecured(new CommentsMigrator(), false, true, false);
        transactionalFeature.nextTransaction();

        List<LogEvent> events = logCaptureResult.getCaughtEvents();
        int eventSize = events.size();
        assertTrue(String.valueOf(eventSize), eventSize >= NB_COMMENTS_BY_FILE + 1);

        String query = String.format("SELECT %s FROM Comment WHERE %s = '%s'", ECM_UUID, COMMENT_PARENT_ID_PROPERTY,
                fileToCommentAndRemove.getId());
        List<String> unMigratedCommentDocIds = session.queryProjection(query, 0, 0)
                                                      .stream()
                                                      .map(m -> (String) m.get(ECM_UUID))
                                                      .collect(Collectors.toList());

        // The comments not migrated should be there but, not under 'fileToCommentAndRemove'
        assertEquals(NB_COMMENTS_BY_FILE, unMigratedCommentDocIds.size());
        List<String> caughtEventMessages = logCaptureResult.getCaughtEventMessages();

        assertTrue(caughtEventMessages.contains(String.format(
                "Some comments have not been migrated, see logs for more information. The folder containing these comments will be renamed to %s",
                UNMIGRATED_COMMENTS_FOLDER_NAME)));

        for (String docId : unMigratedCommentDocIds) {
            String message = String.format(
                    "The comment document model with IdRef: %s cannot be migrated, because its parent: %s cannot be found",
                    docId, fileToCommentAndRemove.getId());
            assertTrue(caughtEventMessages.contains(message));
        }

    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.comment.tests:OSGI-INF/relation-comment-manager-override.xml")
    @SuppressWarnings("deprecation")
    public void testMigrationThroughService() {
        CommentManager commentManager;

        commentManager = Framework.getService(CommentManager.class);
        assertTrue(commentManager.getClass().getName(), commentManager instanceof CommentManagerImpl);

        MigrationService.MigrationStatus status = migrationService.getStatus(MIGRATION_ID);
        assertNotNull(status);
        assertFalse(status.isRunning());
        assertEquals(MIGRATION_STATE_RELATION, status.getState());

        // Launch the step relation to property and wait until it's finished
        runMigrationStep(MIGRATION_STEP_RELATION_TO_PROPERTY);

        commentManager = Framework.getService(CommentManager.class);
        assertTrue(commentManager.getClass().getName(), commentManager instanceof PropertyCommentManager);

        status = migrationService.getStatus(MIGRATION_ID);
        assertNotNull(status);
        assertFalse(status.isRunning());
        assertEquals(MIGRATION_STATE_PROPERTY, status.getState());

        // Launch the step property to secured and wait until it's finished
        runMigrationStep(MIGRATION_STEP_PROPERTY_TO_SECURED);
        commentManager = Framework.getService(CommentManager.class);
        assertTrue(commentManager.getClass().getName(), commentManager instanceof TreeCommentManager);

        status = migrationService.getStatus(MIGRATION_ID);
        assertNotNull(status);
        assertFalse(status.isRunning());
        assertEquals(MIGRATION_STATE_SECURED, status.getState());
        commentManager = Framework.getService(CommentManager.class);
        assertTrue(commentManager.getClass().getName(), commentManager instanceof TreeCommentManager);
    }

    protected void runMigration(Runnable migrator) {
        try (CapturingEventListener listener = new CapturingEventListener(DOCUMENT_UPDATED)) {
            migrator.run();
            List<Event> events = listener.getCapturedEvents();

            // Ensure the migration is done silently
            for (Event event : events) {
                assertEquals(TRUE, event.getContext().getProperty(DISABLE_NOTIFICATION_SERVICE));
            }
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testProbe() {
        CommentServiceConfig config = CommentServiceHelper.getCommentService().getConfig();
        CommentManager relationCommentManager = new CommentManagerImpl(config);
        CommentManager propertyCommentManager = new PropertyCommentManager();

        DocumentModel domain = session.createDocumentModel("/", "test-domain", "Domain");
        session.createDocument(domain);
        DocumentModel file = session.createDocumentModel("/test-domain", "anotherFile", "File");
        file = session.createDocument(file);
        session.save();

        Migrator migrator = new CommentsMigrator();
        assertEquals(MIGRATION_STATE_SECURED, migrator.probeState());

        // Both a relation-based comment and a property-based comment, detected as not migrated
        DocumentModel comment = session.createDocumentModel(null, "comment", COMMENT_DOC_TYPE);
        comment.setPropertyValue(COMMENT_AUTHOR_PROPERTY, session.getPrincipal().getName());
        relationCommentManager.createComment(file, comment);
        session.save();

        DocumentModel otherComment = session.createDocumentModel(null, "comment", COMMENT_DOC_TYPE);
        otherComment.setPropertyValue(COMMENT_PARENT_ID_PROPERTY, file.getId());
        propertyCommentManager.createComment(file, otherComment);
        session.save();

        // Migrate the created relation based comment to property
        runMigration(() -> migrator.run(MIGRATION_STEP_RELATION_TO_PROPERTY, new ProgressMigrationContext()));
        assertEquals(MIGRATION_STATE_PROPERTY, migrator.probeState());

        // Migrate the created property based comment to secured
        runMigration(() -> migrator.run(MIGRATION_STEP_PROPERTY_TO_SECURED, new ProgressMigrationContext()));
        assertEquals(MIGRATION_STATE_SECURED, migrator.probeState());

        // Just a relation-based comment, detected as not migrated
        comment = session.createDocumentModel(null, "comment", COMMENT_DOC_TYPE);
        comment.setPropertyValue(COMMENT_AUTHOR_PROPERTY, session.getPrincipal().getName());
        comment = relationCommentManager.createComment(file, comment);
        session.save();
        assertEquals(MIGRATION_STATE_RELATION, migrator.probeState());

        // Just a property-based comment, detected as migrated
        relationCommentManager.deleteComment(session, comment.getId());
        session.save();

        // Simulate comment deletion event
        RelationManager relationManager = Framework.getService(RelationManager.class);
        Resource commentRes = relationManager.getResource(config.commentNamespace, comment, null);
        Graph graph = relationManager.getGraph(config.graphName, session);
        List<Statement> statementList = graph.getStatements(commentRes, null, null);
        graph.remove(statementList);

        // No more relation comments detected as 'Relation'
        assertEquals(MIGRATION_STATE_PROPERTY, migrator.probeState());

        // Migrate the created property based comment to secured
        runMigration(() -> migrator.run(MIGRATION_STEP_PROPERTY_TO_SECURED, new ProgressMigrationContext()));

        // No more unsecured property comments
        assertEquals(MIGRATION_STATE_SECURED, migrator.probeState());
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "WARN")
    @Deploy("org.nuxeo.ecm.platform.comment.tests:OSGI-INF/disable-removing-comment-children-listener.xml")
    public void testProbeWithInConsistentComments() {
        CommentManager propertyCommentManager = new PropertyCommentManager();
        // add a comment with empty parent
        DocumentModel commentWithEmptyParent = session.createDocumentModel(null, "comment", COMMENT_DOC_TYPE);
        commentWithEmptyParent = propertyCommentManager.createComment(fileWithCommentWithoutParent,
                commentWithEmptyParent);
        commentWithEmptyParent.setPropertyValue(COMMENT_PARENT_ID_PROPERTY, null);
        session.saveDocument(commentWithEmptyParent);

        // add a comment with removed parent
        DocumentModel commentWithRemovedParent = session.createDocumentModel(fileToCommentAndRemove.getPathAsString(),
                "comment", COMMENT_DOC_TYPE);
        commentWithRemovedParent.setPropertyValue(COMMENT_PARENT_ID_PROPERTY, fileToCommentAndRemove.getId());
        commentWithRemovedParent = propertyCommentManager.createComment(fileToCommentAndRemove,
                commentWithRemovedParent);

        // wait for asynchronous completion before removal to avoid ConcurrentUpdateException
        transactionalFeature.nextTransaction();
        session.removeDocument(fileToCommentAndRemove.getRef());
        transactionalFeature.nextTransaction();

        Migrator migrator = new CommentsMigrator();

        // At this point we are in property step
        assertEquals(MIGRATION_STATE_PROPERTY, migrator.probeState());

        // Migrate the created property based comment to secured
        runMigration(() -> migrator.run(MIGRATION_STEP_PROPERTY_TO_SECURED, new ProgressMigrationContext()));

        // Check even with the prob, we should have the expected behaviour: logs + `secured` step
        List<String> caughtEventMessages = logCaptureResult.getCaughtEventMessages();
        assertTrue(caughtEventMessages.contains(String.format(
                "Some comments have not been migrated, see logs for more information. The folder containing these comments will be renamed to %s",
                UNMIGRATED_COMMENTS_FOLDER_NAME)));
        assertTrue(caughtEventMessages.contains(String.format(
                "The comment document model with IdRef: %s cannot be migrated, because its 'comment:parentId' is not defined",
                commentWithEmptyParent.getId())));
        assertTrue(caughtEventMessages.contains(String.format(
                "The comment document model with IdRef: %s cannot be migrated, because its parent: %s cannot be found",
                commentWithRemovedParent.getId(), fileToCommentAndRemove.getId())));

        // No more unsecured property comments
        assertEquals(MIGRATION_STATE_SECURED, migrator.probeState());

    }

    @Test
    public void testMigrationFromPropertyToSecuredWithPlacelessParent() {
        // NB_COMMENTS_BY_FILE*4 (files) + NB_COMMENT_TO_REPLY_ON_IT*NB_REPLY_BY_COMMENT (5 levels of reply) = 200 + 50
        createCommentsAsProperty(false, false, true);

        // Second step of migrate: from 'Property' to 'Secured'
        migrateFromPropertyToSecured(new CommentsMigrator(), false, false, true);
    }

    @Test
    public void testProbeWithCommentParentPlaceless() {
        CommentManager propertyCommentManager = new PropertyCommentManager();

        // add a comment where the parent is placeless
        DocumentModel commentWithPlacelessParent = session.createDocumentModel(placelessFileToComment.getPathAsString(),
                "comment", COMMENT_DOC_TYPE);
        commentWithPlacelessParent.setPropertyValue(COMMENT_PARENT_ID_PROPERTY, placelessFileToComment.getId());
        propertyCommentManager.createComment(placelessFileToComment, commentWithPlacelessParent);

        transactionalFeature.nextTransaction();

        Migrator migrator = new CommentsMigrator();

        // At this point we are in property step
        assertEquals(MIGRATION_STATE_PROPERTY, migrator.probeState());

        // Migrate the created property based comment to secured
        runMigration(() -> migrator.run(MIGRATION_STEP_PROPERTY_TO_SECURED, new ProgressMigrationContext()));

        // No more unsecured property comments
        assertEquals(MIGRATION_STATE_SECURED, migrator.probeState());
    }

    protected void migrateFromRelationToProperty(Migrator migrator) {
        ProgressMigrationContext migrationContext = new ProgressMigrationContext();
        runMigration(() -> migrator.run(MIGRATION_STEP_RELATION_TO_PROPERTY, migrationContext));

        CommentManager propertyCommentManager = new PropertyCommentManager();
        List<Comment> commentsForFile1 = propertyCommentManager.getComments(session, firstFileToComment.getId());
        List<Comment> commentsForFile2 = propertyCommentManager.getComments(session, secondFileToComment.getId());
        List<Comment> commentsForProxy = propertyCommentManager.getComments(session, proxyFileToComment.getId());

        assertEquals(NB_COMMENTS_BY_FILE * 3,
                commentsForFile1.size() + commentsForFile2.size() + commentsForProxy.size());
        for (Comment comment : commentsForFile1) {
            assertEquals(firstFileToComment.getId(), comment.getParentId());
        }
        for (Comment comment : commentsForFile2) {
            assertEquals(secondFileToComment.getId(), comment.getParentId());
            assertNotEquals(proxyFileToComment.getId(), comment.getParentId());
        }

        for (Comment comment : commentsForProxy) {
            assertEquals(proxyFileToComment.getId(), comment.getParentId());
            assertNotEquals(secondFileToComment.getId(), comment.getParentId());
        }

        List<String> expectedLines = Arrays.asList( //
                "Initializing: 0/-1", //
                "Migrating comments from Relation to Property: 1/150", //
                "Migrating comments from Relation to Property: 51/150", //
                "Migrating comments from Relation to Property: 101/150", //
                "Migrating comments from Relation to Property: 150/150", //
                "Done Migrating from Relation to Property: 150/150");
        assertEquals(expectedLines, migrationContext.getProgressLines());
    }

    protected void migrateFromPropertyToSecured(Migrator migrator, boolean commentsWithEmptyParent,
            boolean commentsWithRemovedParent, boolean commentsWithPlacelessParent) {
        ProgressMigrationContext migrationContext = new ProgressMigrationContext();

        runMigration(() -> migrator.run(MIGRATION_STEP_PROPERTY_TO_SECURED, migrationContext));

        List<String> expectedLines = new ArrayList<>();

        if (commentsWithEmptyParent || commentsWithRemovedParent) {
            expectedLines.addAll(Arrays.asList( //
                    "Initializing: 0/-1", //
                    "Migrating comments from Property to Secured: 1/250", //
                    "Migrating comments from Property to Secured: 51/250", //
                    "Migrating comments from Property to Secured: 101/250", //
                    "Migrating comments from Property to Secured: 151/250", //
                    "Migrating comments from Property to Secured: 201/250", //
                    "Migrating comments from Property to Secured: 250/250", //
                    "Done Migrating from Property to Secured: 200/250"));
        } else if (commentsWithPlacelessParent) {
            expectedLines.addAll(Arrays.asList( //
                    "Initializing: 0/-1", //
                    "Migrating comments from Property to Secured: 1/250", //
                    "Migrating comments from Property to Secured: 51/250", //
                    "Migrating comments from Property to Secured: 101/250", //
                    "Migrating comments from Property to Secured: 151/250", //
                    "Migrating comments from Property to Secured: 201/250", //
                    "Migrating comments from Property to Secured: 250/250", //
                    "Done Migrating from Property to Secured: 250/250"));
        } else {
            expectedLines.addAll(Arrays.asList( //
                    "Initializing: 0/-1", //
                    "Migrating comments from Property to Secured: 1/200", //
                    "Migrating comments from Property to Secured: 51/200", //
                    "Migrating comments from Property to Secured: 101/200", //
                    "Migrating comments from Property to Secured: 151/200", //
                    "Migrating comments from Property to Secured: 200/200", //
                    "Done Migrating from Property to Secured: 200/200"));
        }

        assertEquals(expectedLines, migrationContext.getProgressLines());

        // Check that the comments folder are correctly created
        DocumentModelList childrenOfFile1 = session.getChildren(firstFileToComment.getRef(), COMMENT_ROOT_DOC_TYPE);
        assertEquals(1, childrenOfFile1.size());

        DocumentModelList childrenOfFile2 = session.getChildren(secondFileToComment.getRef(), COMMENT_ROOT_DOC_TYPE);
        assertEquals(1, childrenOfFile2.size());

        DocumentModelList childrenOfProxyFile = session.getChildren(proxyFileToComment.getRef(), COMMENT_ROOT_DOC_TYPE);
        assertEquals(1, childrenOfProxyFile.size());

        // Check that the comments are created under the correct folder
        checkCommentsForDocument(childrenOfFile1.get(0).getId());
        checkCommentsForDocument(childrenOfFile2.get(0).getId());
        checkCommentsForDocument(childrenOfFile2.get(0).getId());

        // Ensure that there no more Comments folder
        DocumentModelList rootCommentFolder = session.query(CommentsMigrator.GET_COMMENTS_FOLDERS_QUERY);
        assertEquals(0, rootCommentFolder.size());

        // Ensure that the Comments folder is correctly renamed if it's required
        String query = String.format("SELECT %s FROM Document WHERE %s = '%s' AND %s ='%s'", ECM_UUID, ECM_NAME,
                UNMIGRATED_COMMENTS_FOLDER_NAME, ECM_PRIMARYTYPE, "HiddenFolder");
        assertEquals((commentsWithEmptyParent || commentsWithRemovedParent) ? 2 : 0, session.query(query).size());

        CommentManager treeCommentManager = new TreeCommentManager();
        assertEquals(NB_COMMENTS_BY_FILE, treeCommentManager.getComments(session, secondFileToComment).size());
        assertEquals(NB_COMMENTS_BY_FILE, treeCommentManager.getComments(session, proxyFileToComment).size());

        // Get comments, retrieve only the first level
        assertEquals(NB_COMMENTS_BY_FILE, treeCommentManager.getComments(session, firstFileToComment).size());

        List<DocumentModel> commentsOfFirstFile = session.query(
                String.format("SELECT * FROM Comment WHERE %s = '%s'", ECM_ANCESTORID, firstFileToComment.getId()));

        // Check the tree structure of some replies
        checkTreeStructure(commentsOfFirstFile);
    }

    protected void checkCommentsForDocument(String docId) {
        DocumentModelList dml = session.query(String.format("SELECT * FROM Document WHERE %s = '%s' AND %s = '%s'",
                NXQL.ECM_PARENTID, docId, NXQL.ECM_PRIMARYTYPE, COMMENT_DOC_TYPE));
        assertEquals(NB_COMMENTS_BY_FILE, dml.size());
        dml.forEach((doc) -> assertNull(doc.getACP().getACL(LOCAL_ACL)));
    }

    @SuppressWarnings("deprecation")
    protected void createCommentsAsRelations() {
        createComments(new CommentManagerImpl(CommentServiceHelper.getCommentService().getConfig()));
    }

    protected void createCommentsAsProperty(boolean commentsWithEmptyParent, boolean commentsWithRemovedParent,
            boolean commentsWithPlacelessParent) {
        PropertyCommentManager propertyCommentManager = new PropertyCommentManager();

        // Add comments without comment:parentId
        NuxeoPrincipal principal = session.getPrincipal();
        if (commentsWithEmptyParent) {
            for (int i = 0; i < NB_COMMENTS_BY_FILE; i++) {
                DocumentModel comment = session.createDocumentModel(null, "comment_" + i, COMMENT_DOC_TYPE);
                comment.setPropertyValue(COMMENT_PARENT_ID_PROPERTY, fileWithCommentWithoutParent.getId());
                DocumentModel createdComment = propertyCommentManager.createComment(fileWithCommentWithoutParent,
                        comment);

                // Simulate removing the comment:parentId
                createdComment.setPropertyValue(COMMENT_PARENT_ID_PROPERTY, null);
                session.saveDocument(createdComment);

                notificationManager.addSubscription(principal.getName(), "notification" + i, createdComment, FALSE,
                        principal, "notification" + i);
            }

        }

        if (commentsWithRemovedParent) {
            for (int i = 0; i < NB_COMMENTS_BY_FILE; i++) {
                DocumentModel comment = session.createDocumentModel(null, "comment_" + i, COMMENT_DOC_TYPE);
                comment.setPropertyValue(COMMENT_PARENT_ID_PROPERTY, fileToCommentAndRemove.getId());
                DocumentModel createdComment = propertyCommentManager.createComment(fileToCommentAndRemove, comment);
                notificationManager.addSubscription(principal.getName(), "notification" + i, createdComment, FALSE,
                        principal, "notification" + i);
            }
            // Remove the `fileToCommentAndRemove` which result on comments without a parent.
            session.removeDocument(fileToCommentAndRemove.getRef());
        }

        if (commentsWithPlacelessParent) {
            for (int i = 0; i < NB_COMMENTS_BY_FILE; i++) {
                DocumentModel comment = session.createDocumentModel(null, "comment_" + i, COMMENT_DOC_TYPE);
                comment.setPropertyValue(COMMENT_PARENT_ID_PROPERTY, placelessFileToComment.getId());
                DocumentModel createdComment = propertyCommentManager.createComment(placelessFileToComment, comment);
                notificationManager.addSubscription(principal.getName(), "notification" + i, createdComment, FALSE,
                        principal, "notification" + i);
            }
        }

        // Create comments under the others files
        createComments(propertyCommentManager);

        // Reply on somme comments
        List<Comment> comments = propertyCommentManager.getComments(session, firstFileToComment.getId())
                                                       .stream()
                                                       .sorted(Comparator.comparing(Comment::getId))
                                                       .limit(NB_COMMENT_TO_REPLY_ON_IT)
                                                       .collect(Collectors.toList());

        for (Comment comment : comments) {
            // First reply
            DocumentModel reply = createReply(propertyCommentManager, new IdRef(comment.getId()), 1);
            for (int j = 1; j < NB_REPLY_BY_COMMENT; j++) {
                reply = createReply(propertyCommentManager, reply.getRef(), j + 1);
            }
        }
    }

    protected void createComments(CommentManager commentManager) {
        // To build the comments tree, we will be related on the `comment:parentId`, we should set it, when call
        // commentManager.createComment(docModel, docModel) directly without using CommentableDocumentAdapter#addComment

        boolean setParent = commentManager instanceof PropertyCommentManager;
        NuxeoPrincipal principal = session.getPrincipal();
        for (int i = 0; i < NB_COMMENTS_BY_FILE * 2; i++) {
            DocumentModel comment = session.createDocumentModel(null, "comment_" + i, COMMENT_DOC_TYPE);
            DocumentModel fileToComment = (i % 2 == 0 ? firstFileToComment : secondFileToComment);
            if (setParent) {
                comment.setPropertyValue(COMMENT_PARENT_ID_PROPERTY, fileToComment.getId());
            }
            DocumentModel createdComment = commentManager.createComment(fileToComment, comment);
            notificationManager.addSubscription(principal.getName(), "notification" + i, createdComment, FALSE,
                    principal, "notification" + i);
        }

        for (int i = 0; i < NB_COMMENTS_BY_FILE; i++) {
            DocumentModel comment = session.createDocumentModel(null, "comment_proxy" + i, COMMENT_DOC_TYPE);
            if (setParent) {
                comment.setPropertyValue(COMMENT_PARENT_ID_PROPERTY, proxyFileToComment.getId());
            }
            DocumentModel createdComment = commentManager.createComment(proxyFileToComment, comment);

            notificationManager.addSubscription(principal.getName(), "notification_proxy" + i, createdComment, FALSE,
                    principal, "notification_proxy" + i);
        }

        transactionalFeature.nextTransaction();
    }

    protected DocumentModel createReply(CommentManager commentManager, DocumentRef docRefToComment, int level) {
        DocumentModel documentToComment = session.getDocument(docRefToComment);
        String text = String.format("I am a reply level%d on comment %s !", level, docRefToComment);
        Comment reply = commentManager.createComment(session, newComment(documentToComment.getId(), text));
        transactionalFeature.nextTransaction();
        return session.getDocument(new IdRef(reply.getId()));
    }

    protected void checkTreeStructure(List<DocumentModel> commentsOfFirstFile) {
        // Get replies of last level
        String replyText = String.format("I am a reply level%d", NB_REPLY_BY_COMMENT);
        List<DocumentModel> replies = commentsOfFirstFile.stream()
                                                         .filter(c -> nonNull(
                                                                 c.getPropertyValue(COMMENT_TEXT_PROPERTY)))
                                                         .filter(c -> String.valueOf(
                                                                 c.getPropertyValue(COMMENT_TEXT_PROPERTY))
                                                                            .contains(replyText))
                                                         .collect(Collectors.toList());
        assertEquals(NB_COMMENT_TO_REPLY_ON_IT, replies.size());

        DocumentModel anyReply = replies.get(0);
        DocumentRef[] parentDocumentRefs = session.getParentDocumentRefs(anyReply.getRef());

        for (int i = 0; i < NB_REPLY_BY_COMMENT - 1; i++) {
            // Should be the reply at index i
            assertEquals(COMMENT_DOC_TYPE, session.getDocument(parentDocumentRefs[i]).getType());
        }
        // Should be the first comment of the Thread
        assertEquals(COMMENT_DOC_TYPE, session.getDocument(parentDocumentRefs[NB_REPLY_BY_COMMENT - 1]).getType());
        // Should be the `Comments` folder
        assertEquals(COMMENT_ROOT_DOC_TYPE, session.getDocument(parentDocumentRefs[NB_REPLY_BY_COMMENT]).getType());
        // Should be the `firstFileToComment`
        assertEquals(firstFileToComment.getRef(), parentDocumentRefs[NB_REPLY_BY_COMMENT + 1]);
    }

    public static class ProgressMigrationContext implements MigrationContext {
        protected final List<String> progressLines = new ArrayList<>();

        public List<String> getProgressLines() {
            return progressLines;
        }

        @Override
        public void reportProgress(String message, long num, long total) {
            progressLines.add(String.format("%s: %s/%s", message, num, total));
        }

        @Override
        public void requestShutdown() {
        }

        @Override
        public boolean isShutdownRequested() {
            return false;
        }
    }

    protected void runMigrationStep(String step) {
        runMigration(() -> {
            migrationService.runStep(MIGRATION_ID, step);

            // Wait a bit for the migration to start and poll until migration done
            Duration duration = new Duration(1, SECONDS);
            await().pollDelay(duration)
                   .pollInterval(duration)
                   .until(() -> !migrationService.getStatus(MIGRATION_ID).isRunning());
        });
    }
}
