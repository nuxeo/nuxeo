/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.sql.Binary;
import org.nuxeo.ecm.core.storage.sql.BinaryGarbageCollector;
import org.nuxeo.ecm.core.storage.sql.BinaryManager;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.ServerDescriptor;

/**
 * A Binary Manager that uses a local {@link BinaryManager} as a cache but also
 * passes calls to a remote {@link BinaryManagerServlet} for writes and cache
 * misses.
 */
public class BinaryManagerClient implements BinaryManager {

    private static final Log log = LogFactory.getLog(BinaryManagerClient.class);

    protected final BinaryManager binaryManager;

    protected final HttpClient httpClient;

    protected String url;

    public BinaryManagerClient(BinaryManager binaryManager,
            HttpClient httpClient) {
        this.binaryManager = binaryManager;
        this.httpClient = httpClient;
    }

    @Override
    public void initialize(RepositoryDescriptor repositoryDescriptor)
            throws IOException {
        url = getUrl(repositoryDescriptor);
    }

    @Override
    public BinaryGarbageCollector getGarbageCollector() {
        // cannot GC for a remote repository
        return null;
    }

    protected static String getUrl(RepositoryDescriptor repositoryDescriptor) {
        ServerDescriptor sd = repositoryDescriptor.connect.get(0);
        return sd.getUrl() + '/' + RepositoryImpl.SERVER_PATH_BINARY;
    }

    @Override
    public Binary getBinary(InputStream in) throws IOException {
        Binary binary = binaryManager.getBinary(in);

        // also write to remote
        PostMethod m = new PostMethod(url + getQuery(binary.getDigest()));
        try {
            BinaryRequestEntity writer = new BinaryRequestEntity(binary);
            m.setRequestEntity(writer);
            int status = httpClient.executeMethod(m);
            if (status != HttpStatus.SC_CREATED) {
                log.error(String.format(
                        "Could not create remote binary on server %s (%s)",
                        url, String.valueOf(status)));
            }
        } catch (IOException e) {
            log.error(String.format(
                    "Could not create remote binary on server %s (%s)", url,
                    e.toString()), e);
        } finally {
            m.releaseConnection();
        }

        return binary;
    }

    @Override
    public Binary getBinary(String digest) {
        Binary binary = binaryManager.getBinary(digest);
        if (binary != null) {
            return binary;
        }

        GetMethod m = new GetMethod(url + getQuery(digest));
        try {
            int status = httpClient.executeMethod(m);
            if (status == HttpStatus.SC_NOT_FOUND) {
                return null;
            } else if (status != HttpStatus.SC_OK) {
                log.error(String.format(
                        "Could not get remote binary on server %s (%s)", url,
                        String.valueOf(status)));
                return null;
            } else {
                binary = binaryManager.getBinary(m.getResponseBodyAsStream());
            }
        } catch (IOException e) {
            log.error(String.format(
                    "Could not get remote binary on server %s (%s)", url,
                    e.toString()), e);
            return null;
        } finally {
            m.releaseConnection();
        }

        if (binary.getDigest().equals(digest)) {
            return binary;
        } else {
            log.error("Remote binary digest  mismatch: '" + digest + "' vs '"
                    + binary.getDigest() + "'");
            return null;
        }
    }

    protected static String getQuery(String digest) {
        try {
            digest = URLEncoder.encode(digest, "UTF-8");
        } catch (Exception e) {
            // cannot happen
        }
        return "?digest=" + digest;
    }

}
