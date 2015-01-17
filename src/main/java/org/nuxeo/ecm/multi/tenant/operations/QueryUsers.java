/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant.operations;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
@Operation(id = QueryUsers.ID, category = Constants.CAT_SERVICES, label = "Query users", description = "Query users filtered by a tenantId if provided.")
public class QueryUsers {

    public static final String ID = "Services.QueryUsers";

    public static final Set<String> FULLTEXT_FIELDS = new HashSet<String>(Arrays.asList("username", "firstName",
            "lastName"));

    @Context
    protected UserManager userManager;

    @Param(name = "pattern", required = false)
    protected String pattern;

    @Param(name = "tenantId", required = false)
    protected String tenantId;

    @OperationMethod
    public Blob run() throws Exception {
        List<DocumentModel> users = new ArrayList<DocumentModel>();
        if (StringUtils.isBlank(pattern)) {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            if (!StringUtils.isBlank(tenantId)) {
                filter.put("tenantId", tenantId);
            }

            users.addAll(userManager.searchUsers(filter, filter.keySet()));
        } else {
            Map<String, Serializable> filter = new HashMap<String, Serializable>();
            for (String field : FULLTEXT_FIELDS) {
                filter.put(field, pattern);

                if (!StringUtils.isBlank(tenantId)) {
                    filter.put("tenantId", tenantId);
                }

                users.addAll(userManager.searchUsers(filter, filter.keySet()));
            }
        }

        return buildResponse(users);
    }

    protected Blob buildResponse(List<DocumentModel> users) throws UnsupportedEncodingException, ClientException {

        JSONArray array = new JSONArray();
        for (DocumentModel user : users) {
            JSONObject o = new JSONObject();
            o.element("username", user.getProperty("user", "username"));
            o.element("firstName", user.getProperty("user", "firstName"));
            o.element("lastName", user.getProperty("user", "lastName"));
            o.element("email", user.getProperty("user", "email"));
            o.element("company", user.getProperty("user", "company"));
            o.element("tenantId", user.getProperty("user", "tenantId"));
            array.add(o);
        }

        JSONObject result = new JSONObject();
        result.put("users", array);

        return new StringBlob(result.toString(), "application/json");
    }
}
