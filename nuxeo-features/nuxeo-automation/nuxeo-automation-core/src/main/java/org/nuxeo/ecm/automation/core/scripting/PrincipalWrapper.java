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
package org.nuxeo.ecm.automation.core.scripting;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@SuppressWarnings("serial")
public class PrincipalWrapper extends HashMap<String, Serializable> {

    protected NuxeoPrincipal principal;

    protected UserManager mgr;

    public PrincipalWrapper(NuxeoPrincipal principal) {
        try {
            mgr = Framework.getService(UserManager.class);
            this.principal = principal;
        } catch (Exception e) {
            throw new RuntimeException("User manager not found: ", e);
        }
    }

    public String getName() {
        return principal.getName();
    }

    public String getCompany() {
        return principal.getCompany();
    }

    public String getFirstName() {
        return principal.getFirstName();
    }

    public String getLastName() {
        return principal.getLastName();
    }

    public String getOriginatingUser() {
        return principal.getOriginatingUser();
    }

    public List<String> getAllGroups() {
        return principal.getAllGroups();
    }

    public List<String> getGroups() {
        return principal.getGroups();
    }

    public String getEmail() throws Exception {
        return (String) principal.getModel().getProperty(
                mgr.getUserSchemaName(), mgr.getUserEmailField());
    }

    public NuxeoPrincipal getPrincipal() {
        return principal;
    }

    public Serializable getProperty(String xpath) {
        try {
            return principal.getModel().getPropertyValue(xpath);
        } catch (ClientException e) {
            throw new RuntimeException(
                    "Principal property not found: " + xpath, e);
        }
    }

    /** property map implementation */

    @Override
    public boolean containsKey(Object key) {
        try {
            getProperty(key.toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * The behavior of this method was changed -> it is checking if an xpath
     * has a value attached.
     */
    @Override
    public boolean containsValue(Object value) {
        try {
            return getProperty(value.toString()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Serializable get(Object key) {
        try {
            return getProperty(key.toString());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public Set<String> keySet() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public Collection<Serializable> values() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public Set<Map.Entry<String, Serializable>> entrySet() {
        throw new UnsupportedOperationException("Operation not supported.");
    }

    @Override
    public Serializable put(String key, Serializable value) {
        try {
            Property p = principal.getModel().getProperty(key);
            Serializable v = p.getValue();
            p.setValue(value);
            return v;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void putAll(Map<? extends String, ? extends Serializable> m) {
        throw new UnsupportedOperationException("Read Only Map.");
    }

    @Override
    public Serializable remove(Object key) {
        throw new UnsupportedOperationException("Read Only Map.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Read Only Map.");
    }

}
