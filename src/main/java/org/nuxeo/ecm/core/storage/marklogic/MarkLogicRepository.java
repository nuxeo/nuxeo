/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.marklogic;

import static java.lang.Boolean.TRUE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_PROXY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LOCK_CREATED;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LOCK_OWNER;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_TARGET_ID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.resource.spi.ConnectionManager;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;
import org.nuxeo.ecm.core.storage.dbs.DBSStateFlattener;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.Authentication;
import com.marklogic.client.FailedRequestException;
import com.marklogic.client.ResourceNotFoundException;
import com.marklogic.client.admin.ServerConfigurationManager;
import com.marklogic.client.admin.ServerConfigurationManager.UpdatePolicy;
import com.marklogic.client.document.DocumentDescriptor;
import com.marklogic.client.document.DocumentMetadataPatchBuilder.PatchHandle;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.client.query.RawQueryDefinition;

/**
 * MarkLogic implementation of a {@link Repository}.
 *
 * @since 8.3
 */
public class MarkLogicRepository extends DBSRepositoryBase {

    private static final Log log = LogFactory.getLog(MarkLogicRepository.class);

    private static final Function<String, String> ID_FORMATTER = id -> String.format("/%s.xml", id);

    public static final String DB_DEFAULT = "nuxeo";

    protected DatabaseClient markLogicClient;

    public MarkLogicRepository(ConnectionManager cm, MarkLogicRepositoryDescriptor descriptor) {
        super(cm, descriptor.name, descriptor.getFulltextDescriptor());
        markLogicClient = newMarkLogicClient(descriptor);
        initRepository();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        markLogicClient.release();
    }

