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
            HttpClient httpClient) throws StorageException {
        NetMapper handler = new NetMapper(repository, httpClient);
        Mapper mapper = (Mapper) Proxy.newProxyInstance(
                NetMapper.class.getClassLoader(),
                new Class<?>[] { Mapper.class }, handler);
        handler.mapperId = mapper.getMapperId();
        return mapper;
    }

    private enum Eof {
        VALUE; // used as singleton
    }

    public static final Object EOF = Eof.VALUE;

    protected String mapperId;

    protected final String url;

    protected final HttpClient httpClient;

    protected NetMapper(RepositoryImpl repository, HttpClient httpClient) {
        this.httpClient = httpClient;
        url = getUrl(repository);
    }

    protected static String getUrl(RepositoryImpl repository) {
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
        return uri.toString();
    }

    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        String methodName = method.getName();
        // special cases
        if ("getMapperId".equals(methodName)) {
            if (mapperId != null) {
                return mapperId;
            }
            // else fall through (send to remote)
        } else if ("getTableSize".equals(methodName)) {
            return Integer.valueOf(getTableSize((String) args[0]));
        } else if ("createDatabase".equals(methodName)) {
            createDatabase();
            return null;
        }

        // send through network
        // this is decoded by NetServlet

        String postUrl = url;
        if (mapperId != null) {
            postUrl += "?sid=" + mapperId;
        }
        PostMethod m = new PostMethod(postUrl);
        try {
            ObjectWriterRequestEntity writer = new ObjectWriterRequestEntity();
            writer.add(methodName, args);
            m.setRequestEntity(writer);
            int status = httpClient.executeMethod(m);
            if (status != HttpStatus.SC_OK) {
                throw new ProtocolException(String.valueOf(status));
            }
            String cs = m.getResponseCharSet();
            if (cs != null && !cs.equals("ISO-8859-1")) {
                throw new RuntimeException("Bad encoding: " + cs);
            }
            Object res = new ObjectInputStream(m.getResponseBodyAsStream()).readObject();
            if (res instanceof Throwable) {
                Throwable t = (Throwable) res;
                throw new StorageException("Remote exception: " + t, t);
            } else {
                return res;
            }
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException(e);
        } finally {
            m.releaseConnection();
        }
    }

    public int getTableSize(String tableName) {
        return 5; // TODO get from remote
    }

    public void createDatabase() {
        // do not create, remote did it
    }

}
