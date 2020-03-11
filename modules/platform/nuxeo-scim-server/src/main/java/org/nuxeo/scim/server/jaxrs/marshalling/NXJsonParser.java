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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.marshal.json.JsonParser;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.InvalidResourceException;

/**
 * Hack to make a method public !
 *
 * @author tiry
 * @since 7.4
 */
public class NXJsonParser extends JsonParser {

    public <R extends BaseResource> R doUnmarshal(
            final JSONObject jsonObject,
            final ResourceDescriptor resourceDescriptor,
            final ResourceFactory<R> resourceFactory,
            final JSONArray defaultSchemas)
            throws JSONException, InvalidResourceException {
        return super.unmarshal(jsonObject, resourceDescriptor, resourceFactory, defaultSchemas);
    }

}
