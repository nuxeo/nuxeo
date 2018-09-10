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
 */

package org.nuxeo.ecm.platform.comment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.comment.AbstractTestCommentManager.newConfig;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_ID;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STATE_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STATE_RELATION;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STEP_RELATION_TO_PROPERTY;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.CommentManagerImpl;
import org.nuxeo.ecm.platform.comment.impl.CommentsMigrator;
import org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationService;
import org.nuxeo.runtime.migration.MigrationService.MigrationContext;
import org.nuxeo.runtime.migration.MigrationService.Migrator;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.comment")
@Deploy("org.nuxeo.ecm.relations.api")
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.platform.comment.tests:OSGI-INF/comment-jena-contrib.xml")
public class TestCommentsMigrator {

    protected static final int NB_COMMENTS = 100;

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected MigrationService migrationService;

    protected static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMigrationImpl() {
        Migrator migrator = new CommentsMigrator();
        List<String> progressLines = new ArrayList<>();
        MigrationContext migrationContext = new MigrationContext() {
            @Override
            public void reportProgress(String message, long num, long total) {
                String line = message + ": " + num + "/" + total;
                progressLines.add(line);
            }

            @Override
            public void requestShutdown() {
            }

            @Override
            public boolean isShutdownRequested() {
                return false;
            }
        };

        testMigration(() -> migrator.run(MIGRATION_STEP_RELATION_TO_PROPERTY, migrationContext));

        List<String> expectedLines = Arrays.asList( //
                "Initializing: 0/-1", //
                "Migrating comments: 1/100", //
                "Migrating comments: 51/100", //
                "Migrating comments: 100/100", //
                "Done: 100/100");
        assertEquals(expectedLines, progressLines);
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

        testMigration(() -> {
            migrationService.runStep(MIGRATION_ID, MIGRATION_STEP_RELATION_TO_PROPERTY);

            // wait a bit for the migration to start
            sleep(1000);

            // poll until migration done
            long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
            while (System.currentTimeMillis() < deadline) {
                if (!migrationService.getStatus(MIGRATION_ID).isRunning()) {
                    break;
                }
                sleep(100);
            }
        });

        commentManager = Framework.getService(CommentManager.class);
        assertTrue(commentManager.getClass().getName(), commentManager instanceof PropertyCommentManager);

        status = migrationService.getStatus(MIGRATION_ID);
        assertNotNull(status);
        assertFalse(status.isRunning());
        assertEquals(MIGRATION_STATE_PROPERTY, status.getState());
    }

    @SuppressWarnings("deprecation")
    protected void testMigration(Runnable migrator) {

        CommentManager relationCommentManager = new CommentManagerImpl(newConfig());
        CommentManager propertyCommentManager = new PropertyCommentManager();

        DocumentModel domain = session.createDocumentModel("/", "test-domain", "Domain");
        session.createDocument(domain);

        DocumentModel file1 = session.createDocumentModel("/test-domain", "file1", "File");
        file1 = session.createDocument(file1);

        DocumentModel file2 = session.createDocumentModel("/test-domain", "file2", "File");
        file2 = session.createDocument(file2);

        // create some comments
        for (int i = 0; i < NB_COMMENTS; i++) {
            DocumentModel comment = session.createDocumentModel(null, "comment_" + i, COMMENT_DOC_TYPE);
            if (i % 2 == 0) {
                relationCommentManager.createComment(file1, comment);
            } else {
                relationCommentManager.createComment(file2, comment);
            }
        }

        session.save();
        transactionalFeature.nextTransaction();

        // migrate
        migrator.run();

        // check resulting tags
        List<Comment> commentsForFile1 = propertyCommentManager.getComments(session, file1.getId());
        List<Comment> commentsForFile2 = propertyCommentManager.getComments(session, file2.getId());

        assertEquals(NB_COMMENTS, commentsForFile1.size() + commentsForFile2.size());
        for (Comment c : commentsForFile1) {
            assertEquals(file1.getId(), c.getParentId());
        }
        for (Comment c : commentsForFile2) {
            assertEquals(file2.getId(), c.getParentId());
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testProbe() {

        CommentServiceConfig config = newConfig();
        CommentManager relationCommentManager = new CommentManagerImpl(config);
        CommentManager propertyCommentManager = new PropertyCommentManager();

        DocumentModel domain = session.createDocumentModel("/", "test-domain", "Domain");
        session.createDocument(domain);
        DocumentModel file = session.createDocumentModel("/test-domain", "file1", "File");
        file = session.createDocument(file);
        session.save();

        // no comments, detected as already migrated
        Migrator migrator = new CommentsMigrator();
        assertEquals(MIGRATION_STATE_PROPERTY, migrator.probeState());

        // just a relation-based comment, detected as not migrated
        DocumentModel comment = session.createDocumentModel(null, "comment", COMMENT_DOC_TYPE);
        comment = relationCommentManager.createComment(file, comment);
        session.save();
        assertEquals(MIGRATION_STATE_RELATION, migrator.probeState());

        // both a relation-based comment and a property-based comment, detected as not migrated
        DocumentModel otherComment = session.createDocumentModel(null, "comment", COMMENT_DOC_TYPE);
        otherComment.setPropertyValue(COMMENT_PARENT_ID, file.getId());
        propertyCommentManager.createComment(file, otherComment);
        session.save();
        assertEquals(MIGRATION_STATE_RELATION, migrator.probeState());

        // just a property-based comment, detected as migrated
        relationCommentManager.deleteComment(file, comment);
        session.save();

        // Simulate comment deletion event
        RelationManager relationManager = Framework.getService(RelationManager.class);
        Resource commentRes = relationManager.getResource(config.commentNamespace, comment, null);
        Graph graph = relationManager.getGraph(config.graphName, session);
        List<Statement> statementList = graph.getStatements(commentRes, null, null);
        graph.remove(statementList);

        assertEquals(MIGRATION_STATE_PROPERTY, migrator.probeState());
    }

}
