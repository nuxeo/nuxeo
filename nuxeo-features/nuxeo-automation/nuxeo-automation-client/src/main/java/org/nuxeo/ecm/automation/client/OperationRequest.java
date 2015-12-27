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

import org.nuxeo.ecm.automation.client.model.OperationDocumentation;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// TODO: comment me.
public interface OperationRequest {

    Session getSession();

    String getUrl();

    /**
     * Get the ID of the operation to be invoked
     *
     * @return
     */
    OperationDocumentation getOperation();

    OperationRequest setInput(Object input);

    Object getInput();

    OperationRequest set(String key, Object value);

    OperationRequest setContextProperty(String key, Object value);

    Object execute() throws IOException;

    Map<String, Object> getParameters();

    Map<String, Object> getContextParameters();

    OperationRequest setHeader(String key, String value);

    Map<String, String> getHeaders();

}
