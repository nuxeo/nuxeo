/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: CompassBackend.java 29926 2008-02-06 18:56:29Z tdelprat $
 */

package org.nuxeo.ecm.core.search.backend.compass;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.compass.core.Compass;
import org.compass.core.CompassDetachedHits;
import org.compass.core.CompassException;
import org.compass.core.CompassHits;
import org.compass.core.CompassQuery;
import org.compass.core.CompassSession;
import org.compass.core.CompassTransaction;
import org.compass.core.Property;
import org.compass.core.Resource;
import org.compass.core.config.CompassConfiguration;
import org.compass.core.engine.SearchEngineQueryParseException;
import org.compass.core.lucene.util.LuceneHelper;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.search.NXSearch;
import org.nuxeo.ecm.core.search.api.backend.impl.AbstractSearchEngineBackend;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.backend.security.SecurityFiltering;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.indexing.nxcore.IndexingThread;
import org.nuxeo.ecm.core.search.api.client.indexing.session.SearchServiceSession;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQuery;
import org.nuxeo.ecm.core.search.api.client.query.NativeQueryString;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.client.search.results.impl.ResultItemImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.impl.ResultSetImpl;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceDataConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.ResourceType;
import org.nuxeo.ecm.core.search.api.internals.SearchServiceInternals;
import org.nuxeo.ecm.core.search.backend.compass.connection.ConnectionConf;
import org.nuxeo.ecm.core.search.session.SearchServiceSessionImpl;
import org.nuxeo.ecm.core.search.transaction.Transactions;
import org.nuxeo.runtime.model.ComponentInstance;

/**
 * Compass search engine backend implementation.
 *
 * @author <a href="mailto:gr@nuxeo.com">Georges Racinet</a
 *
 */
public class CompassBackend extends AbstractSearchEngineBackend {

    private static final long serialVersionUID = -2101828120168725548L;

    private static final Log log = LogFactory.getLog(CompassBackend.class);

    private static final int BATCH_SIZE_MARGIN = 10;

    private static List<String> CACHED_BROWSE_PERMISSIONS;

    /*
     * TODO Temporary harcoded stuff that has to become dynamic
     */
    private static final String DEFAULT_ALIAS = "nxdoc";

    // TODO Relation specifics that shouldn't be harcoded
    private static final String RELATION_RESOURCE_TYPE = "relations";

    private static final String RELATION_ALIAS_PREFIX = "nxrel-";

    private static final String AGGREGATE_ID = "aggregate_id";

    private static final Object PT_CONNECTION = "connection";

    private static final Lock optimizerLock = new ReentrantLock();

    private static final int OPTIMIZER_SAVE_INTERVAL = 20;

    private static int optimize_try = 0;

    // Default compass session
    protected Compass compass;

    protected SearchServiceInternals searchService;

    protected ConnectionConf connectionConf;

    // @SuppressWarnings("unchecked")
    // protected final Map sessions = new ReferenceMap();
    protected final Map<String, CompassBackendSession> sessions = new ConcurrentHashMap<String, CompassBackendSession>();

    // :XXX: move this to Nuxeo runtime to be sure it's loaded before any
    // modules that may initialze Lucene before this one. Though, we need to
    // think how to define such facillity there.
    static {
        // Override the default Lucene SegmentReader as requested by Compass
        System.setProperty("org.apache.lucene.SegmentReader.class",
                "org.apache.lucene.index.CompassSegmentReader");
    }

    /*
     * Should probably be done by base class, but NXSearch cannot be
     * instantiated from there because it's with API
     */
    public CompassBackend() {
        initSearchService();
    }

    public CompassBackend(String name) {
        super(name);
        initSearchService();
    }

    public CompassBackend(String name, String configurationFileName) {
        super(name, configurationFileName);
        initSearchService();
    }

    public void initSearchService() {
        searchService = (SearchServiceInternals) NXSearch.getSearchService();
    }

