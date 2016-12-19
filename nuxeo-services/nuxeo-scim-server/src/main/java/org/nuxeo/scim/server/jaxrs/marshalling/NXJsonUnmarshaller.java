/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import static com.unboundid.scim.sdk.StaticUtils.toLowerCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.marshal.json.JsonUnmarshaller;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.InvalidResourceException;

/**
 * Copy of the original scimsdk class just to change some org.json constructors scimsdk uses a custom version of
 * org.json with a different artifactId and some code differences but with the same namespace
 *
 * @author tiry
 * @since 7.4
 */
public class NXJsonUnmarshaller extends JsonUnmarshaller {

    @Override
    public <R extends BaseResource> R unmarshal(final InputStream inputStream,
            final ResourceDescriptor resourceDescriptor, final ResourceFactory<R> resourceFactory)
                    throws InvalidResourceException {
        try {

            String json = IOUtils.toString(inputStream, Charsets.UTF_8);
            final JSONObject jsonObject = makeCaseInsensitive(new JSONObject(new JSONTokener(json)));

            final NXJsonParser parser = new NXJsonParser();
            return parser.doUnmarshal(jsonObject, resourceDescriptor, resourceFactory, null);
        } catch (JSONException | IOException  e) {
            throw new InvalidResourceException("Error while reading JSON: " + e.getMessage(), e);
        }
    }

    protected JSONObject makeCaseInsensitive(final JSONObject jsonObject) throws JSONException {
        if (jsonObject == null) {
            return null;
        }

        Iterator keys = jsonObject.keys();
        Map lowerCaseMap = new HashMap(jsonObject.length());
        while (keys.hasNext()) {
            String key = keys.next().toString();
            String lowerCaseKey = toLowerCase(key);
            lowerCaseMap.put(lowerCaseKey, jsonObject.get(key));
        }

        return new JSONObject(lowerCaseMap);
    }
}