    // used also by unit tests
    public static DatabaseClient newMarkLogicClient(MarkLogicRepositoryDescriptor descriptor) {
        String host = descriptor.host;
        Integer port = descriptor.port;
        if (StringUtils.isBlank(host) || port == null) {
            throw new NuxeoException("Missing <host> or <port> in MarkLogic repository descriptor");
        }
        String dbname = StringUtils.defaultIfBlank(descriptor.dbname, DB_DEFAULT);
        String user = descriptor.user;
        String password = descriptor.password;
        if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password)) {
            return DatabaseClientFactory.newClient(host, port, dbname, user, password, Authentication.DIGEST);
        }
        return DatabaseClientFactory.newClient(host, port, dbname);
    }

    protected void initRepository() {
        initRoot();
        // Activate Optimistic Locking
        // https://docs.marklogic.com/guide/java/transactions#id_81051
        ServerConfigurationManager configMgr = markLogicClient.newServerConfigManager();
        configMgr.readConfiguration();
        configMgr.setUpdatePolicy(UpdatePolicy.VERSION_OPTIONAL);
        // write the server configuration to the database
        configMgr.writeConfiguration();
    }

    @Override
    protected void initBlobsPaths() {
        // throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public String generateNewId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public State readState(String id) {
        if (log.isTraceEnabled()) {
            log.trace("MarkLogic: READ " + id);
        }
        try {
            return markLogicClient.newXMLDocumentManager().read(ID_FORMATTER.apply(id), new StateHandle()).get();
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    @Override
    public List<State> readStates(List<String> ids) {
        if (log.isTraceEnabled()) {
            log.trace("MarkLogic: READ " + ids);
        }
        String[] markLogicIds = ids.stream().map(ID_FORMATTER).toArray(String[]::new);
        DocumentPage page = markLogicClient.newXMLDocumentManager().read(markLogicIds);
        return StreamSupport.stream(page.spliterator(), false)
                            .map(document -> document.getContent(new StateHandle()).get())
                            .collect(Collectors.toList());
    }

    @Override
    public void createState(State state) {
        String id = state.get(KEY_ID).toString();
        if (log.isTraceEnabled()) {
            log.trace("MarkLogic: CREATE " + id + ": " + state);
        }
        markLogicClient.newXMLDocumentManager().write(ID_FORMATTER.apply(id), new StateHandle(state));
    }

    @Override
    public void updateState(String id, StateDiff diff) {
        XMLDocumentManager docManager = markLogicClient.newXMLDocumentManager();
        PatchHandle patch = new MarkLogicStateUpdateBuilder(docManager::newPatchBuilder).apply(diff);
        if (log.isTraceEnabled()) {
            log.trace("MarkLogic: UPDATE " + id + ": " + patch.toString());
        }
        docManager.patch(ID_FORMATTER.apply(id), patch);
    }

    @Override
    public void deleteStates(Set<String> ids) {
        if (log.isTraceEnabled()) {
            log.trace("MarkLogic: DELETE " + ids);
        }
        String[] markLogicIds = ids.stream().map(ID_FORMATTER).toArray(String[]::new);
        markLogicClient.newXMLDocumentManager().delete(markLogicIds);
    }

    @Override
    public State readChildState(String parentId, String name, Set<String> ignored) {
        RawQueryDefinition query = getChildQuery(parentId, name, ignored);
        return findOne(query);
    }

    @Override
    public boolean hasChild(String parentId, String name, Set<String> ignored) {
        RawQueryDefinition query = getChildQuery(parentId, name, ignored);
        return exist(query);
    }

    private RawQueryDefinition getChildQuery(String parentId, String name, Set<String> ignored) {
        return new MarkLogicQuerySimpleBuilder(markLogicClient.newQueryManager()).eq(KEY_PARENT_ID, parentId)
                                                                                 .eq(KEY_NAME, name)
                                                                                 .notIn(KEY_ID, ignored)
                                                                                 .build();
    }

    @Override
    public List<State> queryKeyValue(String key, Object value, Set<String> ignored) {
        return queryKeyValue(key, value, ignored, this::findAll);
    }

    @Override
    public List<State> queryKeyValue(String key1, Object value1, String key2, Object value2, Set<String> ignored) {
        MarkLogicQuerySimpleBuilder builder = new MarkLogicQuerySimpleBuilder(markLogicClient.newQueryManager());
        builder.eq(key1, value1);
        builder.eq(key2, value2);
        builder.notIn(KEY_ID, ignored);
        return findAll(builder.build());
    }

    @Override
    public void queryKeyValueArray(String key, Object value, Set<String> ids, Map<String, String> proxyTargets,
            Map<String, Object[]> targetProxies) {
        // TODO retrieve only some field
        // https://docs.marklogic.com/guide/search-dev/qbe#id_54044
        RawQueryDefinition query = new MarkLogicQuerySimpleBuilder(markLogicClient.newQueryManager()).eq(key, value)
                                                                                                     .build();
        if (log.isTraceEnabled()) {
            logQuery(query);
        }

        try (DocumentPage page = markLogicClient.newXMLDocumentManager().search(query, 0)) {
            for (DocumentRecord record : page) {
                State state = record.getContent(new StateHandle()).get();
                String id = (String) state.get(KEY_ID);
                ids.add(id);
                if (proxyTargets != null && TRUE.equals(state.get(KEY_IS_PROXY))) {
                    String targetId = (String) state.get(KEY_PROXY_TARGET_ID);
                    proxyTargets.put(id, targetId);
                }
                if (targetProxies != null) {
                    Object[] proxyIds = (Object[]) state.get(KEY_PROXY_IDS);
                    if (proxyIds != null) {
                        targetProxies.put(id, proxyIds);
                    }
                }
            }
        }
    }

    @Override
    public boolean queryKeyValuePresence(String key, String value, Set<String> ignored) {
        return queryKeyValue(key, value, ignored, this::exist);
    }

    @Override
    public PartialList<Map<String, Serializable>> queryAndFetch(DBSExpressionEvaluator evaluator,
            OrderByClause orderByClause, boolean distinctDocuments, int limit, int offset, int countUpTo) {
        MarkLogicQueryBuilder builder = new MarkLogicQueryBuilder(markLogicClient.newQueryManager(),
                evaluator.getExpression(), evaluator.getSelectClause(), orderByClause, evaluator.pathResolver,
                evaluator.fulltextSearchDisabled);
        // TODO add select
        RawQueryDefinition query = builder.buildQuery();
        // Don't do manual projection if there are no projection wildcards, as this brings no new
        // information and is costly. The only difference is several identical rows instead of one.
        boolean manualProjection = !distinctDocuments && builder.hasProjectionWildcard();
        if (manualProjection) {
            // we'll do post-treatment to re-evaluate the query to get proper wildcard projections
            // so we need the full state from the database
            evaluator.parse();
        }
        XMLDocumentManager docManager = markLogicClient.newXMLDocumentManager();
        try (DocumentPage page = docManager.search(query, offset)) {
            List<Map<String, Serializable>> projections = new ArrayList<>((int) page.size());
            for (DocumentRecord record : page) {
                State state = record.getContent(new StateHandle()).get();
                if (manualProjection) {
                    projections.addAll(evaluator.matches(state));
                } else {
                    projections.add(DBSStateFlattener.flatten(state));
                }
            }
            long totalSize;
            if (countUpTo == -1) {
                // count full size
                if (limit == 0) {
                    totalSize = projections.size();
                } else {
                    totalSize = page.getTotalSize();
                }
            } else if (countUpTo == 0) {
                // no count
                totalSize = -1; // not counted
            } else {
                // count only if less than countUpTo
                if (limit == 0) {
                    totalSize = projections.size();
                } else {
                    totalSize = page.getTotalSize();
                }
                if (totalSize > countUpTo) {
                    totalSize = -2; // truncated
                }
            }

            if (log.isTraceEnabled() && projections.size() != 0) {
                log.trace("MarkLogic:    -> " + projections.size());
            }
            return new PartialList<>(projections, totalSize);
        } catch (FailedRequestException fre) {
            throw new QueryParseException("Request was rejected by server", fre);
        }
    }

    @Override
    public Lock getLock(String id) {
        // TODO test performance : retrieve document with read or search document with extract
        // TODO retrieve only some field
        // https://docs.marklogic.com/guide/search-dev/qbe#id_54044
        State state = readState(id);
        if (state == null) {
            throw new DocumentNotFoundException(id);
        }
        String owner = (String) state.get(KEY_LOCK_OWNER);
        if (owner == null) {
            // not locked
            return null;
        }
        Calendar created = (Calendar) state.get(KEY_LOCK_CREATED);
        return new Lock(owner, created);
    }

    @Override
    public Lock setLock(String id, Lock lock) {
        // Here we use Optimistic Locking to set the lock
        // https://docs.marklogic.com/guide/java/transactions#id_81051
        XMLDocumentManager docManager = markLogicClient.newXMLDocumentManager();
        DocumentDescriptor descriptor = docManager.newDescriptor(ID_FORMATTER.apply(id));
        // TODO test performance : retrieve document with read or search document with extract
        // TODO retrieve only some field
        // https://docs.marklogic.com/guide/search-dev/qbe#id_54044
        try {
            if (log.isTraceEnabled()) {
                log.trace("MarkLogic: READ " + id);
            }
            State state = docManager.read(descriptor, new StateHandle()).get();
            Optional<Lock> oldLock = extractLock(state);
            if (oldLock.isPresent()) {
                // Lock owner already set
                return oldLock.get();
            }
            // Set the lock
            PatchHandle patch = new MarkLogicLockUpdateBuilder(docManager::newPatchBuilder).set(lock);
            if (log.isTraceEnabled()) {
                log.trace("MarkLogic: UPDATE " + id + ": " + patch.toString());
            }
            docManager.patch(descriptor, patch);
            // doc is now locked
            return null;
        } catch (ResourceNotFoundException e) {
            // Document not found
            throw new DocumentNotFoundException(id, e);
        } catch (FailedRequestException e) {
            // There was a race condition - another lock was set
            return extractLock(readState(id)).orElseThrow(() -> new ConcurrentUpdateException("Lock " + id));
        }
    }

    @Override
    public Lock removeLock(String id, String owner) {
        // Here we use Optimistic Locking to set the lock
        // https://docs.marklogic.com/guide/java/transactions#id_81051
        XMLDocumentManager docManager = markLogicClient.newXMLDocumentManager();
        DocumentDescriptor descriptor = docManager.newDescriptor(ID_FORMATTER.apply(id));
        // TODO test performance : retrieve document with read or search document with extract
        // TODO retrieve only some field
        // https://docs.marklogic.com/guide/search-dev/qbe#id_54044
        try {
            if (log.isTraceEnabled()) {
                log.trace("MarkLogic: READ " + id);
            }
            // Retrieve state of document
            State state = docManager.read(descriptor, new StateHandle()).get();
            Optional<Lock> oldLockOpt = extractLock(state);
            if (oldLockOpt.isPresent()) {
                // A Lock exist on document
                Lock oldLock = oldLockOpt.get();
                if (LockManager.canLockBeRemoved(oldLock.getOwner(), owner)) {
                    // Delete the lock
                    PatchHandle patch = new MarkLogicLockUpdateBuilder(docManager::newPatchBuilder).delete();
                    if (log.isTraceEnabled()) {
                        log.trace("MarkLogic: UPDATE " + id + ": " + patch.toString());
                    }
                    docManager.patch(descriptor, patch);
                    // Return previous lock
                    return oldLock;
                } else {
                    // existing mismatched lock, flag failure
                    return new Lock(oldLock.getOwner(), oldLock.getCreated(), true);
                }
            } else {
                // document was not locked
                return null;
            }
        } catch (ResourceNotFoundException e) {
            // Document not found
            throw new DocumentNotFoundException(id, e);
        }
    }

    private Optional<Lock> extractLock(State state) {
        String owner = (String) state.get(KEY_LOCK_OWNER);
        if (owner == null) {
            return Optional.empty();
        }
        Calendar oldCreated = (Calendar) state.get(KEY_LOCK_CREATED);
        return Optional.of(new Lock(owner, oldCreated));
    }

    @Override
    public void closeLockManager() {
    }

    @Override
    public void clearLockManagerCaches() {
    }

    @Override
    public void markReferencedBinaries() {
        throw new IllegalStateException("Not implemented yet");
    }

    private <T> T queryKeyValue(String key, Object value, Set<String> ignored, Function<RawQueryDefinition, T> executor) {
        MarkLogicQuerySimpleBuilder builder = new MarkLogicQuerySimpleBuilder(markLogicClient.newQueryManager());
        builder.eq(key, value);
        builder.notIn(KEY_ID, ignored);
        return executor.apply(builder.build());
    }

    private boolean exist(RawQueryDefinition query) {
        if (log.isTraceEnabled()) {
            logQuery(query);
        }
        return markLogicClient.newQueryManager().findOne(query) != null;
    }

    private State findOne(RawQueryDefinition query) {
        if (log.isTraceEnabled()) {
            logQuery(query);
        }
        try (DocumentPage page = markLogicClient.newXMLDocumentManager().search(query, 0)) {
            if (page.hasNext()) {
                return page.nextContent(new StateHandle()).get();
            }
            return null;
        }
    }

    private List<State> findAll(RawQueryDefinition query) {
        if (log.isTraceEnabled()) {
            logQuery(query);
        }
        return findAll(query, 1);
    }

    private List<State> findAll(RawQueryDefinition query, long start) {
        try (DocumentPage page = markLogicClient.newXMLDocumentManager().search(query, start)) {
            List<State> states = new ArrayList<>((int) (page.getTotalSize() - start + 1));
            for (DocumentRecord record : page) {
                states.add(record.getContent(new StateHandle()).get());
            }
            if (page.hasNextPage()) {
                states.addAll(findAll(query, start + page.getPageSize()));
            }
            return states;
        }
    }

    private void logQuery(RawQueryDefinition query) {
        log.trace("MarkLogic: QUERY " + query.getHandle());
    }

}
