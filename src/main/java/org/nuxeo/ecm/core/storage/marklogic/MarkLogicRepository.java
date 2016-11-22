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
import static org.nuxeo.ecm.core.api.ScrollResultImpl.emptyResult;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.resource.spi.ConnectionManager;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.api.ScrollResultImpl;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;
import org.nuxeo.ecm.core.storage.dbs.DBSStateFlattener;
import org.nuxeo.ecm.core.storage.marklogic.MarkLogicQueryBuilder.MarkLogicQuery;

import com.google.common.base.Strings;
import com.marklogic.xcc.AdhocQuery;
import com.marklogic.xcc.Content;
import com.marklogic.xcc.ContentFactory;
import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.ContentSourceFactory;
import com.marklogic.xcc.ModuleInvoke;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.RequestException;

/**
 * MarkLogic implementation of a {@link Repository}.
 *
 * @since 8.3
 */
public class MarkLogicRepository extends DBSRepositoryBase {

    private static final Log log = LogFactory.getLog(MarkLogicRepository.class);

    private static final Function<String, String> ID_FORMATTER = id -> String.format("/%s.xml", id);

    public static final String DB_DEFAULT = "nuxeo";

    protected static final String NOSCROLL_ID = "noscroll";

    protected ContentSource xccContentSource;

    /** Last value used from the in-memory sequence. Used by unit tests. */
    protected long sequenceLastValue;

    protected final List<MarkLogicRangeElementIndexDescriptor> rangeElementIndexes;

    public MarkLogicRepository(ConnectionManager cm, MarkLogicRepositoryDescriptor descriptor) {
        super(cm, descriptor.name, descriptor);
        xccContentSource = newMarkLogicContentSource(descriptor);
        rangeElementIndexes = descriptor.rangeElementIndexes.stream()
                                                            .map(MarkLogicRangeElementIndexDescriptor::new)
                                                            .collect(Collectors.toList());
        initRepository();
    }

    @Override
    public List<IdType> getAllowedIdTypes() {
        return Collections.singletonList(IdType.varchar);
    }

    // used also by unit tests
    public static ContentSource newMarkLogicContentSource(MarkLogicRepositoryDescriptor descriptor) {
        String host = descriptor.host;
        Integer port = descriptor.port;
        if (StringUtils.isBlank(host) || port == null) {
            throw new NuxeoException("Missing <host> or <port> in MarkLogic repository descriptor");
        }
        String dbname = StringUtils.defaultIfBlank(descriptor.dbname, DB_DEFAULT);
        String user = descriptor.user;
        String password = descriptor.password;
        return ContentSourceFactory.newContentSource(host, port, user, password, dbname);
    }

    protected void initRepository() {
        if (readState(getRootId()) == null) {
            initRoot();
        }
    }

    @Override
    protected void initBlobsPaths() {
        // throw new IllegalStateException("Not implemented yet");
    }

    @Override
    public String generateNewId() {
        if (DEBUG_UUIDS) {
            Long id = getNextSequenceId();
            return "UUID_" + id;
        }
        return UUID.randomUUID().toString();
    }

    // Used by unit tests
    protected synchronized Long getNextSequenceId() {
        sequenceLastValue++;
        return Long.valueOf(sequenceLastValue);
    }

    @Override
    public State readState(String id) {
        if (log.isTraceEnabled()) {
            log.trace("MarkLogic: READ " + id);
        }
        try (Session session = xccContentSource.newSession()) {
            String query = "fn:doc('" + ID_FORMATTER.apply(id) + "')";
            AdhocQuery request = session.newAdhocQuery(query);
            // ResultSequence will be closed by Session close
            ResultSequence rs = session.submitRequest(request);
            if (rs.hasNext()) {
                return MarkLogicStateDeserializer.deserialize(rs.asStrings()[0]);
            }
            return null;
        } catch (RequestException e) {
            throw new NuxeoException("An exception happened during xcc call", e);
        }
    }

    @Override
    public List<State> readStates(List<String> ids) {
        if (log.isTraceEnabled()) {
            log.trace("MarkLogic: READ " + ids);
        }
        try (Session session = xccContentSource.newSession()) {
            String query = ids.stream().map(ID_FORMATTER).map(id -> "'" + id + "'").collect(
                    Collectors.joining(",", "fn:doc((", "))"));
            AdhocQuery request = session.newAdhocQuery(query);
            // ResultSequence will be closed by Session close
            ResultSequence rs = session.submitRequest(request);
            return Arrays.stream(rs.asStrings())
                         .map(MarkLogicStateDeserializer::deserialize)
                         .collect(Collectors.toList());
        } catch (RequestException e) {
            throw new NuxeoException("An exception happened during xcc call", e);
        }
    }

