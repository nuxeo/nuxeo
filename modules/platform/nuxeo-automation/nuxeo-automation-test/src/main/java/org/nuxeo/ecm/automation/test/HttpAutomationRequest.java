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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.automation.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.LOCATION;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.client.protocol.HttpClientContext.REDIRECT_LOCATIONS;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.nuxeo.ecm.automation.jaxrs.io.InputStreamDataSource;
import org.nuxeo.ecm.automation.jaxrs.io.SharedFileInputStream;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

/**
 * @since 11.1
 */
public class HttpAutomationRequest {

    public static final String ASYNC_ADAPTER = "/@async";

    public static final String ENTITY_TYPE = "entity-type";

    public static final String ENTITY_TYPE_EXCEPTION = "exception";

    public static final String ENTITY_TYPE_LOGIN = "login";

    public static final String ENTITY_TYPE_DOCUMENTS = "documents";

    public static final String ENTITY_TYPE_DOCUMENT = "document";

    public static final String ENTITY_TYPE_BOOLEAN = "boolean";

    public static final String ENTITY_TYPE_STRING = "string";

    public static final String ENTITY_TYPE_NUMBER = "number";

    public static final String ENTITY_TYPE_DATE = "date";

    public static final String VALUE = "value";

    public static final String INPUT = "input";

    public static final String PARAMS = "params";

    public static final String CONTEXT = "context";

    protected static final Duration ASYNC_POLL_DELAY = Duration.ofSeconds(1);

    protected static final Duration ASYNC_POLL_TIMEOUT = Duration.ofSeconds(30);

    protected static final JsonFactory FACTORY = new JsonFactory();

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected final HttpAutomationSession session;

    // may be null for login/getFile requests
    protected final String operationId;

    protected final Map<String, Object> params; // NOSONAR

    protected final Map<String, Object> context; // NOSONAR

    protected final Map<String, String> headers;

    protected Object input; // NOSONAR

    public HttpAutomationRequest(HttpAutomationSession session, String operationId) {
        this.session = session;
        this.operationId = operationId;
        params = new HashMap<>();
        context = new HashMap<>();
        headers = new HashMap<>();
    }

    public HttpAutomationSession getSession() {
        return session;
    }

    public HttpAutomationRequest setInput(Object input, Class<?> klass, String type) {
        return setInput(toNuxeoEntity(input, klass, type));
    }

    public HttpAutomationRequest setInput(Object input) {
        this.input = input;
        return this;
    }

    public HttpAutomationRequest set(String key, Object value, Class<?> klass, String type) {
        return set(key, toNuxeoEntity(value, klass, type));
    }

    public HttpAutomationRequest set(String key, Object value) {
        if (value == null) {
            params.remove(key);
            return this;
        }
        if (value.getClass() == Date.class) {
            Date date = (Date) value;
            params.put(key, date.toInstant().toString());
        } else if (value instanceof Calendar) {
            Calendar cal = (Calendar) value;
            params.put(key, cal.toInstant().toString());
        } else {
            params.put(key, value);
        }
        return this;
    }

    public HttpAutomationRequest setContextParameter(String key, Object value) {
        context.put(key, value);
        return this;
    }

