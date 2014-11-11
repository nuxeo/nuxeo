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
package org.nuxeo.ecm.automation.client.jaxrs;

import java.util.Map;

import org.nuxeo.ecm.automation.client.jaxrs.model.OperationInput;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface OperationRequest {

    Session getSession();

    String getUrl();

    OperationRequest setInput(OperationInput input);

    OperationInput getInput();

    OperationRequest set(String key, Object value);

    OperationRequest setContextProperty(String key, String value);

    Object execute() throws Exception;

    void execute(AsyncCallback<Object> cb);

    Map<String,String> getParameters();

    Map<String,String> getContextParameters();

    OperationRequest setHeader(String key, String value);

    Map<String, String> getHeaders();

}
