/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */
package org.nuxeo.ecm.platform.ui.select2.automation;

import java.io.Serializable;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.platform.ui.select2.common.Select2Common;
import org.nuxeo.ecm.platform.usermanager.UserManager;

/**
 * SuggestUser Operation.
 *
 * @since 5.7.3
 */
@Operation(id = SuggestUserEntries.ID, category = Constants.CAT_SERVICES, label = "Get user/group suggestion", description = "Get the user/group list of the running instance. This is returning a blob containing a serialized JSON array..")
public class SuggestUserEntries {

    private static final Log log = LogFactory.getLog(SuggestUserEntries.class);

    public static final String ID = "UserGroup.Suggestion";

    @Context
    protected OperationContext ctx;

    @Context
    protected SchemaManager schemaManager;

    @Param(name = "prefix", required = false)
    protected String prefix;

    @Param(name = "searchType", required = false)
    protected String searchType;

    @Context
    protected UserManager userManager;

    @OperationMethod
    public Blob run() throws ClientException {
        JSONArray result = new JSONArray();
        boolean groupOnly = false, userOnly = false;
        if (searchType != null && !searchType.isEmpty()) {
            if (searchType.equals(Select2Common.USER_TYPE)) {
                userOnly = true;
            } else if (searchType.equals(Select2Common.GROUP_TYPE)) {
                groupOnly = true;
            }
        }
        if (!groupOnly) {
            Schema schema = schemaManager.getSchema("user");
            DocumentModelList userList = userManager.searchUsers(prefix);
            for (DocumentModel user : userList) {
                JSONObject obj = new JSONObject();
                String username = null;
                String firstname = null;
                String lastname = null;
                for (Field field : schema.getFields()) {
                    QName fieldName = field.getName();
                    String key = fieldName.getLocalName();
                    Serializable value = user.getPropertyValue(fieldName.getPrefixedName());
                    obj.element(key, value);
                    if (key.equals("username")) {
                        username = (String) value;
                    } else if (key.equals("firstName")) {
                        firstname = (String) value;
                    } else if (key.equals("lastName")) {
                        lastname = (String) value;
                    }
                }
                String label = "";
                if (firstname != null && !firstname.isEmpty()
                        && lastname != null && !lastname.isEmpty()) {
                    label = firstname + " " + lastname;
                } else {
                    label = username;
                }
                String userId = user.getId();
                obj.put(Select2Common.ID, userId);
                obj.put(Select2Common.LABEL, label);
                obj.put(Select2Common.TYPE_KEY_NAME, Select2Common.USER_TYPE);
                obj.put(Select2Common.PREFIXED_ID_KEY_NAME, NuxeoPrincipal.PREFIX + userId);
                result.add(obj);
            }
        }
        if (!userOnly) {
            Schema schema = schemaManager.getSchema("group");
            DocumentModelList groupList = userManager.searchGroups(prefix);
            for (DocumentModel group : groupList) {
                JSONObject obj = new JSONObject();
                for (Field field : schema.getFields()) {
                    QName fieldName = field.getName();
                    String key = fieldName.getLocalName();
                    Serializable value = group.getPropertyValue(fieldName.getPrefixedName());
                    obj.element(key, value);
                    if (key.equals("grouplabel")) {
                        obj.element(Select2Common.LABEL, value);
                    }
                }
                String groupId = group.getId();
                obj.put(Select2Common.ID, groupId);
                obj.put(Select2Common.TYPE_KEY_NAME, Select2Common.GROUP_TYPE);
                obj.put(Select2Common.PREFIXED_ID_KEY_NAME, NuxeoGroup.PREFIX + groupId);
                result.add(obj);
            }
        }

        return new StringBlob(result.toString(), "application/json");
    }

}
