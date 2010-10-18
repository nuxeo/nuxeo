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

import javax.transaction.xa.Xid;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.ProtocolException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.nuxeo.common.utils.XidImpl;
import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Mapper.Identification;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.ServerDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;

/**
 * Mapper sending calls to a remote {@link NetServer}.
 */
public class MapperClient implements InvocationHandler {

    public static Mapper getMapper(RepositoryImpl repository, Credentials credentials)
            throws StorageException {
        MapperClient handler = new MapperClient(repository, credentials);
        Mapper mapper = (Mapper) Proxy.newProxyInstance(
                MapperClient.class.getClassLoader(),
                new Class<?>[] { Mapper.class }, handler);
        synchronized (repository) {
            handler.repositoryId = repository.repositoryId;
            Identification id = mapper.getIdentification();
            handler.identification = id;
            repository.repositoryId = id.repositoryId;
        }
        return mapper;
    }

    private enum Eof {
        VALUE; // used as singleton
    }

    public static final Object EOF = Eof.VALUE;

    protected String repositoryId;

    protected Identification identification;

    protected final String url;

    protected final HttpClient httpClient;

    protected final Header httpPrincipalHeader; // TODO should be replaced by an identification context in mapper

    protected MapperClient(RepositoryImpl repository, Credentials credentials) {
        httpClient = repository.getHttpClient();
        RepositoryDescriptor desc = repository.getRepositoryDescriptor();
        url = getUrl(desc);
        httpPrincipalHeader = getHttpPrincipalHeader(credentials);
    }


    protected static Header getHttpPrincipalHeader(Credentials credentials) {
        String username = "[unknown]";
        if (credentials != null) {
            username = credentials.getUserName();
        }
        return  new Header("X-Nuxeo-Principal", username);
    }

    protected static String getUrl(RepositoryDescriptor repositoryDescriptor) {
        ServerDescriptor sd = repositoryDescriptor.connect.get(0);
        return sd.getUrl() + '/' + RepositoryImpl.SERVER_PATH_VCS;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        String methodName = method.getName();
        // special cases
        if (Mapper.GET_IDENTIFICATION.equals(methodName)) {
            if (identification != null) {
                return identification;
            }
            // else fall through (send to remote)
        } else if ("getTableSize".equals(methodName)) {
            return Integer.valueOf(getTableSize((String) args[0]));
        } else if ("createDatabase".equals(methodName)) {
            createDatabase();
            return null;
        }

        // copying the transaction id implementation object that may not be
        // known by the class loader on server side.
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Xid) {
                    args[i] = new XidImpl((Xid) args[i]);
                }
            }
        }

        // send through network
        // this is decoded by NetServlet

        String postUrl = url;
        if (identification != null) {
            postUrl += '?' + MapperServlet.PARAM_RID + '='
                    + identification.repositoryId + '&'
                    + MapperServlet.PARAM_MID + '=' + identification.mapperId;
        } else if (repositoryId != null) {
            postUrl += '?' + MapperServlet.PARAM_RID + '=' + repositoryId;
        }
        PostMethod m = new PostMethod(postUrl);
        m.setRequestHeader(httpPrincipalHeader);
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
