/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs.drive;

import java.io.File;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * @since 9.10
 */
@WebObject(type = "drive")
@Produces(MediaType.APPLICATION_JSON)
public class NuxeoDriveObject extends DefaultObject {

    public static final String NUXEO_DRIVE_CONFIGURATION_FILE = "nuxeo-drive-config.json";

    /**
     * Retrieves the Nuxeo Drive global configuration.
     *
     * @implNote The configuration file is expected to be in the server's configuration folder, copied from the
     *           {@code drive} template.
     */
    @GET
    @Path("configuration")
    public Response getConfiguration() {
        File configurationFolder = Environment.getDefault().getConfig();
        File configurationFile = new File(configurationFolder, NUXEO_DRIVE_CONFIGURATION_FILE);
        if (!configurationFile.exists()) {
            throw new WebResourceNotFoundException("Nuxeo Drive configuration file not found.");
        }
        return Response.ok().entity(configurationFile).type(MediaType.APPLICATION_JSON).build();
    }

}
