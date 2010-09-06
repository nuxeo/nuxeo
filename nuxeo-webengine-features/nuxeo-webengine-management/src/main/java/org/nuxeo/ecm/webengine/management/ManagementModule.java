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
package org.nuxeo.ecm.webengine.management;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.management.auth.PrincipalObject;
import org.nuxeo.ecm.webengine.management.locks.LocksObject;
import org.nuxeo.ecm.webengine.management.queues.QueuesObject;
import org.nuxeo.ecm.webengine.management.statuses.StatusesObject;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * Web object implementation corresponding to the root module for management
 * (module used for administrative purpose).
 *
 * @author mcedica
 */
@WebObject(type = "Management")
@Produces("text/html; charset=UTF-8")
public class ManagementModule extends ManagementObject {

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(ManagementModule.class);

    @GET
    public Object doGet() {
        return getView("index");
    }

    @Path("locks")
    public Object dispatchLock() {
        return LocksObject.newObject(this);
    }

    @Path("statuses")
    public Object dispatchStatuses() {
        return StatusesObject.newObject(this);
    }

    @Path("queues")
    public Object dispatchQueues() {
        return QueuesObject.newObject(this);
    }

    @Path("principal")
    public Object dispatchPrincipal() {
        return PrincipalObject.newObject(this);
    }

}