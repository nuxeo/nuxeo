/*
 * (C) Copyright 2017-2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.opensearch1.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.function.ThrowableBiFunction;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.runtime.ConcurrentException;
import org.nuxeo.runtime.RetryableException;
import org.nuxeo.runtime.RuntimeServiceException;
import org.opensearch.OpenSearchStatusException;
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.bulk.BulkShardRequest;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.ClearScrollRequest;
import org.opensearch.action.search.ClearScrollResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchScrollRequest;
import org.opensearch.client.Request;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseException;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.cluster.health.ClusterHealthStatus;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.rest.RestStatus;

import io.opencensus.common.Scope;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

/**
 * @since 9.3
 */
public class OpenSearchRestClient implements OpenSearchClient {

    private static final Logger log = LogManager.getLogger(OpenSearchRestClient.class);

    // @since 11.5
    public static final String LONG_TIMEOUT = "120s";

    /** @deprecated use {@link #LONG_TIMEOUT} instead */
    public static final String CREATE_INDEX_TIMEOUT = LONG_TIMEOUT;

    protected final String id;

    protected final RestHighLevelClient client;

    protected RequestOptions COMPAT_ES_OPTIONS = RequestOptions.DEFAULT.toBuilder()
                                                                       .addHeader("Accept",
                                                                               "application/json; compatible-with=7; charset=UTF-8")
                                                                       .addHeader("Content-Type",
                                                                               "application/json; compatible-with=7; charset=UTF-8")
                                                                       .build();

