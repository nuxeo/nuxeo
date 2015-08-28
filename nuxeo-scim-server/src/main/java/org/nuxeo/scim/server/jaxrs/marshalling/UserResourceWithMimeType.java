package org.nuxeo.scim.server.jaxrs.marshalling;

import javax.ws.rs.core.MediaType;

import com.unboundid.scim.data.UserResource;

/**
 * @author tiry
 * @since 7.4
 */
public class UserResourceWithMimeType extends UserResource {

    protected final MediaType mt;

    public UserResourceWithMimeType(UserResource source, MediaType mt) {
        super(source.getResourceDescriptor(), source.getScimObject());
        this.mt=mt;
    }

    public MediaType getMediaType() {
        return mt;
    }

}
