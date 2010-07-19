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
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.client.search.results.document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderAdapterService;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.common.TypeManagerServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.client.search.results.document.impl.ResultDocumentModel;
import org.nuxeo.ecm.core.search.api.client.search.results.impl.DocumentModelResultItem;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.ResourceType;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 * @deprecated use {@link CoreQueryDocumentPageProvider} instead
 */
@Deprecated
public class SearchPageProvider implements PagedDocumentsProvider {

    // to be used by the blob filter to transform maps into blob instances

    public static final String BLOB_DATA_KEY = "data";

    public static final String BLOB_URI_KEY = "uri";

    public static final String BLOB_ENCODING_KEY = "encoding";

    public static final String BLOB_MIMETYPE_KEY = "mime-type";

    public static final String BLOB_DIGEST_KEY = "digest";

    public static final String BLOB_NAME_KEY = "name";

    public static final String BLOB_LENGTH_KEY = "length";

    private static final long serialVersionUID = 4391326971391218440L;

    private static final Log log = LogFactory.getLog(SearchPageProvider.class);

    private static final DocumentModelList EMPTY = new DocumentModelListImpl();

    private static final Map<String, String> prefix2SchemaNameCache = new HashMap<String, String>();

    private ResultSet searchResults;

    private final String query;

    // fully qualified field name (prefix:name)-> schema name
    private transient Map<String, String> schemaCache;

    // will cache current page
    private DocumentModelList currentPageDocList;

    private transient SearchService service;

    private String providerName;

    private SortInfo sortInfo;

    private boolean sortable;

    private boolean pendingRefresh = false;

    // has the current page changed since last time it has been built
    private boolean pageChanged = false;

    private transient SchemaManager typeManager;

    /**
     * Constructor to create a sortable provider. Note that a provider can be
     * sortable and have a null sortInfo, which means a subsequent method call
     * with sortInfo not null will succeed.
     *
     * @param set the resultset
     * @param sortable if sortable, a subsequent call that provides sorting
     *            info
     * @param sortInfo the sorting info or null if the resultset is not sorted
     * @param query the query that produced this result. will succeed.
     */
    public SearchPageProvider(ResultSet set, boolean sortable,
            SortInfo sortInfo, String query) {
        searchResults = set;
        this.sortInfo = sortInfo;
        this.sortable = sortable;
        this.query = query;

        initSearchService();
        schemaCache = new HashMap<String, String>();
    }

    /**
     * Constructor to create a non-sortable resultset.
     *
     * @param set
     */
    public SearchPageProvider(ResultSet set) {
        this(set, false, null, null);
    }

    private void initSearchService() {
        if (service != null) {
            return;
        }
        try {
            service = SearchServiceDelegate.getRemoteSearchService();
        } catch (NullPointerException e) {
            service = null; // Happens in some unit tests
        }
    }

    public DocumentModelList getCurrentPage() {
        if (currentPageDocList != null) {
            return currentPageDocList;
        }

        try {
            // if page has changed, no need to refresh.
            if (pendingRefresh && !pageChanged) {
                performRefresh();
            }
            currentPageDocList = constructDocumentModels();
            return currentPageDocList;
        } catch (SearchException e) {
            log.error("Catched a SearchException", e);
            return EMPTY;
        }
    }

    public int getCurrentPageIndex() {
        int pag = searchResults.getPageNumber();
        // pag is 1 based
        // we need 0 based
        return pag - 1;
    }

    public String getCurrentPageStatus() {
        int total = getNumberOfPages();
        int current = getCurrentPageIndex() + 1;
        if (total == UNKNOWN_SIZE) {
            return String.format("%d", current);
        } else {
            return String.format("%d/%d", current, total);
        }
    }

    public DocumentModelList getNextPage() {
        next();
        return getCurrentPage();
    }

    public void goToPage(int page) {
        // 1 based
        page += 1;
        // TODO if the page is over the limit maybe go to the last page/ or
        // first
        try {
            ResultSet res = searchResults.goToPage(page);
            if (res == null) {
                return; // keep the same one to avoid NPEs
            }
            searchResults = res;
            // invalidate cache
            currentPageDocList = null;
            pageChanged = true;
        } catch (SearchException e) {
            log.error("getPage failed", e);
        }
    }

    public DocumentModelList getPage(int page) {
        goToPage(page);
        return getCurrentPage();
    }

    public long getResultsCount() {
        return searchResults.getTotalHits();
    }

    public boolean isNextPageAvailable() {
        return searchResults.hasNextPage();
    }

    public String getQuery() {
        return query;
    }

    public void last() {
        goToPage(getNumberOfPages() - 1);
    }

    public void next() {
        if (isNextPageAvailable()) {
            goToPage(getCurrentPageIndex() + 1);
        }
    }

    public void previous() {
        int i = getCurrentPageIndex();
        if (i > 0) {
            goToPage(i - 1);
        }
    }

    public void rewind() {
        goToPage(0);
    }

    public boolean isPreviousPageAvailable() {
        return getCurrentPageIndex() > 0;
    }

