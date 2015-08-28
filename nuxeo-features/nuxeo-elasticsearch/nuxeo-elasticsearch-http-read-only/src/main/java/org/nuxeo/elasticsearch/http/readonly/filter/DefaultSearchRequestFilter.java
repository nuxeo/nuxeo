package org.nuxeo.elasticsearch.http.readonly.filter;/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Benoit Delbosc
 */

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
            JSONObject newQuery = new JSONObject().put("filtered",
                    new JSONObject().put("query", query).put("filter", filter));
            payloadJson.put("query", newQuery);
            filteredPayload = payloadJson.toString();
            if (filteredPayload.contains(BACKSLASH_MARKER)) {
                filteredPayload = filteredPayload.replaceAll(BACKSLASH_MARKER, "\\\\");
            }

        }
        return filteredPayload;
    }

}
