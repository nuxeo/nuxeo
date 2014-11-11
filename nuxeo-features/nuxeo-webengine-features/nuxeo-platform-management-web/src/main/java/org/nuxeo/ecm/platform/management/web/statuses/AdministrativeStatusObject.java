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

import static org.nuxeo.ecm.platform.management.web.statuses.Constants.ADMINISTRATIVE_STATUS_WEB_OBJECT_TYPE;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.platform.management.statuses.AdministrativeStatus;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Web object implementation corresponding to the administrative status of the server.
 *
 * @author mcedica
 */
@WebObject(type = ADMINISTRATIVE_STATUS_WEB_OBJECT_TYPE , administrator=Access.GRANT)
@Produces("text/html; charset=UTF-8")
public class AdministrativeStatusObject extends DefaultObject {

    AdministrativeStatus administrativeStatus;

    @Override
    public void initialize(Object... args) {
        super.initialize(args);
        administrativeStatus = Framework.getLocalService(AdministrativeStatus.class);
    }

    @GET
    public Object doGet() {
            return getView("index").
                arg("serverInstanceId", administrativeStatus.getServerInstanceName()).
                arg("administrativeStatus", administrativeStatus.getValue());
    }

    @POST
    @Path("activate")
    @Produces("text/html")
    public Object activate() {
        try {
            administrativeStatus.setActive();
            return redirect(getPath());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @POST
    @Path("passivate")
    @Produces("text/html")
    public Object passivate() {
        try {
            administrativeStatus.setPassive();
            return redirect(getPath());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

}
