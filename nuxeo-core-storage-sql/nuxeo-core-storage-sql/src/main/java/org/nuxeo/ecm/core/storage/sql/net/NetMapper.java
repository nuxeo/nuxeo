/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.net;

import java.io.ObjectInputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.ProtocolException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.ServerDescriptor;

/**
 * Mapper sending calls to a remote {@link NetServer}.
 */
public class NetMapper implements InvocationHandler {

    public static Mapper getMapper(RepositoryImpl repository,
            HttpClient httpClient) {
        NetMapper handler = new NetMapper(repository, httpClient);
        Mapper mapper = (Mapper) Proxy.newProxyInstance(
                NetMapper.class.getClassLoader(),
                new Class<?>[] { Mapper.class }, handler);
        handler.mapperId = mapper.getMapperId();
        return mapper;
    }

    private enum Barrier {
        BARRIER_VALUE;
    }

    public static final Object BARRIER = Barrier.BARRIER_VALUE;

    protected String mapperId;

    protected final String url;

    protected final HttpClient httpClient;

    protected boolean inBatch;

    protected List<Op> batch;

    protected static final class Op {
        String methodName;

        Object[] args;

        public Op(String methodName, Object[] args) {
            this.methodName = methodName;
            this.args = args;
        }

        @Override
        public String toString() {
            return "Op(" + methodName + ")";
        }
    }

    protected NetMapper(RepositoryImpl repository, HttpClient httpClient) {
        this.httpClient = httpClient;
        ServerDescriptor sd = repository.getRepositoryDescriptor().connect.get(0);
        String path = sd.path;
        if (!path.startsWith("/")) {
            path = '/' + path;
        }
        if (!path.endsWith("/")) {
            path += '/'; // needed otherwise we get a redirect
        }
        URI uri;
        try {
            uri = new URI("http", null, sd.host, sd.port, path, null, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        url = uri.toString();
    }

    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        String methodName = method.getName();
        // System.out.println(methodName + ' '
        // + (args == null ? "[]" : Arrays.asList(args)));
        Object res = invoke(methodName, args);
        // System.out.println(" -> " + res);
        return res;
    }

    protected Object invoke(String methodName, Object[] args) throws Throwable {
        // special cases
        if (methodName.equals("getMapperId")) {
            if (mapperId != null) {
                return mapperId;
            }
            // else fall through (send to remote)
        } else if (methodName.equals("getTableSize")) {
            return Integer.valueOf(getTableSize((String) args[0]));
        } else if (methodName.equals("beginBatch")) {
            beginBatch();
            return null;
        } else if (methodName.equals("endBatch")) {
            endBatch();
            return null;
        } else if (methodName.equals("createDatabase")) {
            createDatabase();
            return null;
        }

        if (inBatch) {
            addBatch(methodName, args);
            return null;
        } else {
            return invokeOne(methodName, args);
        }
    }

    // send through network
    // this is decoded by NetServlet
    protected Object invokeOne(String methodName, Object[] args)
            throws StorageException {
        return invokeMany(Collections.singletonList(new Op(methodName, args)));
    }

    protected Object invokeMany(List<Op> batch) throws StorageException {
        if (batch.isEmpty()) {
            return null;
        }
        // System.out.println(batch); // debug
        String postUrl = url;
        if (mapperId != null) {
            postUrl += "?sid=" + mapperId;
        }
        PostMethod m = new PostMethod(postUrl);
        try {
            ObjectWriterRequestEntity writer = new ObjectWriterRequestEntity();
            for (Op op : batch) {
                writer.add(op.methodName, op.args);
            }
            m.setRequestEntity(writer);
            int status = httpClient.executeMethod(m);
            if (status != HttpStatus.SC_OK) {
                throw new ProtocolException(String.valueOf(status));
            }
            String cs = m.getResponseCharSet();
            if (cs != null && !cs.equals("ISO-8859-1")) {
                throw new RuntimeException("Bad encoding: " + cs);
            }
            return new ObjectInputStream(m.getResponseBodyAsStream()).readObject();
        } catch (Exception e) {
            throw new StorageException(e);
        } finally {
            m.releaseConnection();
        }
    }

    public void beginBatch() {
        batch = new ArrayList<Op>();
        inBatch = true;
    }

    protected void addBatch(String methodName, Object[] args) {
        batch.add(new Op(methodName, args));
    }

    public void endBatch() throws StorageException {
        try {
            invokeMany(batch);
        } finally {
            batch = null;
            inBatch = false;
        }
    }

    public int getTableSize(String tableName) {
        return 5; // TODO get from remote
    }

    public void createDatabase() {
        // do not create, remote did it
    }

}