    public HttpAutomationRequest setHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    /**
     * Converts an object or a list of objects into a representation suitable for server-side interpretation.
     */
    public Object toNuxeoEntity(Object object, Class<?> klass, String type) {
        if (object instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) object;
            return list.stream().map(o -> toNuxeoEntity(o, klass, type)).collect(toList());
        } else if (object instanceof Object[]) {
            Object[] array = (Object[]) object;
            return Arrays.stream(array).map(o -> toNuxeoEntity(o, klass, type)).collect(toList());
        }
        if (klass.isAssignableFrom(object.getClass())) {
            return Map.of(ENTITY_TYPE, type, VALUE, object);
        }
        throw new NuxeoException(object.getClass().getName() + " is not a " + klass.getName());
    }

    public static String getEntityType(JsonNode node) {
        if (node == null) {
            return null;
        }
        JsonNode entityTypeNode = node.get(ENTITY_TYPE);
        if (entityTypeNode == null) {
            return null;
        }
        return entityTypeNode.asText();
    }

    protected void setupAutomationRequest(AbstractHttpMessage request) {
        session.addAuthentication(request);
        request.setHeader(ACCEPT, APPLICATION_JSON.getMimeType() + ", */*");
        headers.forEach(request::setHeader);
    }

    public String executeRaw() throws IOException {
        try {
            return executeRaw(SC_OK, request -> {
                try {
                    return request.getBodyEntity();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public String executeRaw(int expectedStatusCode, Function<HttpAutomationRequest, HttpEntity> entityProvider)
            throws IOException {
        HttpPost request = new HttpPost(session.baseURL + operationId);
        setupAutomationRequest(request);
        request.setEntity(entityProvider.apply(this));
        try (CloseableHttpResponse response = session.client.execute(request);
                InputStream stream = response.getEntity().getContent()) {
            assertEquals(expectedStatusCode, response.getStatusLine().getStatusCode());
            return IOUtils.toString(stream, UTF_8);
        }
    }

    public JsonNode executeReturningDocument() throws IOException {
        JsonNode node = execute();
        assertEquals(ENTITY_TYPE_DOCUMENT, getEntityType(node));
        return node;
    }

    public List<JsonNode> executeReturningDocuments() throws IOException {
        JsonNode node = execute();
        assertEquals(ENTITY_TYPE_DOCUMENTS, getEntityType(node));
        JsonNode entries = node.get("entries");
        assertTrue(entries.getNodeType().toString(), entries.isArray());
        return IteratorUtils.toList(entries.iterator());
    }

    public boolean executeReturningBooleanEntity() throws IOException {
        JsonNode node = execute();
        assertEquals(ENTITY_TYPE_BOOLEAN, getEntityType(node));
        return node.get(VALUE).asBoolean();
    }

    public String executeReturningStringEntity() throws IOException {
        JsonNode node = execute();
        assertEquals(ENTITY_TYPE_STRING, getEntityType(node));
        return node.get(VALUE).asText();
    }

    public Number executeReturningNumberEntity() throws IOException {
        JsonNode node = execute();
        assertEquals(ENTITY_TYPE_NUMBER, getEntityType(node));
        return node.get(VALUE).numberValue();
    }

    public Instant executeReturningDateEntity() throws IOException {
        JsonNode node = execute();
        assertEquals(ENTITY_TYPE_DATE, getEntityType(node));
        return Instant.parse(node.get(VALUE).asText());
    }

    public <T> T executeReturningEntity(Class<T> klass) throws IOException {
        return executeReturningEntity(klass, null);
    }

    public <T> T executeReturningEntity(Class<T> klass, String type) throws IOException {
        JsonNode node = execute();
        if (node == null || node.isMissingNode()) {
            return null;
        }
        if (type == null) {
            type = klass.getName();
        }
        assertEquals(type, getEntityType(node));
        return MAPPER.convertValue(node.get(VALUE), klass);
    }

    public String executeReturningExceptionEntity(int expectedStatusCode) throws IOException {
        JsonNode node = execute(expectedStatusCode);
        if (node == null || node.isMissingNode()) {
            return null; // got status code, but no body
        }
        assertEquals(ENTITY_TYPE_EXCEPTION, getEntityType(node));
        return node.get("message").asText();
    }

    public <T> T executeReturning(Class<T> klass) throws IOException {
        JsonNode node = execute();
        return MAPPER.convertValue(node, klass);
    }

    public <T> T executeReturning(TypeReference<T> typeReference) throws IOException {
        JsonNode node = execute();
        return MAPPER.convertValue(node, typeReference);
    }

    public JsonNode execute() throws IOException {
        return execute(0);
    }

    public JsonNode execute(int expectedStatusCode) throws IOException {
        try {
            return execute(expectedStatusCode, stream -> {
                try {
                    return MAPPER.readTree(stream);
                } catch (ConnectionClosedException | MismatchedInputException e) {
                    return null;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public Blob executeReturningBlob() throws IOException {
        try {
            return execute(response -> {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == SC_NO_CONTENT) {
                    return null;
                }
                assertEquals(SC_OK, statusCode);
                try {
                    return getBlob(response);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public List<Blob> executeReturningBlobs() throws IOException {
        try {
            return execute(response -> {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == SC_NO_CONTENT) {
                    return null;
                }
                assertEquals(SC_OK, statusCode);
                try {
                    return getBlobs(response);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    protected Blob getBlob(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        String mimeType = entity.getContentType() == null ? null : entity.getContentType().getValue();
        String encoding = entity.getContentEncoding() == null ? null : entity.getContentEncoding().getValue();
        String contentDisposition = getHeader(response, "Content-Disposition");
        try (InputStream stream = entity.getContent()) {
            return getBlob(stream, mimeType, encoding, contentDisposition);
        } catch (ConnectionClosedException e) {
            return null;
        }
    }

    protected static String getHeader(HttpResponse response, String name) {
        Header[] headers = response.getHeaders(name);
        if (headers == null || headers.length == 0) {
            return null;
        } else {
            return headers[0].getValue();
        }
    }

    protected Blob getBlob(BodyPart part) throws IOException, MessagingException {
        String mimeType = part.getHeader("Content-Type")[0];
        String encoding = null;
        String contentDisposition = part.getHeader("Content-Disposition")[0];
        try (InputStream stream = part.getInputStream()) {
            return getBlob(stream, mimeType, encoding, contentDisposition);
        }
    }

    protected List<Blob> getBlobs(HttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        String contentType = entity.getContentType().getValue();
        assertTrue(contentType, contentType.startsWith("multipart/mixed"));
        // we need to copy first the stream into a file otherwise it may happen that
        // javax.mail fail to receive some parts
        // perhaps the stream is no more available when javax.mail need it?
        File tmp = Framework.createTempFile("multipart", ".tmp");
        try (InputStream stream = entity.getContent()) {
            FileUtils.copyInputStreamToFile(stream, tmp);
            // get the input from the saved file
            try (InputStream sfin = new SharedFileInputStream(tmp)) {
                MimeMultipart mp = new MimeMultipart(new InputStreamDataSource(sfin, contentType));
                List<Blob> blobs = new ArrayList<>();
                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart part = mp.getBodyPart(i);
                    blobs.add(getBlob(part));
                }
                return blobs;
            } catch (MessagingException e) {
                throw new IOException(e);
            }
        } finally {
            tmp.delete(); // NOSONAR
        }
    }

    protected Blob getBlob(InputStream stream, String mimeType, String encoding, String contentDisposition)
            throws IOException {
        // simplistic parsing, without full RFC6266 compatibility, this is for tests...
        String filename = null;
        int i = contentDisposition.indexOf("filename=");
        if (i != -1) {
            filename = contentDisposition.substring(i + 9).replaceAll("\"", "");
        }
        Blob blob = Blobs.createBlob(stream, mimeType, encoding);
        blob.setFilename(filename);
        return blob;
    }

    protected <T> T execute(int expectedStatusCode, Function<InputStream, T> streamProcessor) throws IOException {
        try {
            return execute(response -> {
                int statusCode = response.getStatusLine().getStatusCode();
                if (expectedStatusCode == 0 && statusCode == SC_NO_CONTENT) {
                    return null;
                }
                assertEquals(expectedStatusCode == 0 ? SC_OK : expectedStatusCode, statusCode);
                if (statusCode == SC_NO_CONTENT) {
                    return null;
                }
                try (InputStream stream = response.getEntity().getContent()) {
                    return streamProcessor.apply(stream);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    protected <T> T execute(Function<CloseableHttpResponse, T> responseProcessor) throws IOException {
        String url = session.baseURL + operationId;
        if (session.async) {
            url += ASYNC_ADAPTER;
        }
        HttpPost request = new HttpPost(url);
        setupAutomationRequest(request);
        request.setEntity(getBodyEntity());
        if (session.async) {
            return executeAsyncAndPoll(request, responseProcessor);
        } else {
            try (CloseableHttpResponse response = session.client.execute(request)) {
                return responseProcessor.apply(response);
            }
        }
    }

    protected <T> T executeAsyncAndPoll(HttpPost request, Function<CloseableHttpResponse, T> responseProcessor)
            throws IOException {
        String pollUrl;
        try (CloseableHttpResponse response = session.client.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != SC_ACCEPTED) {
                return responseProcessor.apply(response);
            }
            pollUrl = getHeader(response, LOCATION);
            if (pollUrl == null) {
                throw new IOException("202 without Location header");
            }
        }
        // poll loop
        HttpGet pollRequest = new HttpGet(pollUrl);
        session.addAuthentication(pollRequest);
        long deadline = System.nanoTime() + ASYNC_POLL_TIMEOUT.toNanos();
        do {
            HttpContext httpContext = new BasicHttpContext();
            try (CloseableHttpResponse response = session.client.execute(pollRequest, httpContext)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= SC_BAD_REQUEST) {
                    // error, finish processing
                    return responseProcessor.apply(response);
                }
                RedirectLocations locations = (RedirectLocations) httpContext.getAttribute(REDIRECT_LOCATIONS);
                if (locations != null && !locations.isEmpty()) {
                    // was redirected to final URL, finish processing
                    return responseProcessor.apply(response);
                }
            }
            // wait a bit before retrying
            try {
                Thread.sleep(ASYNC_POLL_DELAY.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NuxeoException("interrupted", e);
            }
        } while (System.nanoTime() < deadline);
        // we tried all we can, abort
        throw new IOException("Polling timeout: " + pollUrl);
    }

    public HttpEntity getBodyEntity() throws IOException {
        Map<String, Object> jsonMap = new HashMap<>();
        if (input instanceof Blob || isBlobList(input)) {
            // done later
        } else if (isDocument(input)) {
            JsonNode doc = (JsonNode) input;
            jsonMap.put(INPUT, documentToJsonValue(doc));
        } else if (isDocumentList(input)) {
            @SuppressWarnings("unchecked")
            List<JsonNode> docs = (List<JsonNode>) input;
            jsonMap.put(INPUT, documentsToJsonValue(docs));
        } else if (input instanceof Date) {
            String value = ((Date) input).toInstant().toString();
            Map<String, Object> dateMap = new HashMap<>();
            dateMap.put(ENTITY_TYPE, ENTITY_TYPE_DATE);
            dateMap.put(VALUE, value);
            jsonMap.put(INPUT, dateMap);
        } else if (input != null) {
            jsonMap.put(INPUT, input);
        }
        jsonMap.put(PARAMS, params == null ? Map.of() : params);
        jsonMap.put(CONTEXT, context == null ? Map.of() : context);
        StringWriter writer = new StringWriter();
        try {
            MAPPER.writeValue(writer, jsonMap);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String json = writer.toString();
        if (input == null || jsonMap.containsKey(INPUT)) {
            // input is part of the JSON
            return new StringEntity(json, APPLICATION_JSON);
        }
        if (input instanceof Blob) {
            Blob blob = (Blob) input;
            return blobsToEntity(json, List.of(blob));
        } else if (isBlobList(input)) {
            @SuppressWarnings("unchecked")
            List<Blob> blobs = (List<Blob>) input;
            return blobsToEntity(json, blobs);
        }
        throw new IllegalStateException(input.getClass().getName());
    }

    protected static boolean isBlobList(Object object) {
        if (!(object instanceof List<?>)) {
            return false;
        }
        List<?> list = (List<?>) object;
        if (list.isEmpty()) {
            return false;
        }
        return list.get(0) instanceof Blob;
    }

    protected boolean isDocument(Object object) {
        if (!(object instanceof JsonNode)) {
            return false;
        }
        JsonNode node = (JsonNode) object;
        return ENTITY_TYPE_DOCUMENT.equals(getEntityType(node));
    }

    protected boolean isDocumentList(Object object) {
        if (!(object instanceof List<?>)) {
            return false;
        }
        List<?> list = (List<?>) object;
        if (list.isEmpty()) {
            return false;
        }
        return isDocument(list.get(0));
    }

    protected static Object documentToJsonValue(JsonNode doc) {
        return Map.of(ENTITY_TYPE, ENTITY_TYPE_DOCUMENT, "uid", doc.get("uid").asText());
    }

    protected static Object documentsToJsonValue(List<JsonNode> docs) {
        String ids = docs.stream().map(doc -> doc.get("uid").asText()).collect(Collectors.joining(","));
        return "docs:" + ids;
    }

    protected HttpEntity blobsToEntity(String json, List<Blob> blobs) throws IOException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("json", json, APPLICATION_JSON);
        // use a simple byte array in tests
        for (Blob blob : blobs) {
            String filename = blob.getFilename();
            if (filename == null) {
                filename = "file.bin";
            }
            builder.addBinaryBody("content", new ByteArrayInputStream(blob.getByteArray()),
                    ContentType.create(blob.getMimeType(), blob.getEncoding()), filename);
        }
        return builder.build();
    }

    public String login(int expectedStatusCode) throws IOException {
        HttpPost request = new HttpPost(session.baseURL + "login");
        setupAutomationRequest(request);
        try (CloseableHttpResponse response = session.client.execute(request)) {
            assertEquals(expectedStatusCode, response.getStatusLine().getStatusCode());
            if (expectedStatusCode == SC_OK) {
                try (InputStream stream = response.getEntity().getContent()) {
                    JsonNode node = MAPPER.readTree(stream);
                    assertEquals(ENTITY_TYPE_LOGIN, getEntityType(node));
                    return node.get("username").asText();
                }
            } else {
                return null;
            }
        }
    }

    public Blob getFile(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        session.addAuthentication(request);
        try (CloseableHttpResponse response = session.client.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == SC_NO_CONTENT) {
                return null;
            }
            assertEquals(SC_OK, statusCode);
            return getBlob(response);
        }
    }

}
