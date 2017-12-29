/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.automation.client.LoginInfo;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * @author matic
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
    public LoginInfo read(JsonParser jp) throws IOException {
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
            throw new IllegalArgumentException("Unexpected end of stream.");
        }
        return new LoginInfo(username, groups, isAdmin);
    }

    protected Set<String> readGroups(JsonParser jp) throws IOException {
        HashSet<String> groups = new HashSet<String>();
        JsonToken tok = jp.nextToken();
        while (tok != JsonToken.END_ARRAY) {
            groups.add(jp.getText());
            tok = jp.nextToken();
        }
        return groups;
    }

    @Override
    public void write(JsonGenerator jg, Object value) throws IOException {
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
