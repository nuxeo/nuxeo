/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client;

import java.util.Map;

import org.nuxeo.ecm.automation.client.model.Blob;
import org.nuxeo.ecm.automation.client.model.Blobs;
import org.nuxeo.ecm.automation.client.model.OperationDocumentation;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface Session {

    /**
     * Get the client that created this session.
     *
     * @return the client. cannot be null.
     */
    AutomationClient getClient();

    /**
     * Get the login used to authenticate against the server
     *
     * @return the login. cannot be null.
     */
    LoginInfo getLogin();

    /**
     * Create a new operation request given an operation ID.
     *
     * @param id the ID of the operation to be executed.
     * @return the operation request
     */
    OperationRequest newRequest(String id) throws Exception;

    /**
     * Create a new operation request given an operation ID and an operation
     * context map.
     *
     * @param id the operation id
     * @param ctx the context map to be used when executing the operation on
     *            the server.
     * @return the operation request
     */
    OperationRequest newRequest(String id, Map<String, Object> ctx)
            throws Exception;

    Object execute(OperationRequest request) throws Exception;

    void execute(OperationRequest request, AsyncCallback<Object> cb);

    /**
     * Get a file from the server given a path identifying the file.
     *
     * @param path the file path
     * @return a blob representation of the file
     */
    Blob getFile(String path) throws Exception;

    /**
     * Get a collection of files from the server given the path identifying the
     * collection.
     *
     * @param path the file path
     * @return a collection of files represented as blobs.
     */
    Blobs getFiles(String path) throws Exception;

    void getFile(String path, AsyncCallback<Blob> cb) throws Exception;

    void getFiles(String path, AsyncCallback<Blobs> cb) throws Exception;

    OperationDocumentation getOperation(String id);

    Map<String, OperationDocumentation> getOperations();

    /**
     * Get an adapter of the current session. Adapters can be used to define
     * custom API over a Nuxeo Automation Session.
     * <p>
     * Optional operation. Environments that cannot support this method (like
     * GWT) must throw {@link UnsupportedOperationException}
     *
     * @see AutomationClient#getAdapter(Object, Class)
     */
    <T> T getAdapter(Class<T> type);

    /**
     * Remove any resources held by this session. The session will no more be used again.
     */
    void close();
}
