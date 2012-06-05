/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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