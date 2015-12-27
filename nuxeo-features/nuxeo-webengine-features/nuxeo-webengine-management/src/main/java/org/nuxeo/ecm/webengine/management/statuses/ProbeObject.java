/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     mcedica
 */
package org.nuxeo.ecm.webengine.management.statuses;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import org.nuxeo.ecm.core.management.api.ProbeInfo;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * List and execute a probe
 *
 * @author matic
 */
@WebObject(type = "Probe")
public class ProbeObject extends DefaultObject {

    private ProbeInfo info;

    public static ProbeObject newProbe(DefaultObject parent, ProbeInfo info) {
        return (ProbeObject) parent.newObject("Probe", info);
    }

    @Override
    protected void initialize(Object... args) {
        assert args != null && args.length == 1;
        info = (ProbeInfo) args[0];
    }

    @GET
    public Object doGet() {
        return getView("index");
    }

    @PUT
    public Object doPut() {
        return doRun();
    }

    /**
     * For easier invocation using links.
     */
    @GET
    @Path("/@run")
    public Object doRun() {
        ProbeManager probeMgr = Framework.getLocalService(ProbeManager.class);
        probeMgr.runProbe(info);
        return redirect(getPath());
    }

    public ProbeInfo getInfo() {
        return info;
    }

}
