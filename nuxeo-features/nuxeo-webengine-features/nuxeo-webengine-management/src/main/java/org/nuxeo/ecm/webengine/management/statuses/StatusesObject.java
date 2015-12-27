/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.webengine.management.statuses;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.nuxeo.ecm.webengine.management.ManagementObject;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * List the statuses
 *
 * @author matic
 */
@WebObject(type = "Statuses")
public class StatusesObject extends ManagementObject {

    public static StatusesObject newObject(DefaultObject parent) {
        return (StatusesObject) parent.newObject("Statuses");
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
