/*
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

    public void init(CoreSession session, String indices, String types, String rawQuery, String payload) {
        this.principal = (NuxeoPrincipal) session.getPrincipal();
        if (!this.principal.isAdministrator()) {
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
