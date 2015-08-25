package org.nuxeo.scim.server.jaxrs.marshalling;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.unboundid.scim.data.GroupResource;

public class GroupResponse {

    public static Response created(GroupResource groupResource, MediaType mt) {
        return Response.status(Response.Status.CREATED).header("Content-Type",
                mt.toString()).entity(groupResource).build();

    }

    
    public static Response updated(GroupResource groupResource, MediaType mt) {
        return Response.status(Response.Status.OK).header("Content-Type",
                mt.toString()).entity(groupResource).build();
    }

}
