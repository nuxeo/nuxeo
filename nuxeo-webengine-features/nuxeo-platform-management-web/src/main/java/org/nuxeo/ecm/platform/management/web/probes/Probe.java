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
package org.nuxeo.ecm.platform.management.web.probes;

import static org.nuxeo.ecm.platform.management.web.utils.PlatformManagementWebConstants.PROBE_WEB_OBJECT_TYPE;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;

import org.nuxeo.ecm.platform.management.statuses.ProbeInfo;
import org.nuxeo.ecm.platform.management.statuses.ProbeRunner;
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;


/**
 * Runs a specified probe
 * */
@WebObject(type = PROBE_WEB_OBJECT_TYPE, administrator=Access.GRANT)
public class Probe extends DefaultObject {

    private ProbeRunner probeRunner;

    private String probeName;

    @Override
    protected void initialize(Object... args) {
        assert args != null && args.length > 0;
        probeRunner = (ProbeRunner) args[0];
        probeName = (String) args[1];
    }

    @GET
    public Object doGet() {
        if (probeRunner == null) {
            return getView(getErrorViewName()).args(
                    getNoProbesErrorArguments());
        }
        if (probeRunner.getProbeNames().contains(probeName)) {
            // probe is registered, we should be able to run it
            ProbeInfo probeInfo = probeRunner.getProbeInfo(probeName);
            boolean status = probeRunner.runProbe(probeInfo);
            return getView("run-probe").arg("probe_status", status).arg(
                    "probe", probeInfo);
        } else {
            return getView(getErrorViewName()).args(
                    getNoProbeErrorArguments());
        }

    }

    private Map<String, Object> getNoProbesErrorArguments() {
        Map<String, Object> errorArguments = new HashMap<String, Object>();
        errorArguments.put("probe_name", "there are no registered probes");
        return errorArguments;
    }

    private Map<String, Object> getNoProbeErrorArguments() {
        Map<String, Object> errorArguments = new HashMap<String, Object>();
        errorArguments.put("probe_name", probeName);
        return errorArguments;
    }

    private String getErrorViewName() {
        return "run-probe-error";
    }
}
