/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Endpoint to list PageProviders.
 *
 * @since 2023.34
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "page-providers")
@Produces(APPLICATION_JSON)
public class PageProvidersObject extends AbstractResource<ResourceTypeImpl> {

    /**
     * Lists the registered page providers.
     */
    @GET
    public String listPageProviders() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode res = mapper.createArrayNode();
        PageProviderService pps = Framework.getService(PageProviderService.class);
        for (String ppName : pps.getPageProviderDefinitionNames()) {
            ObjectNode node = mapper.createObjectNode();
            PageProviderDefinition def = pps.getPageProviderDefinition(ppName);
            // Create an instance so class replacer is taken in account
            PageProvider<?> pp = pps.getPageProvider(ppName, def, null, null, 0L, 0L, null);
            String klass = pp.getClass().getCanonicalName();
            node.put("name", pp.getName());
            node.put("class", klass);
            node.put("maxPageSize", pp.getMaxPageSize());
            res.add(node);
        }
        return res.toString();
    }
}