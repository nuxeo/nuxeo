/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.restapi.server.jaxrs.resource.wro;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * Hook on REST API URLs (just forwards to the Wro servlet).
 *
 * @since 7.3
 */
@WebObject(type = "resource")
public class ResourceBundleEndpoint extends DefaultObject {

    @GET
    @Path("bundle/{var:.*}")
    public Object redirect() {
        return new ResourceBundleDispatcher();
    }

    /**
     * Phony class to handle forward to servlet.
     */
    public class ResourceBundleDispatcher {
    }

}
