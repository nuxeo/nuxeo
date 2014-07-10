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
import java.util.Set;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.nuxeo.ecm.automation.client.LoginInfo;
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
    public LoginInfo read(JsonParser jp) throws Exception {
        boolean isAdmin = false;
        String username = null;
        Set<String> groups = null;
        JsonToken tok = jp.nextToken();
        while (tok != null && tok != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            if ("username".equals(key)) {
                username = jp.getText();
            } else if ("isAdministrator".equals(key)) {
                isAdmin = Boolean.parseBoolean(jp.getText());
            } else if ("groups".equals(key)) {
                jp.nextToken();
                groups = readGroups(jp);
            }
            tok = jp.nextToken();
        }
        if (tok == null) {
            throw new IllegalArgumentException(
                    "Unexpected end of stream.");
        }
        return new LoginInfo(username, groups, isAdmin);
    }

    protected Set<String> readGroups(JsonParser jp) throws Exception {
        HashSet<String> groups = new HashSet<String>();
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_ARRAY) {
            groups.add(jp.getText());
            tok = jp.nextToken();
        }
        return groups;
    }

    @Override
    public void write(JsonGenerator jg, Object value) throws Exception {
        LoginInfo loginInfo = (LoginInfo) value;
        jg.writeStringField("username", loginInfo.getUsername());
        jg.writeBooleanField("isAdministrator", loginInfo.isAdministrator());
        jg.writeArrayFieldStart("groups");
        String[] groups = loginInfo.getGroups();
        if (groups != null) {
            for (String g : groups) {
                jg.writeString(g);
            }
        }
        jg.writeEndArray();
    }

}
