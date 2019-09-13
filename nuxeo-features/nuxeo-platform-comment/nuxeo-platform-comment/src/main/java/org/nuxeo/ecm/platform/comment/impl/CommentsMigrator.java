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
import static org.nuxeo.ecm.core.api.security.ACL.LOCAL_ACL;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_NAME;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_PARENTID;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_PRIMARYTYPE;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STATE_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STATE_RELATION;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STATE_SECURED;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STEP_PROPERTY_TO_SECURED;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.MIGRATION_STEP_RELATION_TO_PROPERTY;
import static org.nuxeo.ecm.platform.comment.impl.AbstractCommentManager.COMMENTS_DIRECTORY;
import static org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager.HIDDEN_FOLDER_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.DISABLE_NOTIFICATION_SERVICE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.migrator.AbstractRepositoryMigrator;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.service.CommentService;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.ecm.platform.comment.service.CommentServiceHelper;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.migration.MigrationService.MigrationContext;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Migrator of comments.
 *
 * @since 10.3
 */
public class CommentsMigrator extends AbstractRepositoryMigrator {

    private static final Logger log = LogManager.getLogger(CommentsMigrator.class);

    protected static final int BATCH_SIZE = 50;

    public static final String UNMIGRATED_COMMENTS_FOLDER_NAME = "UnMigratedComments";

    /**
     * @since 11.1.
     */
    public static final String GET_COMMENTS_FOLDERS_QUERY = String.format(
            "SELECT %s FROM Document WHERE %s = '%s' AND %s ='%s'", ECM_UUID, ECM_NAME, COMMENTS_DIRECTORY,
            ECM_PRIMARYTYPE, HIDDEN_FOLDER_TYPE);

    @Override
    protected String probeSession(CoreSession session) {
        CommentService commentComponent = (CommentService) Framework.getRuntime().getComponent(CommentService.NAME);
        CommentServiceConfig commentServiceConfig = commentComponent.getConfig();
        if (commentServiceConfig != null) {
            Graph graph = Framework.getService(RelationManager.class).getGraph(commentServiceConfig.graphName, session);
            if (graph.getStatements().size() > 0) {
                return MIGRATION_STATE_RELATION;
            }
        }
        // If not in relation, check if there are still comments under hidden Comments folder(s)
        if (hasUnsecuredComments(session)) {
            return MIGRATION_STATE_PROPERTY;
        }
        // Comments are already secured
        return MIGRATION_STATE_SECURED;
    }

    @Override
    public void run(String step, MigrationContext migrationContext) {
        if (!Arrays.asList(MIGRATION_STEP_RELATION_TO_PROPERTY, MIGRATION_STEP_PROPERTY_TO_SECURED).contains(step)) {
            throw new NuxeoException("Unknown migration step: " + step);
        }
        this.migrationContext = migrationContext;
        this.step = step;
        reportProgress("Initializing", 0, -1); // unknown
        List<String> repositoryNames = Framework.getService(RepositoryService.class).getRepositoryNames();
        try {
            repositoryNames.forEach(repoName -> migrateRepository(step, migrationContext, repoName));
        } catch (MigrationShutdownException e) {
            return;
        }
    }

    @Override
    protected void migrateSession(CoreSession session) {
        migrateSession(this.step, this.migrationContext, session);
    }

    @Override
    protected void migrateRepository(String step, MigrationContext migrationContext, String repositoryName) {
        TransactionHelper.runInTransaction(() -> CoreInstance.doPrivileged(repositoryName,
                (CoreSession session) -> migrateSession(step, migrationContext, session)));
    }

    @Override
    protected void migrateSession(String step, MigrationContext migrationContext, CoreSession session) {
        if (MIGRATION_STEP_RELATION_TO_PROPERTY.equals(step)) {
            migrateSessionRelationToProperty(session, migrationContext);
        } else if (MIGRATION_STEP_PROPERTY_TO_SECURED.equals(step)) {
            migrateSessionPropertyToSecured(session, migrationContext);
        }
    }

