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
 *     Benoit Delbosc
 */
package org.nuxeo.elasticsearch.http.readonly.filter;

import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.elasticsearch.http.readonly.AbstractSearchRequestFilterImpl;

/**
 * Rewrite an Elsaticsearch search request to add security filter.
 *
 * URI Search are turned into Request body search.
 *
 * @since 7.3
 */
public class DefaultSearchRequestFilter extends AbstractSearchRequestFilterImpl {

    @Override
    public String getPayload() throws JSONException {
        if (principal.isAdministrator()) {
            return payload;
        }
        if (filteredPayload == null) {
            String[] principals = SecurityService.getPrincipalsToCheck(principal);
            if (payload.contains("\\")) {
                // JSONObject removes backslash so we need to hide them
                payload = payload.replaceAll("\\\\", BACKSLASH_MARKER);
            }
            JSONObject payloadJson = new JSONObject(payload);
            JSONObject query;
            if (payloadJson.has("query")) {
                query = payloadJson.getJSONObject("query");

                payloadJson.remove("query");
            } else {
                query = new JSONObject("{\"match_all\":{}}");
            }
            JSONObject filter = new JSONObject().put("terms", new JSONObject().put("ecm:acl", principals));
            JSONObject newQuery = new JSONObject().put("bool",
                    new JSONObject().put("must", query).put("filter", filter));
            payloadJson.put("query", newQuery);
            filteredPayload = payloadJson.toString();
            if (filteredPayload.contains(BACKSLASH_MARKER)) {
                filteredPayload = filteredPayload.replaceAll(BACKSLASH_MARKER, "\\\\");
            }

        }
        return filteredPayload;
    }

}
