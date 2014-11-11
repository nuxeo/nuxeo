/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.management.probes.ProbeInfo;
import org.nuxeo.ecm.platform.management.probes.ProbeRunner;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Runs the contributed probs if any
 * */
@WebObject(type = "ha")
@Produces("text/html; charset=UTF-8")
public class AllProbes extends DefaultObject {

    private static final Log log = LogFactory.getLog(AllProbes.class);

    private ProbeRunner probeRunner;

    @Override
    public void initialize(Object... args) {
        try {
            probeRunner = getProbeRunner();
        } catch (Exception e) {
            log.error("Unable to retreive the probeRunner", e);
        }
    }

    @GET
    public Object doGet() {
        return dispatch("/");
    }

    @GET
    @Path("{probeName}")
    public Object dispatch(@PathParam("probeName") String path) {
        try {
            if (probeRunner == null) {
                return getView("run-probe-error").arg("probe_name",
                        "there are no registered probes");
            }
            if ("/".equals(path)) {
                // run all probes and then display status for each one
                probeRunner.run();
                List<ProbeInfo> succededProbes = new ArrayList<ProbeInfo>();
                succededProbes.addAll(probeRunner.getRunWithSucessProbesInfo());
                return getView("run-all-probes").arg("probes_in_error",
                        probeRunner.getProbesInError()).arg("probes_succeded",
                        succededProbes);
            } else {
                // check to see if probe is correctly registered
                if (probeRunner.getProbeNames().contains(path)) {
                    // probe is registered, we should be able to run it
                    ProbeInfo probeInfo = probeRunner.getProbeInfo(path);
                    boolean status = probeRunner.runProbe(probeInfo);
                    return getView("run-probe").arg("probe_status", status).arg(
                            "probe", probeInfo);
                }
            }

            return getView("run-probe-error").arg("probe_name", path);

        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    ProbeRunner getProbeRunner() throws Exception {
        if (probeRunner == null) {
            probeRunner = Framework.getService(ProbeRunner.class);
        }
        return probeRunner;
    }

}