    @Override
    public void createState(State state) {
        String id = state.get(KEY_ID).toString();
        if (log.isTraceEnabled()) {
            log.trace("MarkLogic: CREATE " + id + ": " + state);
        }
        try (Session session = xccContentSource.newSession()) {
            session.insertContent(convert(state));
        } catch (RequestException e) {
            throw new NuxeoException("An exception happened during xcc call", e);
        }
    }

    @Override
    public void createStates(List<State> states) {
        if (log.isTraceEnabled()) {
            log.trace("MarkLogic: CREATE ["
                    + states.stream().map(state -> state.get(KEY_ID).toString()).collect(Collectors.joining(", "))
                    + "]: " + states);
        }
        try (Session session = xccContentSource.newSession()) {
            Content[] contents = states.stream().map(this::convert).toArray(Content[]::new);
            session.insertContent(contents);
        } catch (RequestException e) {
            throw new NuxeoException("An exception happened during xcc call", e);
        }
    }

    private Content convert(State state) {
        String id = state.get(KEY_ID).toString();
        return ContentFactory.newContent(ID_FORMATTER.apply(id), MarkLogicStateSerializer.serialize(state), null);
    }

    @Override
    public void updateState(String id, StateDiff diff) {
        String patch = MarkLogicStateSerializer.serialize(diff);
        if (log.isTraceEnabled()) {
            log.trace("MarkLogic: UPDATE " + id + ": " + patch);
        }
        try (Session session = xccContentSource.newSession()) {
            ModuleInvoke request = session.newModuleInvoke("/ext/nuxeo/patch.xqy");
            request.setNewStringVariable("uri", ID_FORMATTER.apply(id));
            request.setNewStringVariable("patch-string", patch);
            // ResultSequence will be closed by Session close
            session.submitRequest(request);
        } catch (RequestException e) {
            throw new NuxeoException("An exception happened during xcc call", e);
        }
    }

    @Override
    public void deleteStates(Set<String> ids) {
        if (log.isTraceEnabled()) {
            log.trace("MarkLogic: DELETE " + ids);
        }
        try (Session session = xccContentSource.newSession()) {
            String query = ids.stream().map(ID_FORMATTER).map(id -> "'" + id + "'").collect(
                    Collectors.joining(",", "xdmp:document-delete((", "))"));
            AdhocQuery request = session.newAdhocQuery(query);
            // ResultSequence will be closed by Session close
            session.submitRequest(request);
        } catch (RequestException e) {
            throw new NuxeoException("An exception happened during xcc call", e);
        }
    }

    @Override
    public State readChildState(String parentId, String name, Set<String> ignored) {
        String query = getChildQuery(parentId, name, ignored);
        return findOne(query);
    }

    @Override
    public boolean hasChild(String parentId, String name, Set<String> ignored) {
        String query = getChildQuery(parentId, name, ignored);
        return exist(query);
    }

    private String getChildQuery(String parentId, String name, Set<String> ignored) {
        return new MarkLogicQuerySimpleBuilder(rangeElementIndexes).eq(KEY_PARENT_ID, parentId)
                                                                   .eq(KEY_NAME, name)
                                                                   .notIn(KEY_ID, ignored)
                                                                   .build();
    }

    @Override
    public List<State> queryKeyValue(String key, Object value, Set<String> ignored) {
        MarkLogicQuerySimpleBuilder builder = new MarkLogicQuerySimpleBuilder(rangeElementIndexes);
        builder.eq(key, value);
        builder.notIn(KEY_ID, ignored);
        return findAll(builder.build());
    }

    @Override
    public List<State> queryKeyValue(String key1, Object value1, String key2, Object value2, Set<String> ignored) {
        MarkLogicQuerySimpleBuilder builder = new MarkLogicQuerySimpleBuilder(rangeElementIndexes);
        builder.eq(key1, value1);
        builder.eq(key2, value2);
        builder.notIn(KEY_ID, ignored);
        return findAll(builder.build());
    }

