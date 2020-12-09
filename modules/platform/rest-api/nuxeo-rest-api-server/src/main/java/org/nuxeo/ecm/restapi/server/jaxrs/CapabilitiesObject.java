/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.restapi.server.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.capabilities.Capabilities;
import org.nuxeo.runtime.capabilities.CapabilitiesService;

/**
 * @since 11.5
 */
@WebObject(type = "capabilities")
public class CapabilitiesObject extends DefaultObject {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Capabilities getInfo() {
        return Framework.getService(CapabilitiesService.class).getCapabilities();
    }
}
