/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.auth;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.login.LoginException;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SimpleUserRegistry {

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

    public NuxeoPrincipal authenticate(String name, String password) throws LoginException {
        NuxeoPrincipal principal = getUser(name);
        if (principal == null) {
            return null;
        }
        String pwd = principal.getPassword();
        if (password == pwd) {
            throw new LoginException("Failed to authenticate user "+name);
        }
        if (password == null) {
            throw new LoginException("Failed to authenticate user "+name);
        }
        if (!password.equals(pwd)) {
            throw new LoginException("Failed to authenticate user "+name);
        }
        return principal;
    }

}
