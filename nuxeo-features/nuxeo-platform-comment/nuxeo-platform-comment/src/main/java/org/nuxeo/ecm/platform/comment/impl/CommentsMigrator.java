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

package org.nuxeo.ecm.platform.comment.impl;

import static java.lang.Boolean.TRUE;
import static java.util.function.Predicate.isEqual;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STATE_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STATE_RELATION;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STEP_RELATION_TO_PROPERTY;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.DISABLE_NOTIFICATION_SERVICE;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.migrator.AbstractRepositoryMigrator;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.platform.comment.service.CommentService;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationService.MigrationContext;

/**
 * Migrator of comments.
 *
 * @since 10.3
 */
public class CommentsMigrator extends AbstractRepositoryMigrator {

    private static final Logger log = LogManager.getLogger(CommentsMigrator.class);

    protected static final int BATCH_SIZE = 50;

    @Override
    protected String probeSession(CoreSession session) {
        CommentService commentComponent = Framework.getService(CommentService.class);
        CommentServiceConfig commentServiceConfig = commentComponent.getConfig();
        if (commentServiceConfig != null) {
            Graph graph = Framework.getService(RelationManager.class).getGraph(commentServiceConfig.graphName, session);
            if (graph.getStatements().size() > 0) {
                return MIGRATION_STATE_RELATION;
            }
        }
        return MIGRATION_STATE_PROPERTY;
    }

    @Override
    protected void migrateSession(CoreSession session) {
        CommentService commentComponent = Framework.getService(CommentService.class);
        CommentServiceConfig commentServiceConfig = commentComponent.getConfig();
        if (commentServiceConfig != null) {
            RelationManager relationManager = Framework.getService(RelationManager.class);
            Graph graph = relationManager.getGraph(commentServiceConfig.graphName, session);
            List<Statement> statements = graph.getStatements();
            checkShutdownRequested();

            processBatched(BATCH_SIZE, statements,
                    statement -> migrateComments(session, relationManager, commentServiceConfig, statement),
                    "Migrating comments");
            reportProgress("Done", statements.size(), statements.size());
        }
    }

    @Override
    public String probeState() {
        List<String> repositoryNames = Framework.getService(RepositoryService.class).getRepositoryNames();
        if (repositoryNames.stream().map(this::probeRepository).anyMatch(isEqual(MIGRATION_STATE_RELATION))) {
            return MIGRATION_STATE_RELATION;
        }
        return MIGRATION_STATE_PROPERTY;
    }

    @Override
    public void run(String step, MigrationContext migrationContext) {
        if (!MIGRATION_STEP_RELATION_TO_PROPERTY.equals(step)) {
            throw new NuxeoException("Unknown migration step: " + step);
        }
        this.migrationContext = migrationContext;
        reportProgress("Initializing", 0, -1); // unknown
        List<String> repositoryNames = Framework.getService(RepositoryService.class).getRepositoryNames();
        try {
            repositoryNames.forEach(this::migrateRepository);
        } catch (MigrationShutdownException e) {
            return;
        }
    }

    @SuppressWarnings("unchecked")
    protected void migrateComments(CoreSession session, RelationManager relationManager, CommentServiceConfig config,
            Statement statement) {
        Map<String, Object> ctxMap = Collections.singletonMap(ResourceAdapter.CORE_SESSION_CONTEXT_KEY, session);
        QNameResourceImpl object = (QNameResourceImpl) statement.getObject();
        DocumentModel parent = (DocumentModel) relationManager.getResourceRepresentation(config.documentNamespace,
                object, ctxMap);

        QNameResourceImpl subject = (QNameResourceImpl) statement.getSubject();
        DocumentModel comment = (DocumentModel) relationManager.getResourceRepresentation(config.commentNamespace,
                subject, ctxMap);

        if (parent != null && comment != null) {
            comment.putContextData(DISABLE_NOTIFICATION_SERVICE, TRUE); // Remove notifications
            comment.setPropertyValue(COMMENT_PARENT_ID, parent.getId());
            session.saveDocument(comment);
        } else if (parent == null && comment == null) {
            log.debug("Documents {} and {} do not exist, they can not be migrated", object.getLocalName(),
                    subject.getLocalName());
        } else {
            log.debug("Document {} does not exist, it can not be migrated",
                    parent == null ? object.getLocalName() : subject.getLocalName());
        }

        Graph graph = relationManager.getGraph(config.graphName, session);
        graph.remove(statement);
    }

    @Override
    public void notifyStatusChange() {
        CommentService commentComponent = Framework.getService(CommentService.class);
        commentComponent.invalidateCommentManagerImplementation();
    }
}
