/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour Al Kotob
 */

package org.nuxeo.ecm.restapi.server.jaxrs.management;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.management.api.ProbeInfo;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.3
 */
@WebObject(type = ManagementObject.MANAGEMENT_OBJECT_PREFIX + "probes")
@Produces(APPLICATION_JSON)
public class ProbesObject extends AbstractResource<ResourceTypeImpl> {

    /**
     * Gets the info of a specific probe.
     *
     * @param probeName the shortcut name of the probe to get
     * @return a {@link ProbeInfo}
     */
    @GET
    @Path("{probeName}")
    public ProbeInfo doGet(@PathParam("probeName") String probeName) {
        ProbeInfo probeInfo = Framework.getService(ProbeManager.class).getProbeInfo(probeName);
        return checkProbe(probeName, probeInfo);
    }

    /**
     * Gets all the infos of all probes.
     *
     * @return a list of all {@link ProbeInfo}
     */
    @GET
    public List<ProbeInfo> doGet() {
        return new ArrayList<>(Framework.getService(ProbeManager.class).getAllProbeInfos());
    }

    /**
     * Launches a specific probe.
     *
     * @param probeName the shortcut name of the probe to launch
     * @return the result of the probe in a {@link ProbeInfo}
     */
    @POST
    @Path("{probeName}")
    public ProbeInfo launch(@PathParam("probeName") String probeName) {
        ProbeInfo probeInfo = Framework.getService(ProbeManager.class).runProbe(probeName);
        return checkProbe(probeName, probeInfo);
    }

    protected ProbeInfo checkProbe(String probeName, ProbeInfo probeInfo) {
        if (probeInfo == null) {
            throw new NuxeoException("No such probe: " + probeName, SC_NOT_FOUND);
        }
        return probeInfo;
    }

}
