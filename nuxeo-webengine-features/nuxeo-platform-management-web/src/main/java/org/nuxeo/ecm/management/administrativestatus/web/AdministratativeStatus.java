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
import org.nuxeo.ecm.management.administrativestatus.service.AdministrativeStatusService;
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
public class AdministratativeStatus extends DefaultObject {

    AdministrativeStatusService administrativeStatusService;

    @Override
    public void initialize(Object... args) {
        super.initialize(args);
        try {
            administrativeStatusService = getAdministrativeStatusService();
        } catch (Exception e) {
        }
    }

    @GET
    public Object doGet() {
        WebContext context = WebEngine.getActiveContext();
        CoreSession session = context.getCoreSession();
        try {
            return getView("administrative-status").arg("serverInstanceId",
                    getAdministrativeStatusService().getServerInstanceName()).arg(
                    "administrativeStatus",
                    getAdministrativeStatusService().getServerStatus(session));
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    @POST
    @Path("lock")
    @Produces("text/html")
    public Object lockServer() {
        try {
            CoreSession session = WebEngine.getActiveContext().getCoreSession();
            getAdministrativeStatusService().lockServer(session);
            return getView("administrative-status").arg("serverInstanceId",
                    getAdministrativeStatusService().getServerInstanceName()).arg(
                    "administrativeStatus",
                    getAdministrativeStatusService().getServerStatus(session));
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @POST
    @Path("unlock")
    @Produces("text/html")
    public Object unlockServer() {
        try {
            CoreSession session = WebEngine.getActiveContext().getCoreSession();
            getAdministrativeStatusService().unlockServer(session);
            return getView("administrative-status").arg("serverInstanceId",
                    getAdministrativeStatusService().getServerInstanceName()).arg(
                    "administrativeStatus",
                    getAdministrativeStatusService().getServerStatus(session));
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    private AdministrativeStatusService getAdministrativeStatusService()
            throws ClientException {
        if (administrativeStatusService == null) {
            try {
                administrativeStatusService = Framework.getService(AdministrativeStatusService.class);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }
        return administrativeStatusService;
    }
}
