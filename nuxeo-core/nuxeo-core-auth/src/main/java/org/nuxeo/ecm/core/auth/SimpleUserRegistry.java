/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.core.auth;

import java.io.File;
import java.io.FileInputStream;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.login.LoginException;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.runtime.api.login.Authenticator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SimpleUserRegistry implements Authenticator {

    protected Map<String, SimpleNuxeoPrincipal> users;

    public SimpleUserRegistry(File file) {
        users = new ConcurrentHashMap<String, SimpleNuxeoPrincipal>();
    }

    public void loadRegistry(File file) throws Exception {
        if (file.isFile()) {
            XMap xmap = new XMap();
            xmap.register(SimpleNuxeoPrincipal.class);
            FileInputStream in = new FileInputStream(file);
            try {
                Object[] ar = xmap.loadAll(in);
                for (Object o : ar) {
                    SimpleNuxeoPrincipal principal = (SimpleNuxeoPrincipal)o;
                    users.put(principal.getName(), principal);
                }
            } finally {
                in.close();
            }
        }
    }

    public void clear() {
        users.clear();
    }

    public NuxeoPrincipal getUser(String name) {
        NuxeoPrincipal user = users.get(name);
        return user;
    }

    public NuxeoPrincipal[] getUsers() {
        return users.values().toArray(new NuxeoPrincipal[users.size()]);
    }

    public Principal getPrincipal(String name, String password) throws LoginException {
        NuxeoPrincipal principal = doAuthenticate(name, password);
        if (principal == null) {
            throw new LoginException("Failed to authenticate user "+name);
        }
        return principal;
    }

    public NuxeoPrincipal doAuthenticate(String name, String password) {
        NuxeoPrincipal principal = getUser(name);
        if (principal == null) {
            return null;
        }
        String pwd = principal.getPassword();
        if (password == null || !password.equals(pwd)) {
            return null;
        }
        return principal;
    }

    @Override
    public boolean authenticate(String name, String password) {
        return doAuthenticate(name, password) != null;
    }

}
