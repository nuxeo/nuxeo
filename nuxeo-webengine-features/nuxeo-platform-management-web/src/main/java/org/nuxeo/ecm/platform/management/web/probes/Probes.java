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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;

import org.nuxeo.ecm.platform.management.probes.ProbeInfo;
import org.nuxeo.ecm.platform.management.probes.ProbeRunner;
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

import static org.nuxeo.ecm.platform.management.web.utils.PlatformManagementWebConstants.PROBES_WEB_OBJECT_TYPE;


/**
 * Runs the contributed probs if any
 * */
@WebObject(type = PROBES_WEB_OBJECT_TYPE , administrator=Access.GRANT)
public class Probes extends DefaultObject {

    private ProbeRunner probeRunner;

    @Override
    protected void initialize(Object... args) {
        assert args != null && args.length > 0;
        probeRunner = (ProbeRunner) args[0];
    }

    @GET
    public Object doGet() {
        // run all probes and then display status for each one
        if (probeRunner == null) {
            return getView(getErrorViewName()).args(getNoProbesErrorArguments());
        }
        probeRunner.run();
        List<ProbeInfo> succededProbes = new ArrayList<ProbeInfo>();
        succededProbes.addAll(probeRunner.getRunWithSucessProbesInfo());
        return getView("run-all-probes").arg("probes_in_error",
                probeRunner.getProbesInError()).arg("probes_succeded",
                succededProbes);

    }

    private Map<String, Object> getNoProbesErrorArguments() {
        Map<String, Object> errorArguments = new HashMap<String, Object>();
        errorArguments.put("probe_name", "there are no registered probes");
        return errorArguments;
    }

    private String getErrorViewName() {
        return "run-probe-error";
    }
}
