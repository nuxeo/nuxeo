/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.scim.server.jaxrs.usermanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.scim.server.jaxrs.marshalling.UserResponse;

import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.sdk.Resources;

/**
 * Simple Resource class used to expose the SCIM API on Users endpoint
 *
 * @author tiry
 * @since 7.4
 */
@WebObject(type = "users")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public class SCIMUserWebObject extends BaseUMObject {

    @Override
    protected String getPrefix() {
        return "/Users";
    }

    protected UserResource resolveUserRessource(String uid) {

        try {
            DocumentModel userModel = um.getUserModel(uid);
            if (userModel != null) {
                return mapper.getUserResourceFromNuxeoUser(userModel);
            }
        } catch (Exception e) {
            log.error("Error while resolving User", e);
        }

        return null;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + "; qs=0.9" })
    public Resources<UserResource> getUsers(@Context UriInfo uriInfo) {

        Map<String, List<String>> params = uriInfo.getQueryParameters();

        // filter
        Map<String, Serializable> filter = new HashMap<>();
        List<String> filters = params.get("filter");
        if (filters != null && filters.size() > 0) {
            String[] filterParts = filters.get(0).split(" ");
            if (filterParts[1].equals("eq")) {
                String key = filterParts[0];
                if (key.equals("userName")) {
                    key = "username";
                }
                String value = filterParts[2];
                if (value.startsWith("\"")) {
                    value = value.substring(1, value.length() - 2);
                }
                filter.put(key, value);
            }
        }

        // sort
        List<String> sortCol = params.get("sortBy");
        List<String> sortType = params.get("sortOrder");
        // XXX mapping
        Map<String, String> orderBy = new HashMap<>();
        if (sortCol != null && sortCol.size() > 0) {
            String order = "asc";
            if (sortType != null && sortType.size() > 0) {
                if (sortType.get(0).equalsIgnoreCase("descending")) {
                    order = "desc";
                }
                orderBy.put(sortCol.get(0), order);
            }
        }
        int startIndex = 1;
        if (params.get("startIndex") != null) {
            startIndex = Integer.parseInt(params.get("startIndex").get(0));
        }
        int count = 10;
        if (params.get("count") != null) {
            count = Integer.parseInt(params.get("count").get(0));
        }

        try {
            String directoryName = um.getUserDirectoryName();

            DirectoryService ds = Framework.getService(DirectoryService.class);

            Session dSession = null;
            DocumentModelList userModels = null;
            try {
                dSession = ds.open(directoryName);
                userModels = dSession.query(filter, null, orderBy, true, count, startIndex - 1);
            } finally {
                dSession.close();
            }

            List<UserResource> userResources = new ArrayList<>();
            for (DocumentModel userModel : userModels) {
                userResources.add(mapper.getUserResourceFromNuxeoUser(userModel));
            }
            return new Resources<>(userResources, userResources.size(), startIndex);
        } catch (Exception e) {
            log.error("Error while getting Users", e);
        }
        return null;
    }

    @Path("{uid}")
    @GET
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public UserResource getUserResource(@Context UriInfo uriInfo, @PathParam("uid") String uid) {
        return resolveUserRessource(uid);

    }

    @Path("{uid}.xml")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public UserResource getUserResourceAsXml(@Context UriInfo uriInfo, @PathParam("uid") String uid) {
        return getUserResource(uriInfo, uid);
    }

    @Path("{uid}.json")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public UserResource getUserResourceAsJSON(@Context UriInfo uriInfo, @PathParam("uid") String uid) {
        return getUserResource(uriInfo, uid);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response createUser(UserResource user) {
        return doCreateUserResponse(user, fixeMediaType);
    }

    protected Response doCreateUserResponse(UserResource user, MediaType mt) {
        checkUpdateGuardPreconditions();
        return UserResponse.created(doCreateUser(user), mt);

    }

    protected UserResource doCreateUser(UserResource user) {

        try {
            DocumentModel newUser = mapper.createNuxeoUserFromUserResource(user);
            UserResource resource = mapper.getUserResourceFromNuxeoUser(newUser);
            return resource;
        } catch (Exception e) {
            log.error("Unable to create User", e);
        }
        return null;
    }

    @PUT
    @Path("{uid}")
    @Consumes({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response updateUser(@Context UriInfo uriInfo, @PathParam("uid") String uid, UserResource user) {
        checkUpdateGuardPreconditions();
        return doUpdateUser(uid, user, fixeMediaType);
    }

    protected Response doUpdateUser(String uid, UserResource user, MediaType mt) {

        try {
            DocumentModel userModel = mapper.updateNuxeoUserFromUserResource(uid, user);
            if (userModel != null) {
                UserResource userResource = mapper.getUserResourceFromNuxeoUser(userModel);
                return UserResponse.updated(userResource, mt);
            }
        } catch (Exception e) {
            log.error("Unable to create User", e);
        }
        return null;
    }

    @Path("{uid}")
    @DELETE
    public Response deleteUserResource(@Context UriInfo uriInfo, @PathParam("uid") String uid) {
        try {
            um.deleteUser(uid);
            return Response.ok().build();
        } catch (DirectoryException e) {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

}
