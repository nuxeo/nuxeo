/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client;

import java.io.IOException;
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
    OperationRequest newRequest(String id);

    /**
     * Create a new operation request given an operation ID and an operation context map.
     *
     * @param id the operation id
     * @param ctx the context map to be used when executing the operation on the server.
     * @return the operation request
     */
    OperationRequest newRequest(String id, Map<String, Object> ctx);

    Object execute(OperationRequest request) throws IOException;

    /**
     * Get a file from the server given a path identifying the file.
     *
     * @param path the file path
     * @return a blob representation of the file
     */
    Blob getFile(String path) throws IOException;

    /**
     * Get a collection of files from the server given the path identifying the collection.
     *
     * @param path the file path
     * @return a collection of files represented as blobs.
     */
    Blobs getFiles(String path) throws IOException;

    OperationDocumentation getOperation(String id);

    Map<String, OperationDocumentation> getOperations();

    /**
     * Get an adapter of the current session. Adapters can be used to define custom API over a Nuxeo Automation Session.
     * <p>
     * Optional operation. Environments that cannot support this method (like GWT) must throw
     * {@link UnsupportedOperationException}
     *
     * @see AutomationClient#getAdapter(Object, Class)
     */
    <T> T getAdapter(Class<T> type);

    /**
     * Get the default schemas that should be sent by the server.
     * <p>
     * This is a comma separated String (ex: dublincore,file)
     * <p>
     * default value is null (let the server decide what to send)
     * <p>
     * when Automation server convert the Documents to JSON, it will use this list to select what properties should be
     * sent. You can explicitly set the X-NXDocumentProperties header at request level. If defaultSchemas, the request
     * that don't already have the header set will inherit the default value.
     *
     * @since 5.7
     */
    String getDefaultSchemas();

    /**
     * Set the default schemas that should be sent by the server.
     * <p>
     * This is a comma separated String (ex: dublincore,file)
     * <p>
     * when Automation server convert the Documents to JSON, it will use this list to select what properties should be
     * sent. You can explicitly set the X-NXDocumentProperties header at request level. If defaultSchemas, the request
     * that don't already have the header set will inherit the default value.
     *
     * @param defaultSchemas list of schemas as a comma separated string
     * @since 5.7
     */
    void setDefaultSchemas(String defaultSchemas);

    /**
     * Remove any resources held by this session. The session will no more be used again.
     */
    void close();
}
