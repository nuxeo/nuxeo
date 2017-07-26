/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.jaxrs.test;

import java.io.InputStream;

import javax.ws.rs.core.Response.StatusType;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.MessageBodyWorkers;

/**
 * Wraps a {@link ClientResponse} to make it {@link AutoCloseable}.
 *
 * @since 9.3
 */
public class CloseableClientResponse extends ClientResponse implements AutoCloseable {

    public CloseableClientResponse(StatusType statusType, InBoundHeaders headers, InputStream entity,
            MessageBodyWorkers workers) {
        super(statusType, headers, entity, workers);
    }

    /**
     * Makes the given {@link ClientResponse} {@link AutoCloseable} by wrapping it in a {@link CloseableClientResponse}.
     */
    public static CloseableClientResponse of(ClientResponse response) {
        return new CloseableClientResponse(response.getStatusInfo(), getResponseHeaders(response),
                response.getEntityInputStream(), response.getClient().getMessageBodyWorkers());
    }

    protected static InBoundHeaders getResponseHeaders(ClientResponse response) {
        InBoundHeaders inBoundHeaders = new InBoundHeaders();
        inBoundHeaders.putAll(response.getHeaders());
        return inBoundHeaders;
    }

}