    /**
     * @since 11.1
     */
    protected void migrateSessionRelationToProperty(CoreSession session, MigrationContext migrationContext) {
        CommentServiceConfig commentServiceConfig = CommentServiceHelper.getCommentService().getConfig();
        if (commentServiceConfig != null) {
            RelationManager relationManager = Framework.getService(RelationManager.class);
            Graph graph = relationManager.getGraph(commentServiceConfig.graphName, session);
            List<Statement> statements = graph.getStatements();
            checkShutdownRequested(migrationContext);

            processBatched(migrationContext, BATCH_SIZE, statements,
                    statement -> migrateCommentsFromRelationToProperty(session, relationManager, commentServiceConfig,
                            statement),
                    "Migrating comments from Relation to Property");
            reportProgress("Done Migrating from Relation to Property", statements.size(), statements.size());
        }
    }

    /**
     * @since 11.1
     */
    protected void migrateCommentsFromRelationToProperty(CoreSession session, RelationManager relationManager,
            CommentServiceConfig config, Statement statement) {
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
            log.warn("Documents {} and {} do not exist, they can not be migrated", object.getLocalName(),
                    subject.getLocalName());
        } else {
            log.warn("Document {} does not exist, it can not be migrated",
                    parent == null ? object.getLocalName() : subject.getLocalName());
        }

