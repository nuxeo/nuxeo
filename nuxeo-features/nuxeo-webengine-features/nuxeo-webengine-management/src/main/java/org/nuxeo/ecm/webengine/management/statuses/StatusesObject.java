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
 *     matic
 */
package org.nuxeo.ecm.webengine.management.statuses;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.nuxeo.ecm.webengine.management.ManagementObject;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 *
 * List the statuses
 *
 * @author matic
 *
 */
@WebObject(type="Statuses")
public class StatusesObject extends ManagementObject {

    public static StatusesObject newObject(DefaultObject parent) {
        return (StatusesObject)parent.newObject("Statuses");
    }

    @GET
    public Object doGet() {
        return getView("index");
    }

    @Path("probes")
    public Object dispatchProbes() {
        return ProbesObject.newProbes(this);
    }

    @Path("admin")
    public Object dispatchAdmin() {
        return AdministrativeStatusObject.newAdministrativeStatus(this);
    }

}
