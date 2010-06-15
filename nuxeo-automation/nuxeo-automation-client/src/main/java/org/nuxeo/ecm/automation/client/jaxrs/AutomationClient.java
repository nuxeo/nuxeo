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

import org.nuxeo.ecm.automation.client.jaxrs.model.OperationDocumentation;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public interface AutomationClient {

    String getBaseUrl();

    void connect(String url) throws Exception;

    void connect(String url, AsyncCallback<AutomationClient> cb);

    public boolean isConnected();

    void disconnect();

    OperationDocumentation getOperation(String id);

    Map<String, OperationDocumentation> getOperations();

    Session getSession(String username, String password) throws Exception;

    void getSession(String username, String password, AsyncCallback<Session> cb);

    /**
     * Adapt the given object to the given type. Return the adapter instance if
     * any otherwise null.
     * <p>
     * Optional operation. Framework that doesn't supports reflection like GWT
     * must throw {@link UnsupportedOperationException}
     * 
     * @param <T>
     * @param objToAdapt
     * @param adapterType
     * @return
     */
    <T> T getAdapter(Object objToAdapt, Class<T> adapterType);

    void registerAdapter(AdapterFactory<?> factory);

}
