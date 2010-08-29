/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mcedica
 */
package org.nuxeo.ecm.platform.management.web.statuses;

import static org.nuxeo.ecm.platform.management.web.statuses.Constants.PROBES_WEB_OBJECT_TYPE;
import static org.nuxeo.ecm.platform.management.web.statuses.Constants.PROBE_WEB_OBJECT_TYPE;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.management.statuses.ProbeRunner;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;


@WebObject(type = PROBES_WEB_OBJECT_TYPE)
@Produces("text/html; charset=UTF-8")
public class ProbesObject extends DefaultObject {

    private ProbeRunner probeRunner;

    @Override
    protected void initialize(Object... args) {
        assert args != null && args.length > 0;
        probeRunner = (ProbeRunner) args[0];
    }

    @GET
    public Object doGet() {
        return getView("index").arg("probes",
                probeRunner.getProbeInfos());
    }

    @GET
    @Path("availability")
    @Produces("text/plain")
    public Object doGetAvailability() {
        probeRunner.run();
        return getView("availability").arg("isAvailable", probeRunner.getProbesInError().isEmpty());
    }

    @Path("{probe}")
    public Object doGetProbe(@PathParam("probe") String name) {
        return newObject(PROBE_WEB_OBJECT_TYPE, probeRunner, name);
    }

    @POST
    public Object doPost() {
        probeRunner.run();
        return redirect(getPath());
    }
}
