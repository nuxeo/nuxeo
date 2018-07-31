/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Mariana Cedica
 */
package org.nuxeo.adobe.cc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 9.10
 */
public class NuxeoAdobeConnectorServiceImpl extends DefaultComponent implements NuxeoAdobeConnectorService {

    private static Log log = LogFactory.getLog(NuxeoAdobeConnectorService.class);

    /**
     * Component activated notification. Called when the component is activated. All component dependencies are resolved
     * at that moment. Use this method to initialize the component.
     *
     * @param context the component context.
     */
    @Override
    public void activate(ComponentContext context) {
        super.activate(context);
    }

    /**
     * Component deactivated notification. Called before a component is unregistered. Use this method to do cleanup if
     * any and free any resources held by the component.
     *
     * @param context the component context.
     */
    @Override
    public void deactivate(ComponentContext context) {
        super.deactivate(context);
    }

    /**
     * Application started notification. Called after the application started. You can do here any initialization that
     * requires a working application (all resolved bundles and components are active at that moment)
     *
     * @param context the component context. Use it to get the current bundle context
     * @throws Exception
     */
    @Override
    public void applicationStarted(ComponentContext context) {
        registerAdobeCCClient();

    }

    protected void registerAdobeCCClient() {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Framework.doPrivileged(() -> {
            try (Session session = ds.open(OAuth2ClientService.OAUTH2CLIENT_DIRECTORY_NAME)) {

                Map<String, Serializable> filter = new HashMap<>();
                filter.put("clientId", ADOBE_CC_CLIENT_ID);
                DocumentModelList res = session.query(filter);
                if (res == null || res.size() == 0) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("clientId", ADOBE_CC_CLIENT_ID);
                    data.put("name", ADOBE_CC_NAME);
                    data.put("redirectURIs", ADOBE_CC_CLIENT_URL);
                    data.put("autoGrant", true);
                    session.createEntry(data);
                }

            } catch (DirectoryException e) {
                log.error("Error during registering adobe cc app", e);
            }
        });
    }

}