    @Override
    public void queryKeyValueArray(String key, Object value, Set<String> ids, Map<String, String> proxyTargets,
            Map<String, Object[]> targetProxies) {
        MarkLogicQuerySimpleBuilder builder = new MarkLogicQuerySimpleBuilder(rangeElementIndexes);
        builder.eq(key, value);
        for (State state : findAll(builder.build(), KEY_ID, KEY_IS_PROXY, KEY_PROXY_TARGET_ID, KEY_PROXY_IDS)) {
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

    @Override
    public boolean queryKeyValuePresence(String key, String value, Set<String> ignored) {
        MarkLogicQuerySimpleBuilder builder = new MarkLogicQuerySimpleBuilder(rangeElementIndexes);
        builder.eq(key, value);
        builder.notIn(KEY_ID, ignored);
        return exist(builder.build());
    }

    @Override
    public PartialList<Map<String, Serializable>> queryAndFetch(DBSExpressionEvaluator evaluator,
            OrderByClause orderByClause, boolean distinctDocuments, int limit, int offset, int countUpTo) {
        MarkLogicQueryBuilder builder = new MarkLogicQueryBuilder(evaluator, orderByClause, distinctDocuments,
                rangeElementIndexes);
        MarkLogicQuery query = builder.buildQuery();
        // Don't do manual projection if there are no projection wildcards, as this brings no new
        // information and is costly. The only difference is several identical rows instead of one.
        boolean manualProjection = builder.doManualProjection();
        if (manualProjection) {
            // we'll do post-treatment to re-evaluate the query to get proper wildcard projections
            // so we need the full state from the database
            evaluator.parse();
        }
        String searchQuery = query.getSearchQuery(limit, offset);
        if (log.isTraceEnabled()) {
            logQuery(searchQuery);
        }
        // Run query
        try (Session session = xccContentSource.newSession()) {
            AdhocQuery request = session.newAdhocQuery(searchQuery);
            // ResultSequence will be closed by Session close
            ResultSequence rs = session.submitRequest(request);

            List<Map<String, Serializable>> projections = new ArrayList<>(limit);
            for (String rsItem : rs.asStrings()) {
                State state = MarkLogicStateDeserializer.deserialize(rsItem);
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
                    AdhocQuery countRequest = session.newAdhocQuery(query.getCountQuery());
                    // ResultSequence will be closed by Session close
                    ResultSequence countRs = session.submitRequest(countRequest);
                    totalSize = Long.parseLong(countRs.asStrings()[0]);
                }
            } else if (countUpTo == 0) {
                // no count
                totalSize = -1; // not counted
            } else {
                // count only if less than countUpTo
                if (limit == 0) {
                    totalSize = projections.size();
                } else {
                    AdhocQuery countRequest = session.newAdhocQuery(query.getCountQuery(countUpTo + 1));
                    // ResultSequence will be closed by Session close
                    ResultSequence countRs = session.submitRequest(countRequest);
                    totalSize = Long.parseLong(countRs.asStrings()[0]);
                }
                if (totalSize > countUpTo) {
                    totalSize = -2; // truncated
                }
            }

            if (log.isTraceEnabled() && projections.size() != 0) {
                log.trace("MarkLogic:    -> " + projections.size());
            }
            return new PartialList<>(projections, totalSize);
        } catch (RequestException e) {
            throw new NuxeoException("An exception happened during xcc call", e);
        }
    }

    @Override
    public ScrollResult scroll(DBSExpressionEvaluator evaluator, int batchSize, int keepAliveInSecond) {
        // Not yet implemented, return all result in one shot for now
        MarkLogicQueryBuilder builder = new MarkLogicQueryBuilder(evaluator, null, false, rangeElementIndexes);
        String query = builder.buildQuery().getSearchQuery();
        // Run query
        try (Session session = xccContentSource.newSession()) {
            AdhocQuery request = session.newAdhocQuery(query);
            // ResultSequence will be closed by Session close
            ResultSequence rs = session.submitRequest(request);
            return Arrays.stream(rs.asStrings())
                         .map(MarkLogicStateDeserializer::deserialize)
                         .map(state -> state.get(KEY_ID).toString())
                         .collect(Collectors.collectingAndThen(Collectors.toList(),
                                 ids -> new ScrollResultImpl(NOSCROLL_ID, ids)));
        } catch (RequestException e) {
            throw new NuxeoException("An exception happened during xcc call", e);
        }
    }

    @Override
    public ScrollResult scroll(String scrollId) {
        if (NOSCROLL_ID.equals(scrollId)) {
            // there is only one batch
            return emptyResult();
        }
        throw new NuxeoException("Unknown or timed out scrollId");
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
        State state = new State();
        state.put(KEY_LOCK_OWNER, lock.getOwner());
        state.put(KEY_LOCK_CREATED, lock.getCreated());
        String lockString = MarkLogicStateSerializer.serialize(state);
        if (log.isTraceEnabled()) {
            log.trace("MarkLogic: SETLOCK " + id + ": " + lockString);
        }
        try (Session session = xccContentSource.newSession()) {
            ModuleInvoke request = session.newModuleInvoke("/ext/nuxeo/set-lock.xqy");
            request.setNewStringVariable("uri", ID_FORMATTER.apply(id));
            request.setNewStringVariable("lock-string", lockString);
            // ResultSequence will be closed by Session close
            ResultSequence result = session.submitRequest(request);
            State resultState = MarkLogicStateDeserializer.deserialize(result.asString());
            return extractLock(resultState);
        } catch (RequestException e) {
            if ("Document not found".equals(e.getMessage())) {
                throw new DocumentNotFoundException(id, e);
            }
            throw new NuxeoException("An exception happened during xcc call", e);
        }
        // TODO check how the concurrent exception is raised
    }

    @Override
    public Lock removeLock(String id, String owner) {
        if (log.isTraceEnabled()) {
            log.trace("MarkLogic: REMOVELOCK " + id + ": " + owner);
        }
        try (Session session = xccContentSource.newSession()) {
            ModuleInvoke request = session.newModuleInvoke("/ext/nuxeo/remove-lock.xqy");
            request.setNewStringVariable("uri", ID_FORMATTER.apply(id));
            request.setNewStringVariable("owner", Strings.nullToEmpty(owner));
            // ResultSequence will be closed by Session close
            ResultSequence result = session.submitRequest(request);
            State resultState = MarkLogicStateDeserializer.deserialize(result.asString());
            return extractLock(resultState);
        } catch (RequestException e) {
            if ("Document not found".equals(e.getMessage())) {
                throw new DocumentNotFoundException(id, e);
            }
            throw new NuxeoException("An exception happened during xcc call", e);
        }
    }

    private Lock extractLock(State state) {
        if (state.isEmpty()) {
            return null;
        }
        String owner = (String) state.get(KEY_LOCK_OWNER);
        Calendar created = (Calendar) state.get(KEY_LOCK_CREATED);
        Boolean failed = (Boolean) state.get("failed");
        return new Lock(owner, created, Boolean.TRUE.equals(failed));
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

    private void logQuery(String query) {
        log.trace("MarkLogic: QUERY " + query);
    }

    private boolean exist(String ctsQuery) {
        // first build exist query from cts query
        String query = "xdmp:exists(" + ctsQuery + ")";
        if (log.isTraceEnabled()) {
            logQuery(query);
        }
        // Run query
        try (Session session = xccContentSource.newSession()) {
            AdhocQuery request = session.newAdhocQuery(query);
            // ResultSequence will be closed by Session close
            ResultSequence rs = session.submitRequest(request);
            return Boolean.parseBoolean(rs.asString());
        } catch (RequestException e) {
            throw new NuxeoException("An exception happened during xcc call", e);
        }
    }

    private State findOne(String ctsQuery) {
        // first add limit to ctsQuery
        String query = ctsQuery + "[1 to 1]";
        if (log.isTraceEnabled()) {
            logQuery(query);
        }
        // Run query
        try (Session session = xccContentSource.newSession()) {
            AdhocQuery request = session.newAdhocQuery(query);
            // ResultSequence will be closed by Session close
            ResultSequence rs = session.submitRequest(request);
            if (rs.hasNext()) {
                return MarkLogicStateDeserializer.deserialize(rs.asStrings()[0]);
            }
            return null;
        } catch (RequestException e) {
            throw new NuxeoException("An exception happened during xcc call", e);
        }
    }

    private List<State> findAll(String ctsQuery, String... selects) {
        String query = ctsQuery;
        if (selects.length > 0) {
            query = "for $i in " + query
                    + " return document {element document{$i/document/@*,$i/document/*[ fn:local-name(.) = ("
                    + Arrays.stream(selects)
                            .map(MarkLogicHelper::serializeKey)
                            .map(select -> "\"" + select + "\"")
                            .collect(Collectors.joining(","))
                    + ")]}}";
        }
        if (log.isTraceEnabled()) {
            logQuery(query);
        }
        // Run query
        try (Session session = xccContentSource.newSession()) {
            AdhocQuery request = session.newAdhocQuery(query);
            // ResultSequence will be closed by Session close
            ResultSequence rs = session.submitRequest(request);
            return Arrays.stream(rs.asStrings())
                         .map(MarkLogicStateDeserializer::deserialize)
                         .collect(Collectors.toList());
        } catch (RequestException e) {
            throw new NuxeoException("An exception happened during xcc call", e);
        }
    }

}
