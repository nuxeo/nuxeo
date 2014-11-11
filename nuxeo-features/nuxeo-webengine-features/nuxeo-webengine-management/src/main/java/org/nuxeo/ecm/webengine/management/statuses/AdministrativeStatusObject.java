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

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.management.api.AdministrativeStatus;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.webengine.WebException;
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
@WebObject(type = "AdministrativeStatus" , administrator=Access.GRANT)
@Produces("text/html; charset=UTF-8")
public class AdministrativeStatusObject extends ManagementObject {

    protected AdministrativeStatus administrativeStatus;

    public static AdministrativeStatusObject newAdministrativeStatus(DefaultObject parent) {
        return (AdministrativeStatusObject)parent.newObject("AdministrativeStatus");
    }

    @Override
    public void initialize(Object... args) {
        super.initialize(args);
        AdministrativeStatusManager mgr = Framework.getLocalService(AdministrativeStatusManager.class);
        administrativeStatus =mgr.getNuxeoInstanceStatus();
    }

    @PUT
    public Object doPut() {
        FormData form = ctx.getForm();
        try {
            AdministrativeStatusManager manager = Framework.getLocalService(AdministrativeStatusManager.class);
            manager.setNuxeoInstanceStatus(form.getString("status"), "assigned from rest interface", ctx.getPrincipal().getName());
            return redirect(getPath());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @GET
    public Object doGet() {
            return getView("index").
                arg("serverInstanceId", administrativeStatus.getInstanceIdentifier()).
                arg("administrativeStatus", administrativeStatus.getState());
    }

    @GET
    @Path("/@activate")
    public Object doActivate() {
        try {
            AdministrativeStatusManager manager = Framework.getLocalService(AdministrativeStatusManager.class);
            manager.setNuxeoInstanceStatus(AdministrativeStatus.ACTIVE, "", ctx.getPrincipal().getName());
            return redirect(getPath());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @GET
    @Path("/@passivate")
    public Object passivate() {
        try {
            AdministrativeStatusManager manager = Framework.getLocalService(AdministrativeStatusManager.class);
            manager.setNuxeoInstanceStatus(AdministrativeStatus.PASSIVE, "", ctx.getPrincipal().getName());
            return redirect(getPath());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

}