    /**
     * Builds the shared thread-safe compass object using the standard
     * configuration file compass.cfg.xml.
     *
     * @return shared thread-safe compass object
     */
    protected Compass getCompass() {
        if (compass == null) {
            compass = createCompass();
        }

        return compass;
    }

    /**
     * The main Resource builder, called by index(). Can use a given builder to
     * add properties or start with a fresh one which as the effect to make a
     * separate resource.
     *
     * @param session
     * @param builder
     * @param iResource
     * @param commonData
     * @param joinIdName
     * @param joinIdValue
     * @param acp ACP of the indexed document
     * @return
     * @throws CompassException
     * @throws IndexingException
     */
    protected static Resource buildResource(CompassSession session,
            ResourceBuilder builder, ResolvedResource iResource,
            List<ResolvedData> commonData, String joinIdName,
            String joinIdValue, ACP acp) throws IndexingException {

        if (builder == null) {
            builder = new ResourceBuilder(session, getAlias(iResource),
                    iResource.getId());
        }

        // Put join id property
        if (joinIdName != null) {
            // TODO handle potential conflict in naming:
            // investigate what the 'internal' flag in compass means
            builder.addProperty(joinIdName, joinIdValue, "keyword", true, true,
                    false, false, new HashMap<String, Serializable>(), null);
        }

        String prefix = "";
        // should become applicable to all resources
        IndexableResourceConf conf = iResource.getConfiguration();
        if (conf.getType().equals(ResourceType.SCHEMA)) {
            prefix = conf.getPrefix() + ':';
        }

        // Data properties
        for (ResolvedData iData : iResource.getIndexableData()) {
            builder.addProperty(prefix + iData.getName(), iData);
        }
        // Common Data
        for (ResolvedData iData : commonData) {
            builder.addProperty(iData);
        }
        if (acp != null) {
            // index
            try {
                builder.addSecurityProperty(
                        BuiltinDocumentFields.FIELD_ACP_INDEXED,
                        getBrowsePermissions(), acp);
            } catch (Exception e) {
                throw new IndexingException("error building indexable ACP: "
                        + e.getMessage(), e);
            }
            // store
            builder.addProperty(BuiltinDocumentFields.FIELD_ACP_STORED, acp,
                    null, false, true, false, false,
                    new HashMap<String, Serializable>(), null);
        }
        return builder.toResource();
    }

    /**
     * TODO change this to an extension point. For example we can't even use
     * constants her since we don't want to introduce dependencies to, e.g,
     * nuxeo-platform-relations-search
     *
     * @param resource
     * @return
     */
    public static String getAlias(ResolvedResource resource) {
        IndexableResourceConf conf = resource.getConfiguration();
        String type = conf.getType();
        if (ResourceType.SCHEMA.equals(type)) {
            return DEFAULT_ALIAS;
        }
        String name = conf.getName();
        if (RELATION_RESOURCE_TYPE.equals(type)) {
            return RELATION_ALIAS_PREFIX + name;
        }
        return name;
    }

    protected void index(ResolvedResources resources,
            CompassBackendSession session, boolean userTxn)
            throws IndexingException {

        try {
            ResourceBuilder schemasBuilder = null;
            String aggId = resources.getId();
            List<ResolvedData> common = resources.getCommonIndexableData();
            List<ResolvedResource> iResources = resources.getIndexableResolvedResources();
            if (iResources != null) {
                for (ResolvedResource iResource : iResources) {
                    // XXX shouldn't be relying on this here => make this
                    // more generic conf side.
                    if (!iResource.getConfiguration().getType().equals(
                            ResourceType.SCHEMA)) {
                        session.add(buildResource(session.getCompassSession(),
                                null, iResource, common, AGGREGATE_ID, aggId,
                                resources.getACP()));
                    } else { // first time we add ACP and common
                        // resources
                        if (schemasBuilder == null) {
                            schemasBuilder = new ResourceBuilder(
                                    session.getCompassSession(), DEFAULT_ALIAS,
                                    aggId);
                            buildResource(session.getCompassSession(),
                                    schemasBuilder, iResource, common,
                                    AGGREGATE_ID, aggId, resources.getACP());
                        } else {
                            buildResource(session.getCompassSession(),
                                    schemasBuilder, iResource,
                                    Collections.EMPTY_LIST, AGGREGATE_ID,
                                    aggId, null);
                        }
                    }
                }
            }
            if (schemasBuilder != null) {
                session.add(schemasBuilder.toResource());
            }
            if (!userTxn || mustCommitNow(session.countWaitingResources())) {
                session.saveAndCommit(userTxn);
                if (userTxn){
                    doOptimize();
                    markForRecycling();
                }
            }
        } catch (CompassException ce) {
            session.rollback();
            throw new IndexingException(ce);
        }
    }

