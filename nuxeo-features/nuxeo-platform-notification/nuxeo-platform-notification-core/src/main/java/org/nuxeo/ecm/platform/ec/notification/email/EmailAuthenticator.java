/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.ec.notification.email;

import java.util.Properties;

import javax.mail.PasswordAuthentication;

public class EmailAuthenticator extends javax.mail.Authenticator {

    protected final Properties properties;

    public EmailAuthenticator(Properties properties) {
        this.properties = properties;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        String user = value("user");
        if (user == null) {
            return null;
        }
        String password = value("password");
        if (password == null) {
            return null;
        }
        return new PasswordAuthentication(user, password);
    }

    protected String value(String name) {
        String value = protocolValue(name);
        if (value != null) {
            return value;
        }
        return defaultValue(name);
    }

    protected String protocolValue(String name) {
        String key = "mail." + getRequestingProtocol() + "." + name;
        return properties.getProperty(key);
    }

    protected String defaultValue(String name) {
        String key = "mail." + getRequestingProtocol() + "." + name;
        return properties.getProperty(key);
    }
}
