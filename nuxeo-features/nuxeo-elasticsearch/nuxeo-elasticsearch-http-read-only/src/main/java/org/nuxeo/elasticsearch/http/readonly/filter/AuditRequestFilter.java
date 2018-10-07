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

package org.nuxeo.elasticsearch.http.readonly.filter;

import org.json.JSONException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.http.readonly.AbstractSearchRequestFilterImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Define a elasticsearch passthrough filter for audit index. Only administrator can access the audit index.
 *
 * @since 7.4
 */
public class AuditRequestFilter extends AbstractSearchRequestFilterImpl {

    @Override
    public void init(CoreSession session, String indices, String types, String rawQuery, String payload) {
        principal = session.getPrincipal();
        if (!principal.isAdministrator()) {
            throw new IllegalArgumentException("Invalid index submitted: " + indices);
        }
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
        return payload;
    }

}
