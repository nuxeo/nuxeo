/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.rest.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 *
 *
 * @since 5.7.3
 */
@WebObject(type = "user")
@Produces({ MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_JSON + "+nxentity" })
public class UserObject extends DefaultObject {

    protected NuxeoPrincipal principal;

    @Override
    protected void initialize(Object... args) {
        if (args.length < 1) {
            throw new IllegalArgumentException(
                    "UserObject takes at least one parameter");
        }

        principal = (NuxeoPrincipal) args[0];
    }

    @GET
    public NuxeoPrincipal doGetUser() {
        return principal;
    }

}
