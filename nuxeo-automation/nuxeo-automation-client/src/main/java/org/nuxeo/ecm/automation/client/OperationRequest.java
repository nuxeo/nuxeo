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

import org.nuxeo.ecm.automation.client.model.OperationDocumentation;
import org.nuxeo.ecm.automation.client.model.OperationInput;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// TODO: comment me.
public interface OperationRequest {

    Session getSession();

    String getUrl();

    /**
     * Get the ID of the operation to be invoked
     * @return
     */
    OperationDocumentation getOperation();

    OperationRequest setInput(OperationInput input);

    OperationInput getInput();

    OperationRequest set(String key, Object value);

    OperationRequest setContextProperty(String key, Object value);

    Object execute() throws Exception;

    void execute(AsyncCallback<Object> cb);

    Map<String, Object> getParameters();

    Map<String, Object> getContextParameters();

    OperationRequest setHeader(String key, String value);

    Map<String, String> getHeaders();

}