    private void markForRecycling() {
        Thread thread = Thread.currentThread();
        if (thread instanceof IndexingThread) {
            IndexingThread idxThread = (IndexingThread) thread;
            idxThread.markForRecycle();
        }
    }

    /**
     * Creates Compass resources from Search Service's resources.
     * <p>
     * All schema resources get merged as a single Compass Resource and gets the
     * joining id as Compass id.
     * <p>
     * Each resource of a different kind gives rise to a Compass Resource, its
     * own id being used as Compass id.
     * <p>
     * It's quite possible that Compass could do a better join of handling joins
     * etc by its own concept of MultiResource.
     */
    @SuppressWarnings("unchecked")
    public void index(ResolvedResources resources) throws IndexingException {


        boolean activeTxn = false;
        boolean userTxn;
        CompassBackendSession session;

        try {
            activeTxn = Transactions.isTransactionActiveOrMarkedRollback();
        } catch (Exception e) {
            throw new IndexingException(e);
        }

        if (isBoundToIndexingThread()) {

            // AYNCHRONOUS INDEXING
            userTxn = true;
            IndexingThread thread = (IndexingThread) Thread.currentThread();
            String sid = null;
            try {
                sid = thread.getSearchServiceSession().getSessionId();
            } catch (Exception e) {
                throw new IndexingException(e);
            }

            session = sessions.get(sid);

            if (session == null) {
                throw new IndexingException(
                        "CompassBackend session is null for thread "
                                + thread.toString());
            }

            try {
                if (!activeTxn) {
                    Transactions.getUserTransaction().begin();
                }
                session.begin(getCompass());
            } catch (Exception e) {
                throw new IndexingException(e);
            }
        } else {

            // CASE HERE FOR SYNCHRONOUS SEARCH
            userTxn = false;
            session = new CompassBackendSession();

            // XXX check if safe to share the same compass object. Maybe
            // checking if we could set 2 objects : one for the sync index and
            // the other one for the search queries.
            session.begin(getCompass());
        }

        try {
            index(resources, session, userTxn);
        } finally {
            // Nothing to do
        }
    }

    private void doOptimize() {
        if (optimizerLock.tryLock()) {
            try {
                optimize_try += 1;
                if ((optimize_try >= OPTIMIZER_SAVE_INTERVAL) && (getCompass().getSearchEngineOptimizer().needOptimization())) {
                    optimize_try=0;
                    log.debug("Running optimizer");
                    getCompass().getSearchEngineOptimizer().optimize();
                    log.debug("Optimizer ended");
                }
            } finally {
                optimizerLock.unlock();
            }
        }
    }

    public void deleteAggregatedResources(String key) throws IndexingException {
        CompassSession session = getCompass().openSession();
        CompassTransaction tx = null;
        try {
            tx = session.beginTransaction();
            TermQuery lQuery = new TermQuery(new Term(AGGREGATE_ID, key));
            session.delete(LuceneHelper.createCompassQuery(session, lQuery));
            tx.commit();
        } catch (CompassException ce) {
            if (tx != null) {
                tx.rollback();
            }
            throw new IndexingException(ce.getMessage(), ce);
        } finally {
            session.close();
        }
    }

