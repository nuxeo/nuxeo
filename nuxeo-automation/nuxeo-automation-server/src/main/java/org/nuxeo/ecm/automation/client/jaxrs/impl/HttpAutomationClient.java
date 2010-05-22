/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.nuxeo.ecm.automation.client.jaxrs.AsyncCallback;
import org.nuxeo.ecm.automation.client.jaxrs.AuthenticationCallback;
import org.nuxeo.ecm.automation.client.jaxrs.Constants;
import org.nuxeo.ecm.automation.client.jaxrs.OperationRequest;
import org.nuxeo.ecm.automation.client.jaxrs.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.blob.MultipartRequestEntity;
import org.nuxeo.ecm.automation.client.jaxrs.model.Blob;
import org.nuxeo.ecm.automation.client.jaxrs.model.FileBlob;
import org.nuxeo.ecm.automation.client.jaxrs.model.OperationInput;
import org.nuxeo.ecm.automation.client.jaxrs.spi.AbstractAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.spi.DefaultOperationRequest;
import org.nuxeo.ecm.automation.client.jaxrs.spi.DefaultSession;
import org.nuxeo.ecm.automation.client.jaxrs.spi.OperationRegistry;

import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class HttpAutomationClient extends AbstractAutomationClient implements Constants {

    protected static Pattern ATTR_PATTERN = Pattern.compile(";?\\s*filename\\s*=\\s*([^;]+)\\s*", Pattern.CASE_INSENSITIVE);

    protected DefaultHttpClient http;
    protected ExecutorService async;
    protected Map<Class<?>, List<AdapterFactory<?>>> adapters;

    public HttpAutomationClient() {
        http = new DefaultHttpClient();
        http.setCookieSpecs(null);
        http.setCookieStore(null);
        async = Executors.newSingleThreadExecutor();
        adapters = new HashMap<Class<?>, List<AdapterFactory<?>>>();
        registerAdapter(new DocumentServiceFactory());
    }

    public ExecutorService async() {
        return async;
    }

    public HttpClient http() {
        return http;
    }

    @Override
    protected Object execute(OperationRequest req) throws Exception {
        HttpEntity entity = null;
        String ctype = CTYPE_REQUEST;
        String content = JsonMarshalling.writeRequest(req);
        OperationInput input = req.getInput();
        if (input == null || !input.isBinary()) {
            entity = new StringEntity(content, "UTF-8");
        } else {
            MultipartRequestEntity mpentity = new MultipartRequestEntity();
            mpentity.setRequest(content);
            if (input instanceof Blob) {
                mpentity.setBlob((Blob)input);
//            } else if (input instanceof Blobs) {
//                mpentity.setBlobs((Blobs)input);
            }
            mpentity.setChunked(false); // avoid chunked since weird error occurs with javax.mail + chunked=true
            entity = mpentity;
            ctype = mpentity.getContentType().getValue();
        }

        HttpPost post = new HttpPost(req.getUrl());
        post.setHeader("Accept", REQUEST_ACCEPT_HEADER);
        post.setHeader("Content-Type", ctype);
        SessionImpl session = (SessionImpl)((DefaultOperationRequest)req).getSession();
        DefaultSession ds = (DefaultSession)req.getSession();
        if (ds.getAuth() != null) {
            post.setHeader("Authorization", ds.getAuth());
        }
        post.setEntity(entity);
        HttpResponse resp = http.execute(post, session.getContext());
        HttpEntity rentity = resp.getEntity();
        handleException(resp, rentity);
        ctype = rentity.getContentType().getValue().toLowerCase();
        if (ctype.startsWith(CTYPE_MULTIPART)) { // list of blobs
            ArrayList<File> files = new ArrayList<File>();
            //TODO
            return files;
        } else if (ctype.startsWith(CTYPE_ENTITY)) {
            return JsonMarshalling.readEntity(IOUtils.read(rentity.getContent()));
        } else { // a blob?
            File file = IOUtils.copyToTempFile(rentity.getContent());
            file.deleteOnExit();
            FileBlob blob = new FileBlob(file);
            blob.setMimeType(ctype);
            Header[] disp = resp.getHeaders("Content-Disposition");
            if (disp != null && disp.length > 0) {
                String fname = getFileName(disp[0].getValue());
                if (fname != null) {
                    blob.setFileName(fname);
                }
            }
            return blob;
        }
    }

    protected void handleException(HttpResponse resp, HttpEntity entity) throws IOException {
        int code = resp.getStatusLine().getStatusCode();
        if (code >= 400) { // an error
            if (CTYPE_ENTITY.equals(entity.getContentType().getValue())) {
                throw JsonMarshalling.readException(IOUtils.read(entity.getContent()));
            } else {
                throw new RemoteException(code, "ServerError", IOUtils.read(entity.getContent()), null);
            }
        }
    }

    @Override
    protected void execute(final OperationRequest req, final AsyncCallback<Object> cb) {
        async.execute(new Runnable() {
            public void run() {
                try {
                    cb.onSuccess(execute(req));
                } catch (Throwable t) {
                    cb.onError(t);
                }
            }
        });
    }

    protected OperationRegistry getOperationRegistry(String url) throws Exception {
        HttpGet get = new HttpGet(url);
        get.setHeader("Accept", CTYPE_AUTOMATION);
        //TODO remove
        //get.setHeader("Authorization", "Basic "+Base64.encode("Administrator:Administrator"));
        HttpResponse resp = http.execute(get);
        HttpEntity entity = resp.getEntity();
        handleException(resp, entity);
        String content = IOUtils.read(entity.getContent());
        return JsonMarshalling.readRegistry(content);
    }

    @Override
    protected void getOperationRegistry(final String url,
            final AsyncCallback<OperationRegistry> cb) {
        async.execute(new Runnable() {
            public void run() {
                try {
                    cb.onSuccess(getOperationRegistry(url));
                } catch (Throwable t) {
                    cb.onError(t);
                }
            }
        });
    }

    @Override
    public synchronized void disconnect() {
        super.disconnect();
        http.getConnectionManager().shutdown();
        http = null;
    }

    public void shutdown() {
        disconnect();
        try {
            async.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        async = null;
    }


    @Override
    protected Session createSession(AuthenticationCallback cb) {
        String[] c = cb.getCredentials();
        if (c == null) {
            return new SessionImpl(this, null, null);
        }
        return new SessionImpl(this, c[0], c[1]);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Object objToAdapt, Class<T> adapterType) {
        Class<?> cls = objToAdapt.getClass();
        List<AdapterFactory<?>> factories = adapters.get(adapterType);
        if (factories != null) {
            for (AdapterFactory<?> f : factories) {
                if (f.getAcceptType().isAssignableFrom(cls)) {
                    return (T)f.getAdapter(objToAdapt);
                }
            }
        }
        return null;
    }

    /**
     * Register and adapter for a given type.
     * Registration is not thread safe. You should register adapters at initialization time.
     * An adapter type can be bound to a single adaptable type.
     * @param typeToAdapt
     * @param adapterType
     */
    public void registerAdapter(AdapterFactory<?> factory) {
        Class<?> adapter = factory.getAdapterType();
        List<AdapterFactory<?>> factories = adapters.get(adapter);
        if (factories == null) {
            factories = new ArrayList<AdapterFactory<?>>();
            adapters.put(adapter, factories);
        }
        factories.add(factory);
    }

    protected static String getFileName(String ctype) {
        Matcher m = ATTR_PATTERN.matcher(ctype);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

}
