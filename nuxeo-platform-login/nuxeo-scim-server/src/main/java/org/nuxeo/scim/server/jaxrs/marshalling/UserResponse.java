/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.scim.server.jaxrs.marshalling;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.unboundid.scim.data.UserResource;

/**
 * Helper for handling SCIM {@link UserResource}
 *
 * @author tiry
 * @since 7.4
 */
public class UserResponse {

    public static Response created(UserResource userResource, MediaType mt) {
        return Response.status(Response.Status.CREATED).header("Content-Type", mt.toString()).entity(userResource).build();

    }

    public static Response updated(UserResource userResource, MediaType mt) {
        return Response.status(Response.Status.OK).header("Content-Type", mt.toString()).entity(userResource).build();
    }

}
