/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.elasticsearch.api.ESClient;

import io.opencensus.common.Scope;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;

/**
 * @since 9.3
 */
public class ESRestClient implements ESClient {
    // TODO: add security sanitizer to make sure all parameters used to build requests are clean
    private static final Log log = LogFactory.getLog(ESRestClient.class);

    public static final String CREATE_INDEX_TIMEOUT = "60s";

    protected RestClient lowLevelClient;

    protected RestHighLevelClient client;

    public ESRestClient(RestClient lowLevelRestClient, RestHighLevelClient client) {
        this.lowLevelClient = lowLevelRestClient;
        this.client = client;
    }

    @Override
    public boolean waitForYellowStatus(String[] indexNames, int timeoutSecond) {
        ClusterHealthStatus healthStatus;
        Response response;
        try {
            response = performRequestWithTracing(
                    new Request("GET", String.format("/_cluster/health/%s?wait_for_status=yellow&timeout=%ds",
                            getIndexesAsString(indexNames), timeoutSecond)));
            try (InputStream is = response.getEntity().getContent()) {
                Map<String, Object> map = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
                healthStatus = ClusterHealthStatus.fromString((String) map.get("status"));
            }
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        switch (healthStatus) {
        case GREEN:
            log.info("Elasticsearch Cluster ready: " + response);
            return true;
        case YELLOW:
            log.warn("Elasticsearch Cluster ready but not GREEN: " + response);
            return false;
        default:
            String error = "Elasticsearch Cluster health status: " + healthStatus + ", not Yellow after "
                    + timeoutSecond + " give up: " + response;
            throw new IllegalStateException(error);
        }
    }

    protected String getIndexesAsString(String[] indexNames) {
        return indexNames == null ? "" : String.join(",", indexNames);
    }

    @Override
    public ClusterHealthStatus getHealthStatus(String[] indexNames) {
        Response response = performRequestWithTracing(
                new Request("GET", String.format("/_cluster/health/%s", getIndexesAsString(indexNames))));
        try (InputStream is = response.getEntity().getContent()) {
                Map<String, Object> map = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
                return ClusterHealthStatus.fromString((String) map.get("status"));
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void refresh(String indexName) {
        performRequestWithTracing(new Request("POST", "/" + indexName + "/_refresh"));
    }

    @Override
    public void flush(String indexName) {
        performRequestWithTracing(new Request("POST", "/" + indexName + "/_flush?wait_if_ongoing=true"));
    }

    @Override
    public void optimize(String indexName) {
        performRequestWithTracing(new Request("POST", "/" + indexName + "/_forcemerge?max_num_segments=1"));
    }

    @Override
    public boolean indexExists(String indexName) {
        Response response = performRequestWithTracing(new Request("HEAD", "/" + indexName));
        switch (response.getStatusLine().getStatusCode()) {
        case HttpStatus.SC_OK:
            return true;
        case HttpStatus.SC_NOT_FOUND:
            return false;
        default:
            throw new IllegalStateException(String.format("Checking index %s returns: %s", indexName, response));
        }
    }

    @Override
    public boolean mappingExists(String indexName, String type) {
        Response response = performRequestWithTracing(
                new Request("HEAD", String.format("/%s/_mapping/%s", indexName, type)));
        switch (response.getStatusLine().getStatusCode()) {
        case HttpStatus.SC_OK:
            return true;
        case HttpStatus.SC_NOT_FOUND:
            return false;
        default:
            throw new IllegalStateException(String.format("Checking mapping %s returns: %s", indexName, response));
        }
    }

    @Override
    public void deleteIndex(String indexName, int timeoutSecond) {
        Response response;
        try {
            response = lowLevelClient.performRequest(
                    new Request("DELETE", String.format("/%s?master_timeout=%ds", indexName, timeoutSecond)));
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("illegal_argument_exception")) {
                // when trying to delete an alias, throws the same exception than the transport client
                throw new IllegalArgumentException(e);
            }
            throw new NuxeoException(e);
        }
        int code = response.getStatusLine().getStatusCode();
        if (code != HttpStatus.SC_OK) {
            throw new IllegalStateException(String.format("Deleting %s returns: %s", indexName, response));
        }
    }

    @Override
    public void createIndex(String indexName, String jsonSettings) {
        Request request = new Request("PUT", "/" + indexName + "?timeout=" + CREATE_INDEX_TIMEOUT);
        request.setJsonEntity(jsonSettings);
        Response response = performRequestWithTracing(request);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new NuxeoException("Fail to create index: " + indexName + " :" + response);
        }
    }

    @Override
    public void createMapping(String indexName, String type, String jsonMapping) {
        Request request = new Request("PUT", String.format("/%s/%s/_mapping", indexName, type));
        request.setJsonEntity(jsonMapping);
        Response response = performRequestWithTracing(request);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new NuxeoException(String.format("Fail to create mapping on %s/%s: %s", indexName, type, response));
        }
    }

