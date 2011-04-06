/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers;

import java.util.HashSet;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.client.jaxrs.LoginInfo;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

/**
 * @author matic
 * 
 */
public class LoginMarshaller implements JsonMarshaller<LoginInfo> {

    @Override
    public String getType() {
        return "login";
    }

    @Override
    public Class<LoginInfo> getJavaType() {
        return LoginInfo.class;
    }
    
    public String getReference(LoginInfo info) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LoginInfo read(JSONObject json) {
        return readLogin(json);
    }


    @Override
    public void write(JSONObject object, LoginInfo info) {
        object.put("username", info.getUsername());
        object.put("isAdministrator", info.isAdministrator());
        JSONArray groups = new JSONArray();
        for (String group:info.getGroups()) {
            groups.add(group);
        }
        object.put("groups", groups);
    }

    protected static LoginInfo readLogin(JSONObject json) {
        String username = json.getString("username");
        String isAdmin = json.optString("isAdministrator", "false");
        JSONArray groups = json.optJSONArray("groups");
        HashSet<String> set = new HashSet<String>();
        if (groups != null) {
            for (int i = 0, size = groups.size(); i < size; i++) {
                set.add(groups.getString(i));
            }
        }
        return new LoginInfo(username, set, Boolean.parseBoolean(isAdmin));
    }

}
