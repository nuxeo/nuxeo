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
package org.nuxeo.ecm.management.administrativestatus.web;

import static org.nuxeo.ecm.platform.management.web.utils.PlatformManagementWebConstants.ADMINISTRATIVE_STATUS_WEB_OBJECT_TYPE;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * 
 * Web object implementation corresponding to the administraive status of the server
 * 
 * @author mcedica
 * 
 */
@WebObject(type = ADMINISTRATIVE_STATUS_WEB_OBJECT_TYPE , administrator=Access.GRANT)
@Produces("text/html; charset=UTF-8")
public class AdministrativeStatus extends DefaultObject {

	org.nuxeo.ecm.platform.management.statuses.AdministrativeStatus administrativeStatus;

    @Override
    public void initialize(Object... args) {
        super.initialize(args);
        try {
            administrativeStatus = getAdministrativeStatus();
        } catch (Exception e) {
        }
    }

    @GET
    public Object doGet() {
        WebContext context = WebEngine.getActiveContext();
        CoreSession session = context.getCoreSession();
        try {
            return getView("administrative-status").arg("serverInstanceId",
                    getAdministrativeStatus().getServerInstanceName()).arg(
                    "administrativeStatus",
                    getAdministrativeStatus().getValue());
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    @POST
    @Path("lock")
    @Produces("text/html")
    public Object lockServer() {
        try {
            getAdministrativeStatus().setPassive();
            return getView("administrative-status").arg("serverInstanceId",
                    getAdministrativeStatus().getServerInstanceName()).arg(
                    "administrativeStatus",
                    getAdministrativeStatus().getValue());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @POST
    @Path("unlock")
    @Produces("text/html")
    public Object unlockServer() {
        try {
            getAdministrativeStatus().setActive();
            return getView("administrative-status").arg("serverInstanceId",
                    getAdministrativeStatus().getServerInstanceName()).arg(
                    "administrativeStatus",
                    getAdministrativeStatus().getValue());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    private org.nuxeo.ecm.platform.management.statuses.AdministrativeStatus getAdministrativeStatus()
            throws ClientException {
        if (administrativeStatus == null) {
            try {
                administrativeStatus = Framework.getService(org.nuxeo.ecm.platform.management.statuses.AdministrativeStatus.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return administrativeStatus;
    }
}
