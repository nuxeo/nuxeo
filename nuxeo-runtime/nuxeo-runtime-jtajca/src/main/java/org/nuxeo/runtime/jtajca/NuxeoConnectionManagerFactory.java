/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.runtime.jtajca;

import java.util.Collections;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.resource.spi.ConnectionManager;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Factory for the ConnectionManager.
 */
public class NuxeoConnectionManagerFactory implements ObjectFactory {

    private static final Log log = LogFactory.getLog(NuxeoConnectionManagerFactory.class);

    @Override
    public Object getObjectInstance(Object obj, Name objName, Context nameCtx, Hashtable<?, ?> env) {
        Reference ref = (Reference) obj;
        if (!ConnectionManager.class.getName().equals(ref.getClassName())) {
            return null;
        }
        String name;
        int size = objName.size();
        if (size == 1) {
            name = "default";
        } else {
            name = objName.get(size - 1);
        }

        final ConnectionManager cm = NuxeoContainer.connectionManagers.get(name);
        if (cm != null) {
            return cm;
        }

        NuxeoConnectionManagerConfiguration config = new NuxeoConnectionManagerConfiguration();
        for (RefAddr addr : Collections.list(ref.getAll())) {
            String type = addr.getType();
            String content = (String) addr.getContent();
            try {
                BeanUtils.setProperty(config, type, content);
            } catch (ReflectiveOperationException e) {
                log.error(String.format("NuxeoConnectionManagerFactory cannot set %s = %s", type, content));
            }
        }
        return NuxeoContainer.initConnectionManager(config);
    }

    public static NuxeoConnectionManagerConfiguration getConfig(Reference ref) {
        NuxeoConnectionManagerConfiguration config = new NuxeoConnectionManagerConfiguration();
        IllegalArgumentException errors = new IllegalArgumentException("wrong naming config");
        for (RefAddr addr : Collections.list(ref.getAll())) {
            String name = addr.getType();
            String value = (String) addr.getContent();
            try {
                BeanUtils.setProperty(config, name, value);
            } catch (ReflectiveOperationException cause) {
                errors.addSuppressed(cause);
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
        return config;
    }

}
