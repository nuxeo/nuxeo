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
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.management.api.AdministrativeStatus;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.management.ManagementObject;
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Web object implementation corresponding to the administrative status of the server.
 *
 * @author mcedica
 */
@WebObject(type = "AdministrativeStatus", administrator = Access.GRANT)
@Produces("text/html; charset=UTF-8")
public class AdministrativeStatusObject extends ManagementObject {

    protected AdministrativeStatus administrativeStatus;

    public static AdministrativeStatusObject newAdministrativeStatus(DefaultObject parent) {
        return (AdministrativeStatusObject) parent.newObject("AdministrativeStatus");
    }

    @Override
    public void initialize(Object... args) {
        super.initialize(args);
        AdministrativeStatusManager mgr = Framework.getService(AdministrativeStatusManager.class);
        administrativeStatus = mgr.getNuxeoInstanceStatus();
    }

    @PUT
    public Object doPut() {
        FormData form = ctx.getForm();
        AdministrativeStatusManager manager = Framework.getService(AdministrativeStatusManager.class);
        manager.setNuxeoInstanceStatus(form.getString("status"), "assigned from rest interface",
                ctx.getPrincipal().getName());
        return redirect(getPath());
    }

    @GET
    public Object doGet() {
        return getView("index").arg("serverInstanceId", administrativeStatus.getInstanceIdentifier()).arg(
                "administrativeStatus", administrativeStatus.getState());
    }

    @GET
    @Path("/@activate")
    public Object doActivate() {
        AdministrativeStatusManager manager = Framework.getService(AdministrativeStatusManager.class);
        manager.setNuxeoInstanceStatus(AdministrativeStatus.ACTIVE, "", ctx.getPrincipal().getName());
        return redirect(getPath());
    }

    @GET
    @Path("/@passivate")
    public Object passivate() {
        AdministrativeStatusManager manager = Framework.getService(AdministrativeStatusManager.class);
        manager.setNuxeoInstanceStatus(AdministrativeStatus.PASSIVE, "", ctx.getPrincipal().getName());
        return redirect(getPath());
    }

}
