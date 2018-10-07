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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.platform.routing.core.audit.es;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.http.readonly.filter.AuditRequestFilter;
import org.nuxeo.runtime.api.Framework;

/**
 * Define a elasticsearch passthrough filter for audit_wf index view. Restrict to 'Routing' event category and, if the
 * user is not an administrator, to the list of workflow model on which the user has the 'Data Visualization'
 * permission.
 *
 * @since 7.4
 */
public class RoutingAuditRequestFilter extends AuditRequestFilter {

    private CoreSession session;

    @Override
    public void init(CoreSession session, String indices, String types, String rawQuery, String payload) {
        this.session = session;
        principal = session.getPrincipal();
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        this.indices = esa.getIndexNameForType(ElasticSearchConstants.ENTRY_TYPE);
        this.types = ElasticSearchConstants.ENTRY_TYPE;
        this.rawQuery = rawQuery;
        this.payload = payload;
        if (payload == null && !principal.isAdministrator()) {
            // here we turn the UriSearch query_string into a body search
            extractPayloadFromQuery();
        }
    }

    @Override
    public String getPayload() throws JSONException {
        if (filteredPayload == null) {
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
            JSONObject categoryFilter = new JSONObject().put("term", new JSONObject().put(
                    DocumentEventContext.CATEGORY_PROPERTY_KEY, DocumentRoutingConstants.ROUTING_CATEGORY));

            JSONArray fs = new JSONArray().put(categoryFilter);

            if (!principal.isAdministrator()) {
                DocumentRoutingService documentRoutingService = Framework.getService(DocumentRoutingService.class);
                List<DocumentRoute> wfModels = documentRoutingService.getAvailableDocumentRouteModel(session);
                List<String> modelNames = new ArrayList<String>();
                for (DocumentRoute model : wfModels) {
                    if (session.hasPermission(model.getDocument().getRef(), DocumentRoutingConstants.CAN_DATA_VISU)) {
                        modelNames.add(model.getModelName());
                    }
                }

                JSONObject wfModelFilter = new JSONObject().put("terms",
                        new JSONObject().put("extended.modelName", modelNames.toArray(new String[modelNames.size()])));

                fs.put(wfModelFilter);
            }

            JSONObject newQuery = new JSONObject().put("bool",
                    new JSONObject().put("must", query).put("filter", fs));
            payloadJson.put("query", newQuery);
            filteredPayload = payloadJson.toString();
            if (filteredPayload.contains(BACKSLASH_MARKER)) {
                filteredPayload = filteredPayload.replaceAll(BACKSLASH_MARKER, "\\\\");
            }

        }
        return filteredPayload;
    }

}
