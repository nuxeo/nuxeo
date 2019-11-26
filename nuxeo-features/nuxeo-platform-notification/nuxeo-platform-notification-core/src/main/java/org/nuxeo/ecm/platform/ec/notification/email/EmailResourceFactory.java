/*
 * (C) Copyright 2012-2019 Nuxeo (http://nuxeo.com/) and others.
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import javax.mail.Session;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import org.nuxeo.mail.MailSessionBuilder;
import org.nuxeo.runtime.api.Framework;

/**
 * @author matic
 */
public class EmailResourceFactory implements ObjectFactory {

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) {
        final Reference ref = (Reference) obj;
        if (!Session.class.getName().equals(ref.getClassName())) {
            return null;
        }
        final Properties properties = toProperties(ref.getAll());
        return AccessController.doPrivileged(
                (PrivilegedAction<Session>) () -> MailSessionBuilder.fromProperties(properties).build());

    }

    protected Properties toProperties(Enumeration<RefAddr> attributes) {
        Properties props = new Properties();
        while (attributes.hasMoreElements()) {
            RefAddr attribute = attributes.nextElement();
            if ("factory".equals(attribute.getType())) {
                continue;
            }
            String key = attribute.getType();
            String value = Framework.expandVars((String) attribute.getContent());
            props.put(key, value);
        }
        return props;
    }
}
