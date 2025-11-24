/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Antoine Taillefer
 */
package org.nuxeo.scim.v2.rest;

import java.util.Set;

import org.nuxeo.ecm.webengine.app.JsonNuxeoExceptionWriter;
import org.nuxeo.ecm.webengine.app.WebEngineModule;
import org.nuxeo.ecm.webengine.app.jersey.WebEngineServlet;

import com.unboundid.scim2.server.resources.ResourceTypesEndpoint;
import com.unboundid.scim2.server.resources.SchemasEndpoint;

/**
 * WebEngine module for SCIM 2.0.
 * <p>
 * The {@link WebEngineServlet} is mapped to /scim/v2/* in deployment-fragment.xml.
 * <p>
 * ScimV2 only uses the scanning and declaring mechanisms of WebEngine, otherwise it is a standard Jakarta RS
 * application.
 *
 * @since 2023.14
 */
public class ScimV2Module extends WebEngineModule {

    @Override
    public Set<Class<?>> getClasses() {
        // result contains scimV2 resources from module scanning
        Set<Class<?>> result = super.getClasses();
        // marshalling
        result.add(JsonNuxeoExceptionWriter.class);
        // resources
        result.add(SchemasEndpoint.class);
        result.add(ResourceTypesEndpoint.class);
        return result;
    }

}