    /*
     * Implementation details: we have to delete all resources, since Compass
     * doesn't seem to expose a one shot clearing API.
     */
    public void clear() throws IndexingException {
        CompassSession session = getCompass().openSession();
        CompassTransaction tx = null;
        try {
            tx = session.beginTransaction();
            session.delete(session.queryBuilder().matchAll());
            tx.commit();
        } catch (CompassException ce) {
            if (tx != null) {
                tx.rollback();
            }
            throw new IndexingException(ce.getMessage(), ce);
        } finally {
            session.close();
        }
    }

    public ResultSet searchQuery(NativeQueryString queryString, int offset,
            int range) throws SearchException, QueryException {
        return searchQuery(queryString.getQuery(),
                queryString.getSearchPrincipal(), offset, range);
    }

    private ResultSet searchQuery(String query, SearchPrincipal principal,
            int offset, int range) throws SearchException, QueryException {
        CompassSession session = getCompass().openSession();
        CompassTransaction tx = null;
        try {
            tx = session.beginTransaction();
            CompassQuery cQuery = new QueryConverter(session, searchService).toCompassQuery(
                    query, principal);

            // FIXME
            return buildResultSet(cQuery.hits(), offset, range, null, principal);

            // return buildResultSet(cQuery.hits(), offset, range,
            // new CompassNativeQuery(query, name, principal), principal);
        } catch (SearchEngineQueryParseException qe) {
            if (tx != null) {
                tx.rollback();
            }
            throw new QueryException(qe.getMessage(), qe);
        } catch (CompassException ce) {
            if (tx != null) {
                tx.rollback();
            }
            throw new SearchException(ce.getMessage(), ce);
        } finally {
            session.close();
        }
    }

    public ResultSet searchQuery(ComposedNXQuery cQuery, int offset, int range)
            throws SearchException, QueryException {
        return searchQuery(cQuery.getQuery(), cQuery.getSearchPrincipal(),
                offset, range);
    }

    public ResultSet searchQuery(SQLQuery query, SearchPrincipal principal,
            int offset, int range) throws SearchException, QueryException {
        CompassSession session = getCompass().openSession();
        CompassTransaction tx = null;
        ResultSet res;
        try {
            tx = session.beginTransaction();
            QueryConverter converter = new QueryConverter(session,
                    searchService);
            res = buildResultSet(
                    converter.toCompassQuery(query, principal).hits(), offset,
                    range, query, principal);
        } catch (SearchEngineQueryParseException qe) {
            if (tx != null) {
                tx.rollback();
            }
            throw new QueryException(qe.getMessage(), qe);
        } catch (CompassException ce) {
            if (tx != null) {
                tx.rollback();
            }
            throw new SearchException(ce.getMessage(), ce);
        } finally {
            session.close();
        }
        return res;
    }

    public ResultSet searchQuery(CompassNativeQuery cnQuery, int offset,
            int range) throws SearchException, QueryException {
        SearchPrincipal principal = cnQuery.getSearchPrincipal();
        if (cnQuery.isNxql()) {
            return searchQuery((SQLQuery) cnQuery.getQuery(), principal,
                    offset, range);
        }
        return searchQuery((String) cnQuery.getQuery(), principal, offset,
                range);
    }