    public int getNumberOfPages() {
        int range = searchResults.getRange();
        if (range == 0) {
            return 1;
        }
        return (int) (1 + (getResultsCount() - 1) / range);
    }

    protected void performRefresh() throws SearchException {
        searchResults = searchResults.replay();
        pendingRefresh = false;
    }

    /**
     * Actual refresh will be next time the page is really needed. Better
     * suited for Seam/JSF (avoid useless multiple requests)
     */
    public void refresh() {
        pendingRefresh = true;
        currentPageDocList = null;
    }

    public int getCurrentPageOffset() {
        return searchResults.getOffset();
    }

    public int getCurrentPageSize() {
        return searchResults.getPageHits();
    }

    public int getPageSize() {
        return searchResults.getRange();
    }

    private String getSchemaNameForField(String key) {
        String schema = null;
        if (schemaCache == null) {
            schemaCache = new HashMap<String, String>();
        } else {
            schema = schemaCache.get(key);
        }

        if (schema != null) {
            return schema;
        }

        // TODO temporary see NXP-1202
        int cut = key.indexOf(':');
        if (cut == -1) {
            return null;
        }
        schema = getSchemaByPrefix(key.substring(0, cut));
        schemaCache.put(key, schema);
        return schema;
    }

    /**
     * Returns a schema name from its prefix.
     * <p>
     * Meant to be overridden in unit tests that can't lookup the search
     * service.
     * </p>
     * TODO This is wrong: prefix and schema name are actually transversal
     * concepts
     *
     * @param prefix
     * @return the schema name
     */
    protected String getSchemaByPrefix(String prefix) {

        // First do a cache lookup to avoid remote access
        if (prefix2SchemaNameCache.containsKey(prefix)) {
            return prefix2SchemaNameCache.get(prefix);
        }

        initSearchService();
        IndexableResourceConf conf = service.getIndexableResourceConfByPrefix(
                prefix, true);
        if (conf == null || !conf.getType().equals(ResourceType.SCHEMA)) {
            prefix2SchemaNameCache.put(prefix, null);
            return null;
        }

        String schemaName = conf.getName();
        prefix2SchemaNameCache.put(prefix, schemaName);
        return schemaName;
    }

    protected DocumentModelList constructDocumentModels() {
        if (searchResults == null) {
            return EMPTY;
        }
        int pageHits = searchResults.getPageHits();

        List<DocumentModel> res = new ArrayList<DocumentModel>(pageHits);
        for (int i = 0; i < pageHits; i++) {
            try {
                res.add(constructDocumentModel(searchResults.get(i)));
            } catch (SearchException e) {
                log.error("Could not convert result item in DocumentModel");
            }
        }
        pageChanged = false;
        return new DocumentModelListImpl(res);
    }

    @SuppressWarnings("unchecked")
    private DocumentModel constructDocumentModel(ResultItem rItem)
            throws SearchException {

        // try to recover DocumentModel
        if (rItem instanceof DocumentModelResultItem) {
            DocumentModel doc = ((DocumentModelResultItem) rItem).getDocumentModel();
            if (doc != null) {
                return doc;
            }
        }

        // Collector
        Map<String, Map<String, Object>> dataModels = new HashMap<String, Map<String, Object>>();

        // Fields extraction loop. Break cases are typically builtins.
        for (String key : rItem.keySet()) {

            int cut = key.indexOf(':');
            if (cut == -1) {
                continue;
            }

            String fName = key.substring(cut + 1);
            String schema = getSchemaNameForField(key);
            if (schema == null) {
                continue;
            }

            Map<String, Object> dm = dataModels.get(schema);
            if (dm == null) {
                dm = new HashMap<String, Object>();
                dataModels.put(schema, dm);
            }
            Object value = rItem.get(key);

            Field field = getSchemaField(schema, fName);
            dm.put(fName, blobFilter(value, field));
        }

        // ids should be unique, otherwise document comparations could fail
        DocumentRef docRef = (DocumentRef) rItem.get(BuiltinDocumentFields.FIELD_DOC_REF);
        if (docRef == null) {
            throw new SearchException("Document Ref is null");
        }
        String id = docRef.toString(); // XXX IdRef assumption

        Set<String> schemasSet = dataModels.keySet();
        String[] schemas = new String[schemasSet.size()];
        dataModels.keySet().toArray(schemas);

        // Deal with facets.

        List<String> facetsList = (List<String>) rItem.get(BuiltinDocumentFields.FIELD_DOC_FACETS);
        if (facetsList == null) {
            facetsList = Collections.emptyList();
        }

        Set<String> facets = new HashSet<String>(facetsList);
        facets.add("immutable");

        // BBB flags indexed since 5.1.3
        Long flags = 0L;
        try {
            flags = (Long) rItem.get(BuiltinDocumentFields.FIELD_DOC_FLAGS);
            if (flags == null) {
                flags = 0L;
            }
        } catch (ClassCastException cce) {
            log.warn("Wrong value for flags..." + flags);
        }

        String path = (String) rItem.get(BuiltinDocumentFields.FIELD_DOC_PATH);
        if (path == null) { // Root
            path = "/";
        }
        ResultDocumentModel docModel = new ResultDocumentModel(
                (String) rItem.get(BuiltinDocumentFields.FIELD_DOC_TYPE),
                id,
                new Path(path),
                docRef,
                (DocumentRef) rItem.get(BuiltinDocumentFields.FIELD_DOC_PARENT_REF),
                schemas,
                facets, // TODO test facets
                (String) rItem.get(BuiltinDocumentFields.FIELD_DOC_LIFE_CYCLE),
                (String) rItem.get(BuiltinDocumentFields.FIELD_DOC_VERSION_LABEL),
                (String) rItem.get(BuiltinDocumentFields.FIELD_DOC_REPOSITORY_NAME),
                flags);

        for (String s : dataModels.keySet()) {
            docModel.addDataModel(new DataModelImpl(s, dataModels.get(s)));
        }

        return docModel;
    }

