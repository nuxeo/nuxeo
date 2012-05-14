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

/**
 * @author matic
 *
 */
public class EmailResourceFactory implements ObjectFactory {

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
            Hashtable<?, ?> environment) throws Exception {
        final Reference ref = (Reference) obj;
        if (!ref.getClassName().equals("javax.mail.Session"))
            return (null);
        ref.getAll();
        final Properties properties = toProperties(ref.getAll());
        return AccessController.doPrivileged(new PrivilegedAction<Session>() {
            public Session run() {
                return EmailHelper.newSession(properties);
            }
        });

    }

    protected Properties toProperties(Enumeration<RefAddr> attributes) {
        Properties props = new Properties();
        while (attributes.hasMoreElements()) {
            RefAddr attribute = attributes.nextElement();
            if ("factory".equals(attribute.getType())) {
                continue;
            }
            props.put(attribute.getType(), attribute.getContent());
        }
        return props;
    }
}
