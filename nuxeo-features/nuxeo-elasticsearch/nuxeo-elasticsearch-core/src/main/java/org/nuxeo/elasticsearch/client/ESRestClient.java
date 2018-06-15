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

import static java.util.Collections.emptyMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchStatusException;
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
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.rest.RestStatus;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.elasticsearch.api.ESClient;

/**
 * @since 9.3
 */
public class ESRestClient implements ESClient {
    // TODO: add security sanitizer to make sure all parameters used to build requests are clean
    private static final Log log = LogFactory.getLog(ESRestClient.class);

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
            response = lowLevelClient.performRequest("GET",
                    String.format("/_cluster/health/%s?wait_for_status=yellow&timeout=%ds",
                            getIndexesAsString(indexNames), timeoutSecond));
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
        try {
            Response response = lowLevelClient.performRequest("GET",
                    String.format("/_cluster/health/%s", getIndexesAsString(indexNames)));
            try (InputStream is = response.getEntity().getContent()) {
                Map<String, Object> map = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
                return ClusterHealthStatus.fromString((String) map.get("status"));
            }
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void refresh(String indexName) {
        try {
            lowLevelClient.performRequest("POST", "/" + indexName + "/_refresh");
        } catch (IOException e) {
            throw new NuxeoException(e);
        }

    }

    @Override
    public void flush(String indexName) {
        try {
            lowLevelClient.performRequest("POST", "/" + indexName + "/_flush?wait_if_ongoing=true");
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void optimize(String indexName) {
        try {
            lowLevelClient.performRequest("POST", "/" + indexName + "/_forcemerge?max_num_segments=1");
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public boolean indexExists(String indexName) {
        Response response;
        try {
            response = lowLevelClient.performRequest("HEAD", "/" + indexName);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        int code = response.getStatusLine().getStatusCode();
        if (code == HttpStatus.SC_OK) {
            return true;
        } else if (code == HttpStatus.SC_NOT_FOUND) {
            return false;
        }
        throw new IllegalStateException(String.format("Checking index %s returns: %s", indexName, response));
    }

    @Override
    public boolean mappingExists(String indexName, String type) {
        Response response;
        try {
            response = lowLevelClient.performRequest("HEAD", String.format("/%s/_mapping/%s", indexName, type));
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        int code = response.getStatusLine().getStatusCode();
        if (code == HttpStatus.SC_OK) {
            return true;
        } else if (code == HttpStatus.SC_NOT_FOUND) {
            return false;
        }
        throw new IllegalStateException(String.format("Checking mapping %s returns: %s", indexName, response));
    }

    @Override
    public void deleteIndex(String indexName, int timeoutSecond) {
        Response response;
        try {
            response = lowLevelClient.performRequest("DELETE",
                    String.format("/%s?master_timeout=%ds", indexName, timeoutSecond));
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
        HttpEntity entity = new NStringEntity(jsonSettings, ContentType.APPLICATION_JSON);
        Response response;
        try {
            response = lowLevelClient.performRequest("PUT", "/" + indexName, emptyMap(), entity);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new NuxeoException("Fail to create index: " + indexName + " :" + response);
        }
    }

    @Override
    public void createMapping(String indexName, String type, String jsonMapping) {
        HttpEntity entity = new NStringEntity(jsonMapping, ContentType.APPLICATION_JSON);
        Response response;
        try {
            response = lowLevelClient.performRequest("PUT", String.format("/%s/%s/_mapping", indexName, type),
                    emptyMap(), entity);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new NuxeoException(String.format("Fail to create mapping on %s/%s: %s", indexName, type, response));
        }

    }

    @Override
    public String getNodesInfo() {
        try {
            Response response = lowLevelClient.performRequest("GET", "/_nodes/_all");
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public String getNodesStats() {
        try {
            Response response = lowLevelClient.performRequest("GET", "/_nodes/stats");
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public boolean aliasExists(String aliasName) {
        Response response;
        try {
            response = lowLevelClient.performRequest("HEAD", String.format("/_alias/%s", aliasName));
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        int code = response.getStatusLine().getStatusCode();
        if (code == HttpStatus.SC_OK) {
            return true;
        } else if (code == HttpStatus.SC_NOT_FOUND) {
            return false;
        }
        throw new IllegalStateException(String.format("Checking alias %s returns: %s", aliasName, response));
    }

    @Override
    public String getFirstIndexForAlias(String aliasName) {
        if (!aliasExists(aliasName)) {
            return null;
        }
        try {
            Response response = lowLevelClient.performRequest("GET", String.format("/_alias/%s", aliasName));
            try (InputStream is = response.getEntity().getContent()) {
                Map<String, Object> map = XContentHelper.convertToMap(XContentType.JSON.xContent(), is, true);
                if (map.size() != 1) {
                    throw new NuxeoException(String.format(
                            "Expecting alias that point to a single index, alias: %s, got: %s", aliasName, response));
                }
                return map.keySet().iterator().next();
            }
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
        Response response;
        try {
            response = lowLevelClient.performRequest("DELETE", String.format("/_all/_alias/%s", aliasName));
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        int code = response.getStatusLine().getStatusCode();
        if (code != HttpStatus.SC_OK) {
            throw new IllegalStateException(String.format("Deleting %s alias: %s", aliasName, response));
        }
    }

    protected void createAlias(String aliasName, String indexName) {
        Response response;
        try {
            response = lowLevelClient.performRequest("PUT", String.format("/%s/_alias/%s", indexName, aliasName),
                    emptyMap());
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new NuxeoException("Fail to create alias: " + indexName + " :" + response);
        }
    }

    @Override
    public BulkResponse bulk(BulkRequest request) {
        try {
            return client.bulk(request);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public DeleteResponse delete(DeleteRequest request) {
        try {
            return client.delete(request);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        try {
            return client.search(request);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public SearchResponse searchScroll(SearchScrollRequest request) {
        try {
            return client.searchScroll(request);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public GetResponse get(GetRequest request) {
        try {
            return client.get(request);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public IndexResponse index(IndexRequest request) {
        try {
            return client.index(request);
        } catch (ResponseException e) {
            if (e.getMessage() != null && e.getMessage().contains("409 Conflict")) {
                // when a more recent version already exists, throws the same exception than the transport client
                throw new VersionConflictEngineException(null, e.getMessage(), e);
            }
            throw new NuxeoException(e);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public ClearScrollResponse clearScroll(ClearScrollRequest request) {
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Clearing scroll ids: %s",
                        Arrays.toString(request.getScrollIds().toArray())));
            }
            return client.clearScroll(request);
        } catch (ElasticsearchStatusException e) {
            if (RestStatus.NOT_FOUND.equals(e.status())) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Scroll ids not found, they have certainly been already closed",
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
