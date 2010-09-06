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
package org.nuxeo.ecm.webengine.management.statuses;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.management.statuses.ProbeInfo;
import org.nuxeo.ecm.core.management.statuses.ProbeRunner;
import org.nuxeo.ecm.webengine.management.ManagementObject;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;


@WebObject(type = "Probes")
@Produces("text/html; charset=UTF-8")
public class ProbesObject extends ManagementObject {

    protected ProbeRunner runner;
    protected Collection<ProbeInfo> infos;

    public static ProbesObject newProbes(DefaultObject parent) {
        return (ProbesObject)parent.newObject("Probes");
    }

    @Override
    protected void initialize(Object... args) {
        assert args != null && args.length == 2;
        runner = Framework.getLocalService(ProbeRunner.class);
        infos = runner.getProbeInfos();
    }

    @GET
    public Object doGet() {
        return getView("index");
    }

    public Collection<ProbeInfo> getInfos() {
        return infos;
    }

    @GET
    @Path("availability")
    @Produces("text/plain")
    public Object doGetAvailability() {
        runner.run();
        return getView("availability").arg("isAvailable", runner.getProbesInError().isEmpty());
    }

    @Path("{probe}")
    public ProbeObject doGetProbe(@PathParam("probe") String name) {
        Iterator<ProbeInfo> it = infos.iterator();
        while (it.hasNext()) {
            ProbeInfo info = it.next();
            if (info.getShortcutName().equals(name)) {
                return ProbeObject.newProbe(this, info);
            }
        }
        throw new WebResourceNotFoundException("No such probe " + name);
    }

    @POST
    public Object doPost() {
        runner.run();
        return redirect(getPath());
    }
}
