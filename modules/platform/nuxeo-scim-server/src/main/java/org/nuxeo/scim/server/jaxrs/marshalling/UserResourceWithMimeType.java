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
 *     Thierry Delprat
 */
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