    public OpenSearchRestClient(String id, RestHighLevelClient client) {
        this.id = id;
        this.client = client;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isReady() {
        try {
            Response response = performRequestWithTracing(
                    new Request("GET", "/_cluster/health?wait_for_status=yellow&timeout=20s"));
            try (InputStream is = response.getEntity().getContent()) {
                Map<String, Object> map = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
                ClusterHealthStatus status = ClusterHealthStatus.fromString((String) map.get("status"));
                log.trace("OpenSearch Cluster: {} is {}",
                        () -> client.getLowLevelClient().getNodes().getFirst().getHost(), () -> status);
                return switch (status) {
                    case GREEN, YELLOW -> true;
                    case ClusterHealthStatus healthStatus -> {
                        log.warn("OpenSearch client not ready status: {}", healthStatus);
                        yield false;
                    }
                };
            }
        } catch (IOException | RuntimeServiceException e) {
            log.warn("OpenSearch client not ready: {}", e::getMessage);
            return false;
        }
    }

    @Override
    public boolean waitForYellowStatus(String[] indexNames, Duration timeout) {
        ClusterHealthStatus healthStatus;
        Response response;
        try {
            response = performRequestWithTracing(
                    new Request("GET", String.format("/_cluster/health/%s?wait_for_status=yellow&timeout=%ds",
                            getIndexesAsString(indexNames), timeout.toSeconds())));
            try (InputStream is = response.getEntity().getContent()) {
                Map<String, Object> map = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
                healthStatus = ClusterHealthStatus.fromString((String) map.get("status"));
            }
        } catch (IOException e) {
            throw new RuntimeServiceException(e);
        }
        return switch (healthStatus) {
            case GREEN -> {
                log.trace("OpenSearch Cluster ready: {}", response);
                yield true;
            }
            case YELLOW -> {
                log.warn("OpenSearch Cluster ready but not GREEN: {}", response);
                yield false;
            }
            default -> throw new IllegalStateException("OpenSearch Cluster health status: " + healthStatus
                    + ", not Yellow after " + timeout + " give up: " + response);
        };
    }

    protected String getIndexesAsString(String[] indexNames) {
        return indexNames == null ? "" : String.join(",", indexNames);
    }

    @Override
    public void refresh(String indexName) {
        log.trace("Refreshing index: {}", indexName);
        try {
            performRequestWithTracing(new Request("POST", "/" + indexName + "/_refresh"));
            log.trace("Index: {} refreshed", indexName);
        } catch (RuntimeServiceException e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            if (cause instanceof SocketTimeoutException) {
                // We don't want to throw failure on refresh timeout because previous indexing commands are processed
                log.warn("Ignoring refresh timeouts: {}", e::getMessage);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void flush(String indexName) {
        log.trace("Flushing index: {}", indexName);
        performRequestWithTracing(new Request("POST", "/" + indexName + "/_flush?wait_if_ongoing=true"));
        log.trace("Index: {} flushed", indexName);
    }

    @Override
    public void optimize(String indexName) {
        log.trace("Optimizing index: {}", indexName);
        performRequestWithTracing(new Request("POST", "/" + indexName + "/_forcemerge?max_num_segments=1"));
        log.trace("Index: {} optimized", indexName);
    }

    @Override
    public void createIndex(String indexName, String jsonSettings) {
        log.trace("Creating index: {} with setting: {}", indexName, jsonSettings);
        Request request = new Request("PUT",
                "/" + indexName + "?master_timeout=" + LONG_TIMEOUT + "&timeout=" + LONG_TIMEOUT);
        // since elastic 7 REST API needs an additional level
        request.setJsonEntity("{\"settings\": " + jsonSettings + "}");
        Response response = performRequestWithTracing(request);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new RuntimeServiceException("Fail to create index: " + indexName + " :" + response);
        }
        log.trace("Index: {} created", indexName);
    }

    @Override
    public boolean indexExists(String indexName) {
        log.trace("Checking if index: {} exits", indexName);
        Response response = performRequestWithTracing(new Request("HEAD", "/" + indexName));
        return switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK -> {
                log.trace("Index: {} exists", indexName);
                yield true;
            }
            case HttpStatus.SC_NOT_FOUND -> {
                log.trace("Index: {} not found", indexName);
                yield false;
            }
            default ->
                throw new IllegalStateException(String.format("Checking index: %s returns: %s", indexName, response));
        };
    }

    @Override
    public void dropIndex(String indexName, Duration timeout) {
        log.trace("Dropping index: {}", indexName);
        try {
            long timeoutSeconds = timeout.toSeconds();
            client.getLowLevelClient()
                  .performRequest(new Request("DELETE", String.format("/%s?master_timeout=%ds&timeout=%ds", indexName,
                          timeoutSeconds, timeoutSeconds)));
        } catch (IOException e) {
            if (e instanceof ResponseException re) {
                if (re.getResponse().getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    log.info("Index: {} not found, nothing to drop", indexName);
                    return;
                }
            }
            log.warn("Fail to drop index: {}, {}", indexName, e.getMessage());
            throw new RuntimeServiceException("Fail to drop index: " + indexName, e);
        }
        log.trace("Index: {} dropped", indexName);
    }

    @Override
    public void createMapping(String indexName, String jsonMapping) {
        log.trace("Creating mapping for index {}: {}", indexName, jsonMapping);
        Request request = new Request("PUT", String.format("/%s/_mapping", indexName));
        request.setJsonEntity(jsonMapping);
        Response response = performRequestWithTracing(request);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new RuntimeServiceException(String.format("Fail to create mapping on %s: %s", indexName, response));
        }
        log.trace("Mapping for index: {} created", indexName);
    }

    @Override
    public boolean mappingExists(String indexName) {
        log.trace("Checking if mapping exists for index: {}", indexName);
        // HEAD is not supported anymore since elastic 7.x
        Response response = performRequestWithTracing(new Request("GET", String.format("/%s/_mapping", indexName)));
        return switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK -> {
                log.trace("Mapping for index: {} exists", indexName);
                yield true;
            }
            case HttpStatus.SC_NOT_FOUND -> {
                log.trace("Mapping for index: {} not found", indexName);
                yield false;
            }
            default ->
                throw new IllegalStateException(String.format("Checking mapping %s returns: %s", indexName, response));
        };
    }