    /**
     * Extracts a property from the result resource.
     * <p>
     * For multiple properties, this will be actually one of the elements. Same
     * for complex properties.
     * <p>
     * For non string properties, the implementation tries first to get the
     * value through Compass and else falls back to java deserialization.
     */
    protected static Serializable extractAtomicProperty(Property prop,
            String sValue, IndexableResourceDataConf dataConf)
            throws SearchException {

        if (Util.NULL_MARKER.equals(sValue)) {
            return null;
        }

        // Use converter
        Serializable oValue;
        try {
            oValue = (Serializable) prop.getObjectValue();
        } catch (Exception e) {
            oValue = null;
        }
        // String might have been application of default converter
        // null could come from converter lookup failure
        if (oValue != null && !(oValue instanceof String)) {
            return oValue;
        }

        // Try and use dataConf to convert Data
        if (dataConf != null) {
            String type = dataConf.getIndexingType().toLowerCase();

            if (type.equals("keyword")) {
                return Util.unescapeSpecialMarkers(sValue);
            }
            if (type.equals("text") || type.equals("path")) {
                return sValue;
            }
            if (type.equals("boolean")) {
                return Boolean.valueOf(sValue);
            }
        }

        // Fallback to generic serialization
        try {
            byte[] buf = new sun.misc.BASE64Decoder().decodeBuffer(sValue);
            ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(buf));
            return (Serializable) ois.readObject();
        } catch (IOException e) {
            log.warn(String.format(
                    "While building ResultItem, could not handle contents of stored "
                            + "field %s. "
                            + "Check Search Service configuration and Compass mappings.",
                    prop.getName()));
        } catch (ClassNotFoundException e) {
            throw new SearchException(String.format(
                    "Failed to mount stored field %s on result item.",
                    prop.getName()), e);
        }
        return null;
    }

    /**
     * Converts a Resource, typically from the Compass Results to a
     * DocumentResultItem.
     *
     * @param r Input Resource
     * @return A DocumentResultItem
     * @throws SearchException
     */
    protected ResultItem buildResultItem(Resource r) throws SearchException {
        Property idProp = r.getIdProperty();
        String idName = idProp.getName();
        String idValue = idProp.getStringValue();

        ResultItemImpl res = new ResultItemImpl(null, idValue);

        for (Property prop : r.getProperties()) {
            String propName = prop.getName();
            String resultName = propName;

            String[] split = propName.split(":");
            boolean isComplex = split.length > 2;

            // discarding backend specific technical fields
            if (propName.equals(idName) || propName.equals("alias")
                    || propName.equals(AGGREGATE_ID)) {
                continue;
            }

            // treat the case of null early. In particular, for lists, we want
            // null, and not [null]
            String sValue = prop.getStringValue();
            if (Util.NULL_MARKER.equals(sValue) && !isComplex) {
                res.put(propName, null);
                continue;
            }

            // builtins that can be handled directly and don't
            // have to go through serialization
            // These should not depend on data conf
            if (propName.equals(BuiltinDocumentFields.FIELD_DOC_PATH)
                    || propName.equals(BuiltinDocumentFields.FIELD_DOC_TYPE)
                    || propName.equals(BuiltinDocumentFields.FIELD_DOC_URL)
                    || propName.equals(BuiltinDocumentFields.FIELD_DOC_LIFE_CYCLE)
                    || propName.equals(BuiltinDocumentFields.FIELD_DOC_REPOSITORY_NAME)
                    || propName.equals(BuiltinDocumentFields.FIELD_DOC_VERSION_LABEL)) {
                res.put(propName, prop.getStringValue());
                continue;
            }

            if (propName.equals(BuiltinDocumentFields.FIELD_DOC_REF)
                    || propName.equals(BuiltinDocumentFields.FIELD_DOC_PARENT_REF)) {
                String v = prop.getStringValue();
                DocumentRef ref = null;
                if (v.charAt(0) == 'i') {
                    ref = new IdRef(v.substring(1));
                } else if (v.charAt(0) == 'p') {
                    ref = new PathRef(v.substring(1));
                }
                res.put(propName, ref);
                continue;
            }

            IndexableResourceDataConf dataConf = searchService.getIndexableDataConfFor(propName);

            if (isComplex) {
                propName = split[0] + ':' + split[1];
                resultName = split[2];
            }

            Serializable value = extractAtomicProperty(prop, sValue, dataConf);

            Map<String, Serializable> map = res;
            if (isComplex) {
                if (dataConf == null || !dataConf.isMultiple()) {
                    map = (Map<String, Serializable>) res.get(propName);
                    if (map == null) {
                        map = new HashMap<String, Serializable>();
                        res.put(propName, (Serializable) map);
                    }
                } else {
                    List<Map<String, Serializable>> cmpl = (List<Map<String, Serializable>>) res.get(propName);
                    if (cmpl == null) {
                        cmpl = new ArrayList<Map<String, Serializable>>();
                        res.put(propName, (Serializable) cmpl);
                    }

                    // find the first that doesn't have said entry or
                    // make a new one
                    map = null;
                    for (Map<String, Serializable> mapit : cmpl) {
                        if (!mapit.containsKey(resultName)) {
                            map = mapit;
                            break;
                        }
                    }
                    if (map == null) {
                        map = new HashMap<String, Serializable>();
                        cmpl.add(map);
                    }
                }
            }

            if (value == null || dataConf == null || isComplex
                    || !dataConf.isMultiple()) {
                map.put(resultName, value);
            } else { // Multiple properties support (non complex)
                // TODO should probably be Set. Don't suppress warning yet.
                List<Serializable> l = (List<Serializable>) res.get(resultName);
                if (value.equals(Util.EMPTY_MARKER)) {
                    res.put(resultName, (Serializable) Collections.EMPTY_LIST);
                } else if (l != null) {
                    l.add(value);
                } else {
                    l = new LinkedList<Serializable>();
                    l.add(value);
                    res.put(resultName, (Serializable) l);
                }
            }
        }
        return res;
    }

    protected ResultSet buildResultSet(CompassHits compassHits, int offset,
            int range, SQLQuery nxqlQuery, SearchPrincipal principal)
            throws SearchException {

        int total = compassHits.length();
        int resNb = Math.min(Math.max(total - offset, 0), range);
        ResultItem[] resItems = new ResultItem[resNb];

        if (resNb != 0) {
            CompassDetachedHits detached = compassHits.detach(offset, range);
            // TODO check iterator variants
            Resource[] resources = detached.getResources();

            for (int i = 0; i < resNb; i++) {
                resItems[i] = buildResultItem(resources[i]);
            }
        }
        return new ResultSetImpl(nxqlQuery, getName(), principal, offset,
                range, Arrays.asList(resItems), total, resNb);
    }

    public ResultSet searchQuery(NativeQuery nativeQuery, int offset, int range)
            throws SearchException, QueryException {

        if (nativeQuery instanceof CompassNativeQuery) {
            return searchQuery((CompassNativeQuery) nativeQuery, offset, range);
        }

        // Handle case of Lucene Query
        Serializable query = nativeQuery.getQuery();
        if (!(query instanceof org.apache.lucene.search.Query)) {
            throw new SearchException("Unknown native query type");
        }

        CompassSession session = getCompass().openSession();
        CompassTransaction tx = null;
        ResultSet res = null;
        try {
            tx = session.beginTransaction();
            CompassQuery cQuery = LuceneHelper.createCompassQuery(session,
                    (org.apache.lucene.search.Query) query);
            // FIXME
            res = buildResultSet(cQuery.hits(), offset, range, null,
                    nativeQuery.getSearchPrincipal());
        } catch (CompassException ce) {
            if (tx != null) {
                tx.rollback();
            }
            throw new SearchException(ce.getMessage(), ce);
        } finally {
            session.close();
        }
        return res;
    }

    public void deleteAtomicResource(String key) throws IndexingException {
        CompassSession session = getCompass().openSession();
        CompassTransaction tx = null;
        try {
            tx = session.beginTransaction();
            session.delete(DEFAULT_ALIAS, key);
            tx.commit();
        } catch (CompassException ce) {
            if (tx != null) {
                tx.rollback();
            }
            throw new IndexingException(ce.getMessage(), ce);
        } finally {
            session.close();
        }
    }

    protected NativeQuery convertToNativeQuery(ComposedNXQuery query) {
        return new CompassNativeQuery(query.getQuery(), name,
                query.getSearchPrincipal());
    }

    protected String getConnectionString() {
        if (connectionConf == null) {
            return null;
        }
        return connectionConf.getConnectionString();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        if (extensionPoint.equals(PT_CONNECTION)) {
            connectionConf = (ConnectionConf) contribution;
        }
    }

    public synchronized void closeSession(String sid) {
        sessions.remove(sid);
    }

    private synchronized CompassBackendSession createSession(String sid) {
        CompassBackendSession s = new CompassBackendSession(sid);
        sessions.put(sid, s);
        return s;
    }

    @SuppressWarnings("unchecked")
    public SearchServiceSession createSession() {
        SearchServiceSession s = new SearchServiceSessionImpl();
        return createSession(s.getSessionId());
    }

    protected Compass createCompass() {
        // XXX check if here would be a good place to begin UT transactions.s
        CompassConfiguration conf = new CompassConfiguration();
        if (configurationFileName == null) {
            conf.configure();
        } else {
            conf.configure(configurationFileName);
        }
        String cnx = getConnectionString();
        if (cnx != null) {
            conf.setConnection(cnx);
        }
        // Hack for IndexManager to not be created to avoid thread leak
        // conf.getSettings().setFloatSetting(LuceneEnvironment.SearchEngineIndex.INDEX_MANAGER_SCHEDULE_INTERVAL,
        // -1f);

        return conf.buildCompass();
    }

    protected static boolean isBoundToIndexingThread() {
        return Thread.currentThread() instanceof IndexingThread;
    }

    protected int getIndexingDocBatchSize() {
        return searchService.getIndexingDocBatchSize();
    }

    /**
     * Save one compass session.
     *
     * @param cs
     * @throws IndexingException
     */
    protected void saveSession(CompassBackendSession cs)
            throws IndexingException {

        try {
            boolean activeTxn = Transactions.isTransactionActiveOrMarkedRollback();
            boolean userTxn = false;
            if (!activeTxn) {
                Transactions.getUserTransaction().begin();
                userTxn = true;
            } else if (activeTxn && isBoundToIndexingThread()) {
                userTxn = true;
            }

            if (cs.isSessionOpened() && cs.isTransactionStarted()) {
                cs.clean();
                cs.begin(createCompass());
            }

            cs.saveAndCommit(userTxn);
        } catch (CompassException ce) {
            throw new IndexingException(ce);
        } catch (Exception e) {
            throw new IndexingException(e);
        }
    }

    protected boolean mustCommitNow(int queuedNonComitedResources) {
        int configuredSize = getIndexingDocBatchSize();

        // max batch size reached => comit
        if (queuedNonComitedResources >= configuredSize) {
            return true;
        }

        long nbThreads = searchService.getNumberOfIndexingThreads();

        if (nbThreads == 0) {
            log.debug("reducing batch size to " + queuedNonComitedResources);
            return true;
        }

        long queueSize = searchService.getIndexingWaitingQueueSize();

        long maxLeft = queueSize / nbThreads;
        if (maxLeft == 0) {
            log.debug("reducing batch size to " + queuedNonComitedResources);
            return true;
        }

        // theoritical batch size
        long idealBatchSize = queuedNonComitedResources + maxLeft;
        // compute margin
        long margin = idealBatchSize - configuredSize;

        // if not enought margin then do the commit right now
        if (margin < BATCH_SIZE_MARGIN) {
            log.debug("reducing batch size to " + queuedNonComitedResources);
            return true;
        }
        return false;
    }

    public void saveAllSessions() throws IndexingException {
        for (Object each : sessions.values()) {
            saveSession((CompassBackendSession) each);
        }
    }

    // TODO: this cached list should be invalidated upon runtime registration of
    // new permissions
    private static List<String> getBrowsePermissions() throws Exception {
        if (CACHED_BROWSE_PERMISSIONS == null) {
            CACHED_BROWSE_PERMISSIONS = SecurityFiltering.getBrowsePermissionList();
        }
        return CACHED_BROWSE_PERMISSIONS;
    }
}