    protected Field getSchemaField(String schemaName, String fieldName) {
        return getTypeManager().getSchema(schemaName).getField(fieldName);
    }

    /**
     * Gets the type manager from the platform service platform service.
     *
     * @return a type manager instance.
     */
    protected SchemaManager getTypeManager() {
        if (typeManager == null) {
            typeManager = TypeManagerServiceDelegate.getRemoteTypeManagerService();
        }
        return typeManager;
    }

    /**
     * Introspect typed value and create Blob instances instead of Maps when
     * appropriate
     *
     * @param value raw value as returned by the search service backend
     * @param field Field instance of the matching core Schema
     * @return the filter Object with Blob instances instead of Map instances
     *         when required
     */
    @SuppressWarnings("unchecked")
    private static Object blobFilter(Object value, Field field)
            throws SearchException {

        // optim: no need to introspect field structure for empty fields
        if (value == null || field == null) {
            return value;
        }

        Type fieldType = field.getType();
        if (fieldType instanceof ComplexType && value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            if (TypeConstants.isContentType(fieldType)) {
                String data = (String) map.get(BLOB_DATA_KEY);
                String mimetype = (String) map.get(BLOB_MIMETYPE_KEY);
                String digest = (String) map.get(BLOB_DIGEST_KEY);
                String name = (String) map.get(BLOB_NAME_KEY);
                String lengthS = (String) map.get(BLOB_LENGTH_KEY);
                final int length = lengthS == null ? 0
                        : Integer.parseInt(lengthS);
                if (mimetype == null) {
                    mimetype = "application/octet-stream";
                }
                ExtendedStringSource src = new ExtendedStringSource(
                        data == null ? "" : data, length);
                Blob blob = new StreamingBlob(src, mimetype);
                blob.setEncoding((String) map.get(BLOB_ENCODING_KEY));
                blob.setDigest(digest == null ? "" : digest);
                blob.setFilename(name == null ? "" : name);
                return blob;
            } else if (TypeConstants.isExternalContentType(fieldType)) {
                String uri = (String) map.get(BLOB_URI_KEY);
                String mimetype = (String) map.get(BLOB_MIMETYPE_KEY);
                String digest = (String) map.get(BLOB_DIGEST_KEY);
                String name = (String) map.get(BLOB_NAME_KEY);
                if (mimetype == null) {
                    mimetype = "application/octet-stream";
                }
                try {
                    BlobHolderAdapterService service = Framework.getService(BlobHolderAdapterService.class);
                    if (service == null) {
                        throw new SearchException(
                                "BlobHolderAdapterService not found");
                    }
                    Blob blob = service.getExternalBlobForUri(uri);
                    blob.setEncoding((String) map.get(BLOB_ENCODING_KEY));
                    blob.setDigest(digest == null ? "" : digest);
                    blob.setFilename(name == null ? "" : name);
                    return blob;
                } catch (Exception e) {
                    throw new SearchException(e);
                }
            } else {
                // not a blob, just a regular complex type, check the content
                // recursively

                ComplexType cFieldType = (ComplexType) fieldType;
                Map<String, Object> filteredMap = new HashMap<String, Object>();
                for (String key : map.keySet()) {
                    filteredMap.put(key, blobFilter(map.get(key),
                            cFieldType.getField(key)));
                }
                return filteredMap;
            }
        } else if (fieldType instanceof ListType && value instanceof Collection) {
            ListType lFieldType = (ListType) fieldType;
            Collection<Object> values = (Collection<Object>) value;
            List<Object> blobFilteredValues = new ArrayList<Object>(
                    values.size());
            for (Object subValue : values) {
                // recursively blob filter the contained objects
                blobFilteredValues.add(blobFilter(subValue,
                        lFieldType.getField()));
            }
            // XXX: shouldn't we return a List instead?
            return blobFilteredValues.toArray();
        } else {
            // SimpleType or builtin type
            return value;
        }
    }

    public SortInfo getSortInfo() {
        return sortInfo;
    }

    public boolean isSortable() {
        return sortable;
    }

    public void setSortInfo(SortInfo sortInfo) {
        this.sortInfo = sortInfo;
    }

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    public String getName() {
        return providerName;
    }

    public void setName(String name) {
        providerName = name;
    }

}
