/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.mail;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * @since 11.1
 */
public class MailAuthenticator extends Authenticator {

    protected final Properties properties;

    public MailAuthenticator(Properties properties) {
        this.properties = properties;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        String user = protocolValue("user");
        if (user == null) {
            return null;
        }
        String password = protocolValue("password");
        if (password == null) {
            return null;
        }
        return new PasswordAuthentication(user, password);
    }

    protected String protocolValue(String name) {
        String key = String.format("mail.%s.%s", getRequestingProtocol(), name);
        return properties.getProperty(key);
    }

}
