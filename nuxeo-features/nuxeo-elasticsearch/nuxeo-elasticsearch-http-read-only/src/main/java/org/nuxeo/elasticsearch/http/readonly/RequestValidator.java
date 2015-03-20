package org.nuxeo.elasticsearch.http.readonly;/*
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
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
        ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);
        indexTypes = new HashMap<>();
        for (String name : esa.getRepositoryNames()) {
            List<String> types = new ArrayList<>();
            types.add(ElasticSearchConstants.DOC_TYPE);
            indexTypes.put(esa.getIndexNameForRepository(name), types);
        }
        // TODO handle non repository index types
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
