/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.api.Framework;

/**
 * Validate request inputs.
 *
 * @since 7.3
 */
public class RequestValidator {
    final private Map<String, List<String>> indexTypes;

    public RequestValidator() {
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        indexTypes = new HashMap<>();
        for (String name : esa.getRepositoryNames()) {
            List<String> types = new ArrayList<>();
            types.add(ElasticSearchConstants.DOC_TYPE);
            indexTypes.put(esa.getIndexNameForRepository(name), types);
        }
    }

    public void checkValidDocumentId(String documentId) {
        if (documentId == null) {
            throw new IllegalArgumentException("Invalid document id");
        }
    }

    public @NotNull String getTypes(String indices, String types) {
        Set<String> validTypes = new HashSet<>();
        for (String index : indices.split(",")) {
            validTypes.addAll(indexTypes.get(index));
        }
        if (types == null || "*".equals(types) || "_all".equals(types)) {
            return StringUtils.join(validTypes, ',');
        }
        for (String type : types.split(",")) {
            if (!validTypes.contains(type)) {
                throw new IllegalArgumentException("Invalid index type: " + type);
            }
        }
        return types;
    }

    public @NotNull String getIndices(String indices) {
        if (indices == null || "*".equals(indices) || "_all".equals(indices)) {
            return StringUtils.join(indexTypes.keySet(), ',');
        }

        for (String index : indices.split(",")) {
            if (!indexTypes.containsKey(index)) {
                throw new IllegalArgumentException("Invalid index submitted: " + index);
            }
        }
        return indices;
    }

    public void checkAccess(NuxeoPrincipal principal, String docAcl) {
        try {
            JSONObject docAclJson = new JSONObject(docAcl);
            JSONArray acl = docAclJson.getJSONObject("fields").getJSONArray("ecm:acl");
            String[] principals = SecurityService.getPrincipalsToCheck(principal);
            for (int i = 0; i < acl.length(); i++)
                for (String name : principals) {
                    if (name.equals(acl.getString(i))) {
                        return;
                    }
                }
        } catch (JSONException e) {
            // throw a securityException
        }
        throw new SecurityException("Unauthorized access");
    }
}
