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
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.bulk.BulkShardRequest;
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
import org.elasticsearch.action.support.replication.ReplicationRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.elasticsearch.api.ESClient;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

/**
 * @since 9.3
 */
public class ESRestClient implements ESClient {
    // TODO: add security sanitizer to make sure all parameters used to build requests are clean
    private static final Log log = LogFactory.getLog(ESRestClient.class);

    // @since 11.5
    public static final String LONG_TIMEOUT = "120s";

    /** @deprecated use {@link #LONG_TIMEOUT} instead */
    public static final String CREATE_INDEX_TIMEOUT = LONG_TIMEOUT;

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
            response = performRequest(
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
        Response response = performRequest(
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
        performRequest(new Request("POST", "/" + indexName + "/_refresh"));
    }

    @Override
    public void flush(String indexName) {
        performRequest(new Request("POST", "/" + indexName + "/_flush?wait_if_ongoing=true"));
    }

    @Override
    public void optimize(String indexName) {
        performRequest(new Request("POST", "/" + indexName + "/_forcemerge?max_num_segments=1"));
    }

    @Override
    public boolean indexExists(String indexName) {
        Response response = performRequest(new Request("HEAD", "/" + indexName));
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
        Response response = performRequest(new Request("HEAD", String.format("/%s/_mapping/%s", indexName, type)));
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
                    new Request("DELETE", String.format("/%s?master_timeout=%ds&timeout=%ds", indexName, timeoutSecond, timeoutSecond)));
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
        Request request = new Request("PUT", "/" + indexName + "?master_timeout=" + LONG_TIMEOUT + "&timeout=" + LONG_TIMEOUT);
        request.setJsonEntity(jsonSettings);
        Response response = performRequest(request);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new NuxeoException("Fail to create index: " + indexName + " :" + response);
        }
    }

    @Override
    public void createMapping(String indexName, String type, String jsonMapping) {
        Request request = new Request("PUT", String.format("/%s/%s/_mapping", indexName, type));
        request.setJsonEntity(jsonMapping);
        Response response = performRequest(request);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new NuxeoException(String.format("Fail to create mapping on %s/%s: %s", indexName, type, response));
        }
    }

    /**
     * Performs an Elastic request using the low level client, exposed for internal use only.
     */
    public Response performRequest(Request request) {
        try {
            return lowLevelClient.performRequest(request);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public String getNodesInfo() {
        Response response = performRequest(new Request("GET", "/_nodes/_all"));
        try {
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public String getNodesStats() {
        Response response = performRequest(new Request("GET", "/_nodes/stats"));
        try {
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public boolean aliasExists(String aliasName) {
        Response response = performRequest(new Request("HEAD", String.format("/_alias/%s", aliasName)));
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
        Response response = performRequest(new Request("GET", String.format("/_alias/%s", aliasName)));
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
        Response response = performRequest(new Request("DELETE", String.format("/_all/_alias/%s", aliasName)));
        int code = response.getStatusLine().getStatusCode();
        if (code != HttpStatus.SC_OK) {
            throw new IllegalStateException(String.format("Deleting %s alias: %s", aliasName, response));
        }
    }

    protected void createAlias(String aliasName, String indexName) {
        Response response = performRequest(new Request("PUT", String.format("/%s/_alias/%s", indexName, aliasName)));
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new NuxeoException("Fail to create alias: " + indexName + " :" + response);
        }
    }

    @Override
    public BulkResponse bulk(BulkRequest request) {
        // 3 retries with backoff of 30s jitter 0.5:
        // retry 1: 30s +/-15 [t+15, t+45]
        // retry 2: 60s +/-30 [t+45, t+135]
        // retry 3: 120 +/-60 [t+105, t+315]
        RetryPolicy policy = new RetryPolicy().withMaxRetries(3)
                                              .withBackoff(30, 200, TimeUnit.SECONDS)
                                              .withJitter(0.5)
                                              .retryOn(TooManyRequestsRetryableException.class);
        AtomicReference<BulkResponse> response = new AtomicReference<>();
        Failsafe.with(policy)
                .onRetry(failure -> log.warn("Retrying bulk index ... " + request.getDescription()))
                .onRetriesExceeded(failure -> log.warn(
                        "Give up bulk index after " + policy.getMaxRetries() + " retries: " + request.getDescription()))
                .run(() -> response.set(doBulk(request)));
        return response.get();
    }

    protected BulkResponse doBulk(BulkRequest request) throws TooManyRequestsRetryableException {
        try {
            if (BulkShardRequest.DEFAULT_TIMEOUT == request.timeout()) {
                // use a longer timeout than the default one
                request.timeout(LONG_TIMEOUT);
            }
            BulkResponse response = client.bulk(request, RequestOptions.DEFAULT);
            if (response.hasFailures()) {
                for (BulkItemResponse item : response.getItems()) {
                    if (item.isFailed() && RestStatus.TOO_MANY_REQUESTS == item.getFailure().getStatus()) {
                        // Since Elastic 7.0 transient circuit breaker exceptions return 429
                        log.warn("Detecting overloaded Elastic bulk response: " + item.getFailureMessage());
                        throw new TooManyRequestsRetryableException(item.getFailureMessage());
                    }
                }
            }
            return response;
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == RestStatus.TOO_MANY_REQUESTS.getStatus()) {
                log.warn("Detecting overloaded Elastic response: " + e.getResponse().getStatusLine());
                throw new TooManyRequestsRetryableException(e.getResponse().getStatusLine().toString());
            }
            throw new NuxeoException(e);
        } catch (ElasticsearchStatusException e) {
             if (RestStatus.TOO_MANY_REQUESTS.equals(e.status())) {
                 log.warn("Detecting overloaded Elastic bulk response: " + e.getMessage());
                 throw new TooManyRequestsRetryableException(e.getMessage());
             }
             throw new NuxeoException(e);
        } catch (SocketTimeoutException e) {
             log.warn("Elastic timeout, might be overloaded", e);
             throw new TooManyRequestsRetryableException(e.getMessage());
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public DeleteResponse delete(DeleteRequest request) {
        try {
            if (ReplicationRequest.DEFAULT_TIMEOUT == request.timeout()) {
                // use a longer timeout than the default one
                request.timeout(LONG_TIMEOUT);
            }
            return client.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        try {
            return client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public SearchResponse searchScroll(SearchScrollRequest request) {
        try {
            return client.scroll(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public GetResponse get(GetRequest request) {
        try {
            return client.get(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public IndexResponse index(IndexRequest request) {
        // 3 retries with backoff of 20s jitter 0.5:
        // retry 1: 20s +/-10 [t+10, t+30]
        // retry 2: 40s +/-20 [t+30 t+90]
        // retry 3: 80S +/-40 [t+70, t+210]
        RetryPolicy policy = new RetryPolicy().withMaxRetries(3)
                                              .withBackoff(20, 200, TimeUnit.SECONDS)
                                              .withJitter(0.5)
                                              .retryOn(TooManyRequestsRetryableException.class);
        AtomicReference<IndexResponse> response = new AtomicReference<>();
        Failsafe.with(policy)
                .onRetry(failure -> log.warn("Retrying index ... " + request.getDescription()))
                .onRetriesExceeded(failure -> log.warn(
                        "Give up index after " + policy.getMaxRetries() + " retries: " + request.getDescription()))
                .run(() -> response.set(doIndex(request)));
        return response.get();
    }

    protected IndexResponse doIndex(IndexRequest request) throws TooManyRequestsRetryableException {
        try {
            if (IndexRequest.DEFAULT_TIMEOUT == request.timeout()) {
                // use a longer timeout than the default one
                request.timeout(LONG_TIMEOUT);
            }
            return client.index(request, RequestOptions.DEFAULT);
        } catch (ElasticsearchStatusException e) {
            if (RestStatus.CONFLICT.equals(e.status())) {
                throw new ConcurrentUpdateException(e);
            } else if (RestStatus.TOO_MANY_REQUESTS.equals(e.status())) {
                log.warn("Detecting overloaded Elastic index response: " + e.getMessage());
                throw new TooManyRequestsRetryableException(e.getMessage());
            }
            throw new NuxeoException(e);
        } catch (SocketTimeoutException e) {
            log.warn("Elastic timeout, might be overloaded", e);
            throw new TooManyRequestsRetryableException(e.getMessage());
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
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

    /**
     * Exception when Elastic is overloaded by indexing requests.
     *
     * @since 2021.16
     */
    public static class TooManyRequestsRetryableException extends Exception {

        public TooManyRequestsRetryableException(String message) {
            super(message);
        }
    }
}
