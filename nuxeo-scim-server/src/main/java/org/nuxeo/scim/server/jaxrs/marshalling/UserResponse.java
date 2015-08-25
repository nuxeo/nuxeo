package org.nuxeo.scim.server.jaxrs.marshalling;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.unboundid.scim.data.UserResource;

public class UserResponse {

    public static Response created(UserResource userResource, MediaType mt) {
        return Response.status(Response.Status.CREATED).header("Content-Type",
                mt.toString()).entity(userResource).build();

    }

    
    public static Response updated(UserResource userResource, MediaType mt) {
        return Response.status(Response.Status.OK).header("Content-Type",
                mt.toString()).entity(userResource).build();
    }

}
