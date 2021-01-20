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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * POJO representing response to be serialized and served by {@link NuxeoIdempotentFilter}.
 *
 * @since 11.5
 */
public class NuxeoIdempotentResponse {

    public static final Set<String> SKIPPED_HEADERS = new HashSet<>(Arrays.asList("Transfer-Encoding"));

    protected int status;

    protected Map<String, Collection<String>> headers = new LinkedHashMap<>();

    protected static final ObjectMapper MAPPER = //
            new ObjectMapper().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                              .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    protected static final ObjectReader READ_MAPPER = MAPPER.readerFor(NuxeoIdempotentResponse.class)
                                                            .withoutRootName()
                                                            .without(JsonParser.Feature.AUTO_CLOSE_SOURCE);

    protected static final ObjectWriter WRITE_MAPPER = MAPPER.writerFor(NuxeoIdempotentResponse.class)
                                                             .withoutRootName()
                                                             .with(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM)
                                                             .without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, Collection<String>> getHeaders() {
        return headers;
    }

    public void setHeader(String name, Collection<String> value) {
        headers.put(name, value);
    }

    public static final void restore(HttpServletResponse response, byte[] bytes) throws IOException {
        NuxeoIdempotentResponse stored = READ_MAPPER.readValue(bytes);
        response.setStatus(stored.getStatus());
        stored.getHeaders().forEach((name, values) -> {
            boolean isFirst = true;
            for (String value : values) {
                if (SKIPPED_HEADERS.contains(name)) {
                    continue;
                }
                if (isFirst) {
                    response.setHeader(name, value);
                    isFirst = false;
                } else {
                    response.addHeader(name, value);
                }
            }
        });
    }

    public static final byte[] save(HttpServletResponse response) throws JsonProcessingException {
        NuxeoIdempotentResponse stored = new NuxeoIdempotentResponse();
        stored.setStatus(response.getStatus());
        response.getHeaderNames()
                .stream()
                .filter(h -> !SKIPPED_HEADERS.contains(h))
                .forEach(name -> stored.setHeader(name, response.getHeaders(name)));
        return WRITE_MAPPER.writeValueAsBytes(stored);
    }

}
