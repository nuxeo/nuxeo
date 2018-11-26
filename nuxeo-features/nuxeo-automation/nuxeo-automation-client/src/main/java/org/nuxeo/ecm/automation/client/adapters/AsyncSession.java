/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */

package org.nuxeo.ecm.automation.client.adapters;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.nuxeo.ecm.automation.client.AutomationClient;
import org.nuxeo.ecm.automation.client.LoginInfo;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.RemoteException;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Connector;
import org.nuxeo.ecm.automation.client.jaxrs.spi.DefaultOperationRequest;
import org.nuxeo.ecm.automation.client.jaxrs.spi.DefaultSession;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshalling;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Request;
import org.nuxeo.ecm.automation.client.jaxrs.util.MultipartInput;
import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.client.model.Blobs;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.model.OperationInput;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.nuxeo.ecm.automation.client.Constants.CTYPE_REQUEST_NOCHARSET;
import static org.nuxeo.ecm.automation.client.Constants.HEADER_NX_SCHEMAS;
import static org.nuxeo.ecm.automation.client.Constants.REQUEST_ACCEPT_HEADER;

/**
 * Asynchronous session adapter.
 * @since 10.3
 */
public class AsyncSession implements Session {

    protected static ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Request providing a completable call method for convenience.
     */
    public class CompletableRequest extends Request {

        protected CompletableFuture<CompletableRequest> future;

        protected int status;

        protected Header[] headers;

        protected Object result;

        protected boolean redirected;

        public CompletableRequest(int method, String url) {
            super(method, url, (String) null);
        }

        public CompletableRequest(int method, String url, String entity) {
            super(method, url, entity);
        }

        public CompletableRequest(int method, String url, MultipartInput input) {
            super(method, url, input);
        }

        @Override
        public Object handleResult(int status, Header[] headers, InputStream stream, HttpContext ctx)
                throws RemoteException, IOException {
            this.status = status;
            this.headers = headers;
            List redirects = (List) ctx.getAttribute(HttpClientContext.REDIRECT_LOCATIONS);
            this.redirected = CollectionUtils.isNotEmpty(redirects);
            try {
                this.result = super.handleResult(status, headers, stream, ctx);
                future.complete(this);
            } catch (RemoteException e) {
                future.completeExceptionally(e);
            }
            return result;
        }

        protected AsyncSession getSession() {
            return AsyncSession.this;
        }

        protected String getHeader(String name) {
            return Request.getHeaderValue(headers, name);
        }

        public CompletableFuture<? extends CompletableRequest> call() {
            future = new CompletableFuture<>();
            try {
                getSession().getConnector().execute(this);
            } catch (IOException e) {
                future.completeExceptionally(e);
            }
            return future;
        }

        public int getStatus() {
            return status;
        }

        public Object getResult() {
            return result;
        }

        public boolean isRedirected() {
            return redirected;
        }
    }

    /**
     * Asynchronous pooling based request
     */
    public class AsyncRequest extends CompletableRequest {

        protected static final String ASYNC_ADAPTER = "/@async";

        public AsyncRequest(int method, String url, String entity) {
            super(method, url + ASYNC_ADAPTER, entity);
        }

        public AsyncRequest(int method, String url, MultipartInput input) {
            super(method, url + ASYNC_ADAPTER, input);
        }

        protected AsyncSession getSession() {
            return AsyncSession.this;
        }

        public CompletableFuture<Object> execute() {
            return call().thenCompose((req) -> {
                if (req.getStatus() == HttpStatus.SC_ACCEPTED) {
                    String location = req.getHeader(HttpHeaders.LOCATION);
                    return poll(location, Duration.ofSeconds(1), Duration.ofSeconds(30));
                }
                return CompletableFuture.completedFuture(req.getResult());
            });
        }

        protected CompletableFuture<Object> poll(String location, Duration delay, Duration duration) {
            CompletableFuture<Object> resultFuture = new CompletableFuture<>();
            long deadline = System.nanoTime() + duration.toNanos();
            CompletableRequest req = new CompletableRequest(Request.GET, location);
            Future pollFuture = executor.submit(() -> {
                do {
                    req.call().thenAccept(res -> {
                        if (req.isRedirected()) {
                            resultFuture.complete(res.getResult());
                        }
                    }).exceptionally(ex -> {
                        resultFuture.completeExceptionally(ex.getCause());
                        return null;
                    });
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException e) {
                        // interrupted when result is complete
                        return;
                    }
                } while (deadline > System.nanoTime());
            });
            resultFuture.whenComplete((result, thrown) -> {
                pollFuture.cancel(true);
            });
            return resultFuture;
        }
    }

    protected final DefaultSession session;

    public AsyncSession(DefaultSession session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    @Override
    public AutomationClient getClient() {
        return session.getClient();
    }

    @Override
    public LoginInfo getLogin() {
        return session.getLogin();
    }

    @Override
    public OperationRequest newRequest(String id) {
        return newRequest(id, new HashMap<>());
    }

    @Override
    public OperationRequest newRequest(String id, Map<String, Object> ctx) {
        OperationDocumentation op = getOperation(id);
        if (op == null) {
            throw new IllegalArgumentException("No such operation: " + id);
        }
        return new DefaultOperationRequest(this, op, ctx);
    }

    @Override
    public Object execute(OperationRequest request) throws IOException {
        AsyncRequest req;
        String content = JsonMarshalling.writeRequest(request);
        String ctype;
        Object input = request.getInput();
        if (input instanceof OperationInput && ((OperationInput) input).isBinary()) {
            MultipartInput mpinput = Request.buildMultipartInput(input, content);
            req = new AsyncRequest(Request.POST, request.getUrl(), mpinput);
            ctype = mpinput.getContentType();
        } else {
            req = new AsyncRequest(Request.POST, request.getUrl(), content);
            ctype = CTYPE_REQUEST_NOCHARSET;
        }
        // set headers
        for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
            req.put(entry.getKey(), entry.getValue());
        }
        req.put(HttpHeaders.ACCEPT, REQUEST_ACCEPT_HEADER);
        req.put(HttpHeaders.CONTENT_TYPE, ctype);
        if (req.get(HEADER_NX_SCHEMAS) == null && session.getDefaultSchemas() != null) {
            req.put(HEADER_NX_SCHEMAS, session.getDefaultSchemas());
        }
        try {
            return req.execute().get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RemoteException) {
                throw (RemoteException) e.getCause();
            }
            throw new IOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Blob getFile(String path) throws IOException {
        return session.getFile(path);
    }

    @Override
    public Blobs getFiles(String path) throws IOException {
        return session.getFiles(path);
    }

    @Override
    public OperationDocumentation getOperation(String id) {
        return session.getOperation(id);
    }

    @Override
    public Map<String, OperationDocumentation> getOperations() {
        return session.getOperations();
    }

    @Override
    public <T> T getAdapter(Class<T> type) {
        return session.getAdapter(type);
    }

    @Override
    public String getDefaultSchemas() {
        return session.getDefaultSchemas();
    }

    @Override
    public void setDefaultSchemas(String defaultSchemas) {
        session.setDefaultSchemas(defaultSchemas);
    }

    @Override
    public void close() {
        session.close();
    }

    public Connector getConnector() {
        return session.getConnector();
    }
}
