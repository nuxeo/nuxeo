/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.web.common.idempotency;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Filter handling an idempotency key in POST requests.
 * <p>
 * If {@link #HEADER_KEY} is found in the request header, will intercept request handling to:
 * <ul>
 * <li>mark the request as being processed
 * <li>capture the response when request was processed without any error and store it
 * <li>return the stored response if a subsequent request with the same key is processed again
 * <li>return a conflict response if a request with the same key is processed while the first request is still in
 * progress.
 * </ul>
 *
 * @since 11.5
 */
public class NuxeoIdempotentFilter extends HttpFilter {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LogManager.getLogger(NuxeoIdempotentFilter.class);

    public static final String HEADER_KEY = "Idempotency-Key";

    public static final String STORE_PROPERTY = "org.nuxeo.request.idempotency.keyvaluestore.name";

    public static final String DEFAULT_STORE = "idempotentrequest";

    protected static final Duration DEFAULT_TTL = Duration.ofDays(1);

    public static final String TTL_DURATION_PROPERTY = "org.nuxeo.request.idempotency.ttl.duration";

    public static final String INPROGRESS_MARKER = "{\"inprogress\":true}";

    public static final String INFO_SUFFIX = "_info";

    protected static final int DEFERRED_OUTPUT_STREAM_THRESHOLD = 1024 * 1024; // 1 MB

    protected static final int MAX_CONTENT_SIZE = 1024 * 1024 * 5; // 5 MB

    protected static final Set<String> IDEMPOTENT_METHODS = new HashSet<>(Arrays.asList(
            // safe methods according to RFC 7231 4.2.1
            HttpGet.METHOD_NAME, HttpHead.METHOD_NAME, HttpOptions.METHOD_NAME, HttpTrace.METHOD_NAME,
            // idempotent methods according to RFC 7231 4.2.2
            HttpPut.METHOD_NAME, HttpDelete.METHOD_NAME));

    protected Duration getTTL() {
        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        if (cs != null) {
            return cs.getDuration(TTL_DURATION_PROPERTY, DEFAULT_TTL);
        }
        return DEFAULT_TTL;
    }

    protected String getStoreName() {
        ConfigurationService cs = Framework.getService(ConfigurationService.class);
        if (cs != null) {
            return cs.getString(STORE_PROPERTY, DEFAULT_STORE);
        }
        return DEFAULT_STORE;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!doFilterIdempotent(request, response, chain)) {
            chain.doFilter(request, response);
        }
    }

    protected boolean doFilterIdempotent(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String method = request.getMethod();
        if (IDEMPOTENT_METHODS.contains(method)) {
            log.debug("No idempotent processing done: method is already idempotent: {}", method);
            return false;
        }
        String key = request.getHeader(HEADER_KEY);
        if (key == null) {
            log.debug("No idempotent processing done: no {} header present", HEADER_KEY);
            return false;
        }
        log.debug("Idempotent request key: {}", key);
        KeyValueService kvs = Framework.getService(KeyValueService.class);
        if (kvs == null) {
            log.debug("KeyValueService not present");
            return false;
        }
        KeyValueStore store = kvs.getKeyValueStore(getStoreName());
        String storeStatus = store.getString(key + INFO_SUFFIX);
        byte[] storeContent = store.get(key);
        if (storeStatus == null) {
            log.debug("Handle new request for key: {}", key);
            long ttl = getTTL().getSeconds();
            store.put(key + INFO_SUFFIX, INPROGRESS_MARKER, ttl);
            try {
                try (CopyingResponseWrapper wrapper = new CopyingResponseWrapper(DEFERRED_OUTPUT_STREAM_THRESHOLD,
                        response)) {
                    wrapper.setHeader(HEADER_KEY, key);
                    chain.doFilter(request, wrapper);
                    wrapper.flushBuffer();
                    long size = wrapper.getCopySize();
                    if (size > MAX_CONTENT_SIZE) {
                        log.debug(
                                "Not storing response for key: {} (status: {}, size in bytes: {}), max content size exceeded: {}",
                                key, wrapper.getStatus(), size, MAX_CONTENT_SIZE);
                        return true;
                    }
                    byte[] content = wrapper.getCopyAsBytes();
                    store.put(key, content, ttl);
                    store.put(key + INFO_SUFFIX, NuxeoIdempotentResponse.save(wrapper), ttl);
                    log.debug("Stored response for key: {} (status: {}, size in bytes: {})", key, wrapper.getStatus(),
                            content.length);
                    return true;
                }
            } catch (IOException | ServletException e) {
                if (!response.isCommitted()) {
                    response.setStatus(SC_INTERNAL_SERVER_ERROR);
                    response.setHeader(HEADER_KEY, key);
                }
                throw e;
            } finally {
                if (response.getStatus() >= SC_BAD_REQUEST) {
                    // error request: cleanup store
                    store.put(key, (String) null);
                    store.put(key + INFO_SUFFIX, (String) null);
                    log.debug("Cleanup store: error for key: {}", key);
                }
            }
        } else if (INPROGRESS_MARKER.equals(storeStatus)) {
            // request already in progress -> conflict
            // Don't call response.sendError, because it commits the response
            // which prevents NuxeoExceptionFilter from returning a custom error page.
            response.setStatus(SC_CONFLICT);
            response.setHeader(HEADER_KEY, key);
            log.debug("Conflict response for key: {}", key);
            return true;
        } else {
            try {
                // request already done: return stored result
                NuxeoIdempotentResponse.restore(response, storeStatus.getBytes());
                response.setHeader(HEADER_KEY, key);
                response.getOutputStream().write(storeContent);
                log.debug("Returning stored response for key: {} (status: {}, size in bytes: {})", key,
                        response.getStatus(), storeContent.length);
                return true;
            } catch (IOException e) {
                log.error("Error processing stored result for key: {}", key, e);
                return false;
            }
        }
    }

}