    @Override
    public String getMapping(String indexName) {
        Request request = new Request("GET", String.format("/%s/_mapping", indexName));
        Response response = performRequestWithTracing(request);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new RuntimeServiceException(String.format("Fail to get mapping on %s: %s", indexName, response));
        }
        try {
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new RuntimeServiceException(e);
        }
    }

    /**
     * Performs an OpenSearch request using the low level client, exposed since 11.5 for internal use only.
     */
    @Deprecated(forRemoval = true) // for public modifier
    public Response performRequestWithTracing(Request request) {
        try (Scope ignored = getScopedSpan("opensearch" + request.getEndpoint(), request.toString())) {
            request.setOptions(COMPAT_ES_OPTIONS);
            return client.getLowLevelClient().performRequest(request);
        } catch (IOException e) {
            throw new RuntimeServiceException(e);
        }
    }

    @Override
    public IndexResponse index(IndexRequest request) {
        // 3 retries with backoff of 20s jitter 0.5:
        // retry 1: 20s +/-10 [t+10, t+30]
        // retry 2: 40s +/-20 [t+30 t+90]
        // retry 3: 80S +/-40 [t+70, t+210]
        var maxRetries = 3;
        RetryPolicy<Object> policy = new RetryPolicy<>().withMaxRetries(maxRetries)
                                                        .withBackoff(20, 200, ChronoUnit.SECONDS)
                                                        .withJitter(0.5)
                                                        .onRetry(failure -> log.warn("Retrying index ... {}",
                                                                request::getDescription))
                                                        .onRetriesExceeded(failure -> log.warn(
                                                                "Give up index after {} retries: {}", () -> maxRetries,
                                                                request::getDescription))
                                                        .handle(RetryableException.class);
        if (IndexRequest.DEFAULT_TIMEOUT == request.timeout()) {
            // use a longer timeout than the default one
            request.timeout(LONG_TIMEOUT);
        }
        AtomicReference<IndexResponse> response = new AtomicReference<>();
        Failsafe.with(policy).run(() -> response.set(doIndex(request)));
        return response.get();
    }

    protected IndexResponse doIndex(IndexRequest request) throws RetryableException {
        return performRequestWithTracing(client::index, request, "opensearch/_index");
    }

    @Override
    public BulkResponse bulk(BulkRequest request) {
        // 3 retries with backoff of 30s jitter 0.5:
        // retry 1: 30s +/-15 [t+15, t+45]
        // retry 2: 60s +/-30 [t+45, t+135]
        // retry 3: 120 +/-60 [t+105, t+315]
        var maxRetries = 3;
        var policy = new RetryPolicy<>().withMaxRetries(maxRetries)
                                        .withBackoff(30, 200, ChronoUnit.SECONDS)
                                        .withJitter(0.5)
                                        .onRetry(
                                                failure -> log.atWarn()
                                                              .withThrowable(
                                                                      log.isDebugEnabled() ? failure.getLastFailure()
                                                                              : null)
                                                              .log("Retrying bulk index request: {}, due to the error: {}. Enable debug log level to see stacktrace.",
                                                                      request::getDescription,
                                                                      () -> failure.getLastFailure().getMessage()))
                                        .onRetriesExceeded(evt -> log.warn(
                                                "Give up bulk index request after {} retries, request:: {}",
                                                () -> maxRetries, request::getDescription))
                                        .handle(RetryableException.class);
        if (BulkShardRequest.DEFAULT_TIMEOUT == request.timeout()) {
            // use a longer timeout than the default one
            request.timeout(LONG_TIMEOUT);
        }
        AtomicReference<BulkResponse> response = new AtomicReference<>();
        Failsafe.with(policy).run(() -> response.set(doBulk(request)));
        return response.get();
    }

    protected BulkResponse doBulk(BulkRequest request) throws RetryableException {
        var responses = performRequestWithTracing(client::bulk, request, "opensearch/_bulk");
        if (responses.hasFailures()) {
            for (BulkItemResponse response : responses.getItems()) {
                if (!response.isFailed()) {
                    continue;
                }
                switch (response.getFailure().getStatus()) {
                    case CONFLICT -> log.trace("Ignore indexing of: {} because a more recent versions is present: {}",
                            response.getId(), response.getFailureMessage());
                    case TOO_MANY_REQUESTS, BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT ->
                        throw new RetryableException("Detecting overloaded OpenSearch (%s): %s".formatted(
                                response.getFailure().getStatus(), response.getFailureMessage()));
                    default -> log.error("Indexing failure of: {}, {}", response.getId(), response.getFailureMessage());
                }
            }
        }
        return responses;
    }

    @Override
    public GetResponse get(GetRequest request) {
        return performRequestWithTracing(client::get, request, "opensearch/_get");
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        return performRequestWithTracing(client::search, request, "opensearch/_search");
    }

    @Override
    public SearchResponse scroll(SearchScrollRequest request) {
        return performRequestWithTracing(client::scroll, request, "opensearch/_scroll");
    }

    @Override
    public ClearScrollResponse clearScroll(ClearScrollRequest request) {
        try {
            log.trace("Clearing scroll ids: {}", () -> Arrays.toString(request.getScrollIds().toArray()));
            return performRequestWithTracing(client::clearScroll, request, "opensearch/_clearScroll");
        } catch (RuntimeServiceException e) {
            if (e.getCause() instanceof OpenSearchStatusException statusException
                    && RestStatus.NOT_FOUND.equals(statusException.status())) {
                log.trace("Scroll ids not found, they have certainly been already closed: {}",
                        () -> Arrays.toString(request.getScrollIds().toArray()));
                return new ClearScrollResponse(true, 0);
            }
            throw e;
        }
    }

    protected <I extends ActionRequest, O extends ActionResponse> O performRequestWithTracing(
            ThrowableBiFunction<I, RequestOptions, O, IOException> runner, I request, String spanName) {
        String requestStr;
        if (request instanceof BulkRequest bulkRequest) {
            requestStr = "actions: " + bulkRequest.numberOfActions();
        } else if (request instanceof ClearScrollRequest scrollRequest) {
            requestStr = "clearScroll: " + scrollRequest.getScrollIds();
        } else {
            requestStr = request.toString();
        }
        try (var ignored = getScopedSpan(spanName, request.toString())) {
            log.trace("Running request: {}", requestStr);
            O response = runner.apply(request, COMPAT_ES_OPTIONS);
            log.trace("Received response: {}", response);
            return response;
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == RestStatus.TOO_MANY_REQUESTS.getStatus()) {
                log.warn("Detecting overloaded OpenSearch: {}", e.getResponse().getStatusLine());
                throw new RetryableException("Detecting overloaded OpenSearch, response status: %s".formatted(
                        e.getResponse().getStatusLine()), e);
            }
            throw new RuntimeServiceException(e);
        } catch (OpenSearchStatusException e) {
            if (RestStatus.CONFLICT.equals(e.status())) {
                log.info("Conflict: {}", e.getMessage());
                throw new ConcurrentException(e);
            } else if (RestStatus.TOO_MANY_REQUESTS.equals(e.status())) {
                log.warn("Detecting overloaded OpenSearch: {}", e.getMessage());
                throw new RetryableException("OpenSearch timeout, might be overloaded", e);
            } else if (e.getMessage() != null && e.getMessage().contains("Unable to parse response body")) {
                // 404 with unparseable body is benign (e.g. already-freed scroll), let caller handle it
                if (!RestStatus.NOT_FOUND.equals(e.status())) {
                    log.warn("OpenSearch not available, bad gateway", e);
                    throw new RetryableException("OpenSearch timeout, might be overloaded", e);
                }
            }
            throw new RuntimeServiceException(e);
        } catch (SocketTimeoutException e) {
            log.warn("OpenSearch socket timeout, might be overloaded", e);
            throw new RetryableException("OpenSearch socket timeout, might be overloaded", e);
        } catch (ConnectionClosedException e) {
            log.warn("OpenSearch connection has been closed unexpectedly", e);
            throw new RetryableException("OpenSearch connection closed", e);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Connection reset")) {
                log.warn("OpenSearch connection has been reset", e);
                throw new RetryableException("OpenSearch connection reset", e);
            }
            throw new RuntimeServiceException(e);
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
    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            log.warn("Failed to close the OpenSearch client: {}", id, e);
        }
    }
}