    protected Response performRequest(Request request) {
        try {
            return lowLevelClient.performRequest(request);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    protected Response performRequestWithTracing(Request request) {
        try (Scope ignored = getScopedSpan("elastic" + request.getEndpoint(), request.toString())) {
            return performRequest(request);
        }
    }

    @Override
    public String getNodesInfo() {
        Response response = performRequestWithTracing(new Request("GET", "/_nodes/_all"));
        try {
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public String getNodesStats() {
        Response response = performRequestWithTracing(new Request("GET", "/_nodes/stats"));
        try {
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public boolean aliasExists(String aliasName) {
        Response response = performRequestWithTracing(new Request("HEAD", String.format("/_alias/%s", aliasName)));
        switch (response.getStatusLine().getStatusCode()) {
        case HttpStatus.SC_OK:
            return true;
        case HttpStatus.SC_NOT_FOUND:
            return false;
        default:
            throw new IllegalStateException(String.format("Checking alias %s returns: %s", aliasName, response));
        }
    }

    @Override
    public String getFirstIndexForAlias(String aliasName) {
        if (!aliasExists(aliasName)) {
            return null;
        }
        Response response = performRequestWithTracing(new Request("GET", String.format("/_alias/%s", aliasName)));
        try (InputStream is = response.getEntity().getContent()) {
            Map<String, Object> map = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
            if (map.size() != 1) {
                throw new NuxeoException(String.format(
                        "Expecting alias that point to a single index, alias: %s, got: %s", aliasName, response));
            }
            return map.keySet().iterator().next();
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void updateAlias(String aliasName, String indexName) {
        // TODO do this in a single call to make it atomically
        if (aliasExists(aliasName)) {
            deleteAlias(aliasName);
        }
        if (indexExists(aliasName)) {
            throw new NuxeoException("Can create an alias because an index with the same name exists: " + aliasName);
        }
        createAlias(aliasName, indexName);
    }

    protected void deleteAlias(String aliasName) {
        Response response = performRequestWithTracing(
                new Request("DELETE", String.format("/_all/_alias/%s", aliasName)));
        int code = response.getStatusLine().getStatusCode();
        if (code != HttpStatus.SC_OK) {
            throw new IllegalStateException(String.format("Deleting %s alias: %s", aliasName, response));
        }
    }

    protected void createAlias(String aliasName, String indexName) {
        Response response = performRequestWithTracing(
                new Request("PUT", String.format("/%s/_alias/%s", indexName, aliasName)));
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new NuxeoException("Fail to create alias: " + indexName + " :" + response);
        }
    }

    @Override
    public BulkResponse bulk(BulkRequest request) {
        try (Scope ignored = getScopedSpan("elastic/_bulk", "actions: " + request.numberOfActions())) {
            return client.bulk(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public DeleteResponse delete(DeleteRequest request) {
        try {
            return client.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        try (Scope ignored = getScopedSpan("elastic/_search", request.toString())) {
            return client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public SearchResponse searchScroll(SearchScrollRequest request) {
        try (Scope ignored = getScopedSpan("elastic/_scroll", request.toString())) {
            return client.scroll(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public GetResponse get(GetRequest request) {
        try (Scope ignored = getScopedSpan("elastic/_get", request.toString())) {
            return client.get(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public IndexResponse index(IndexRequest request) {
        try (Scope ignored = getScopedSpan("elastic/_index", request.toString())) {
            return client.index(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException e) {
            if (RestStatus.CONFLICT.equals(e.status())) {
                throw new ConcurrentUpdateException(e);
            }
            throw new NuxeoException(e);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    protected Scope getScopedSpan(String name, String request) {
        Scope scope = Tracing.getTracer().spanBuilder(name).setSpanKind(Span.Kind.CLIENT).startScopedSpan();
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("thread", AttributeValue.stringAttributeValue(Thread.currentThread().getName()));
        map.put("request", AttributeValue.stringAttributeValue(request));
        Tracing.getTracer().getCurrentSpan().putAttributes(map);
        return scope;
    }

    @Override
    public ClearScrollResponse clearScroll(ClearScrollRequest request) {
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Clearing scroll ids: %s", Arrays.toString(request.getScrollIds().toArray())));
            }
            return client.clearScroll(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException e) {
            if (RestStatus.NOT_FOUND.equals(e.status())) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Scroll ids not found, they have certainly been already closed: %s",
                            Arrays.toString(request.getScrollIds().toArray())));
                }
                return new ClearScrollResponse(true, 0);
            }
            throw new NuxeoException(e);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public BulkProcessor.Builder bulkProcessorBuilder(BulkProcessor.Listener bulkListener) {
        return BulkProcessor.builder((request, listener) -> client.bulkAsync(request, RequestOptions.DEFAULT, listener),
                bulkListener);
    }

    @Override
    public void close() {
        if (lowLevelClient != null) {
            try {
                lowLevelClient.close();
            } catch (IOException e) {
                log.warn("Fail to close the Elasticsearch low level RestClient: " + e.getMessage(), e);
            }
            lowLevelClient = null;
        }
        client = null;
    }
}