        Graph graph = relationManager.getGraph(config.graphName, session);
        graph.remove(statement);

    }

    /**
     * @since 11.1
     */
    protected void migrateSessionPropertyToSecured(CoreSession session, MigrationContext migrationContext) {
        List<String> comments = getUnsecuredCommentIds(session);
        // For migration purpose and to avoid any duplication, we should rely mainly on `TreeCommentManager`
        // For 10.10 (backward compatibility) Framework.getService(CommentManager.class) will return
        // `PropertyCommentManager` the new location should be computed by the `TreeCommentManager`
        CommentManager commentManager = new TreeCommentManager();
        processBatched(migrationContext, BATCH_SIZE, comments,
                comment -> migrateCommentsFromPropertyToSecured(session, commentManager, new IdRef(comment)),
                "Migrating comments from Property to Secured");

        // All comments were migrated, now we can delete the empty folders
        int totalOfComments = comments.size();
        int nbeOfUnMigratedComments = getUnsecuredCommentIds(session).size();
        if (nbeOfUnMigratedComments == 0) {
            session.query(GET_COMMENTS_FOLDERS_QUERY).forEach((folder -> session.removeDocument(folder.getRef())));

            IdRef[] documentsToRemove = session.queryProjection(GET_COMMENTS_FOLDERS_QUERY, 0, 0)
                                               .stream()
                                               .map(m -> new IdRef((String) m.get(ECM_UUID)))
                                               .toArray(IdRef[]::new);

            session.removeDocuments(documentsToRemove);

            reportProgress("Done Migrating from Property to Secured", totalOfComments, totalOfComments);
        } else {
            // For some reason some comments still not migrated (for example a comment without a comment:parentId)
            // in this case we just rename the Comments folder (see getCommentFolders)
            log.warn(String.format(
                    "Some comments have not been migrated, see logs for more information. The folder containing these comments will be renamed to %s",
                    UNMIGRATED_COMMENTS_FOLDER_NAME));

            session.query(GET_COMMENTS_FOLDERS_QUERY).stream().forEach(docModel -> {
                session.move(docModel.getRef(), null, UNMIGRATED_COMMENTS_FOLDER_NAME);
                docModel.putContextData(DISABLE_NOTIFICATION_SERVICE, TRUE);
                session.saveDocument(docModel);
            });
            session.save();

            reportProgress("Done Migrating from Property to Secured", totalOfComments - nbeOfUnMigratedComments,
                    totalOfComments);
        }
    }

    /**
     * @since 11.1
     */
    protected void migrateCommentsFromPropertyToSecured(CoreSession session, CommentManager commentManager,
            IdRef commentIdRef) {
        DocumentModel commentDocModel = session.getDocument(commentIdRef);
        String parentId = (String) commentDocModel.getPropertyValue(COMMENT_PARENT_ID);
        if (StringUtils.isEmpty(parentId)) {
            log.warn(
                    "The comment document model with IdRef: {} cannot be migrated, because his 'comment:parentId' is not defined",
                    commentIdRef);
            return;
        }

        DocumentRef parentDocRef = new IdRef(parentId);
        DocumentModel parentDocModel = session.getDocument(parentDocRef);

        DocumentRef destination = new PathRef(commentManager.getLocationOfCommentCreation(session, parentDocModel));

        // Move the commentIdRef under the new destination (under the `Comments` folder in the case of the first comment
        // or under the comment itself in the case of reply)
        session.move(commentIdRef, destination, null);

        // Remove notifications
        commentDocModel.putContextData(DISABLE_NOTIFICATION_SERVICE, TRUE);

        // Strip ACLs
        ACP acp = session.getACP(commentIdRef);
        acp.removeACL(LOCAL_ACL);
        session.setACP(commentIdRef, acp, true);

        session.saveDocument(commentDocModel);
        session.save();
    }

    /**
     * @since 11.1
     */
    protected boolean hasUnsecuredComments(CoreSession session) {
        List<String> folderIds = getCommentFolders(session);
        return folderIds.stream().anyMatch(folderId -> session.hasChildren(new IdRef(folderId)));
    }

    /**
     * @since 11.1
     */
    protected List<String> getUnsecuredCommentIds(CoreSession session) {
        List<String> parentIds = getCommentFolders(session);
        if (parentIds.isEmpty()) {
            return Collections.emptyList();
        }

        String query = String.format("SELECT %s FROM Comment WHERE %s IN (%s)", ECM_UUID, ECM_PARENTID,
                buildInClause(parentIds));

        return session.queryProjection(query, 0, 0)
                      .stream()
                      .map(m -> (String) m.get(ECM_UUID))
                      .collect(Collectors.toList());
    }

    /**
     * @since 11.1
     */
    protected List<String> getCommentFolders(CoreSession session) {
        List<String> parentIds = new ArrayList<>();

        List<String> rootCommentsFolderIds = session.queryProjection(GET_COMMENTS_FOLDERS_QUERY, 0, 0)
                                                    .stream()
                                                    .map(entry -> (String) entry.get(ECM_UUID))
                                                    .collect(Collectors.toList());

        // According to the case:
        // Comments created using PropertyCommentManager are stored directly under `Comments` hidden folder (one per domain)
        // Comments created using CommentManagerImpl are stored under subfolder (named with a timestamp) of the `Comments` folder
        if (!rootCommentsFolderIds.isEmpty()) {
            // Get all `Comments` hidden folders
            String query = String.format("SELECT %s FROM Document WHERE %s IN (%s) AND %s = '%s'", ECM_UUID,
                    ECM_PARENTID, buildInClause(rootCommentsFolderIds), ECM_PRIMARYTYPE, HIDDEN_FOLDER_TYPE);

            // Get timestamp subfolders (see CommentManagerImpl#getCommentPathList)
            List<String> timestampCommentFoldersIds = session.queryProjection(query, 0, 0)
                                                             .stream()
                                                             .map(entry -> (String) entry.get(ECM_UUID))
                                                             .collect(Collectors.toList());
            parentIds.addAll(rootCommentsFolderIds);
            parentIds.addAll(timestampCommentFoldersIds);
        }
        return parentIds;
    }

    @Override
    public String probeState() {
        List<String> repositoryNames = Framework.getService(RepositoryService.class).getRepositoryNames();
        Set<String> probes = repositoryNames.stream().map(this::probeRepository).collect(Collectors.toSet());
        if (probes.contains(MIGRATION_STATE_RELATION)) {
            return MIGRATION_STATE_RELATION;
        } else if (probes.contains(MIGRATION_STATE_PROPERTY)) {
            return MIGRATION_STATE_PROPERTY;
        }
        return MIGRATION_STATE_SECURED;
    }

    @Override
    public void notifyStatusChange() {
        CommentService commentComponent = (CommentService) Framework.getRuntime().getComponent(CommentService.NAME);
        commentComponent.invalidateCommentManagerImplementation();
    }

    @SuppressWarnings("unchecked")
    protected void migrateComments(CoreSession session, RelationManager relationManager, CommentServiceConfig config,
            Statement statement) {
        migrateCommentsFromRelationToProperty(session, relationManager, config, statement);
    }

    /**
     * @since 11.1
     */
    protected String buildInClause(List<String> parentIds) {
        return parentIds.stream().collect(Collectors.joining("', '", "'", "'"));
    }
}
