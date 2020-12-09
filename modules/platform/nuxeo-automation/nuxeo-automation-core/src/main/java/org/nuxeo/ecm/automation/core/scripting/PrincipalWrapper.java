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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.scripting;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class PrincipalWrapper extends HashMap<String, Serializable> {

    private static final long serialVersionUID = 1L;

    protected NuxeoPrincipal principal;

    public PrincipalWrapper(NuxeoPrincipal principal) {
        this.principal = principal;
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

    public String getActingUser() {
        return principal.getActingUser();
    }

    public List<String> getAllGroups() {
        return principal.getAllGroups();
    }

    public List<String> getGroups() {
        return principal.getGroups();
    }

    public String getEmail() {
        return principal.getEmail();
    }

    public NuxeoPrincipal getPrincipal() {
        return principal;
    }

    public Serializable getProperty(String xpath) {
        return principal.getModel().getPropertyValue(xpath);
    }

    /** property map implementation */

    @Override
    public boolean containsKey(Object key) {
        try {
            getProperty(key.toString());
            return true;
        } catch (PropertyException e) {
            return false;
        }
    }

    /**
     * The behavior of this method was changed -&gt; it is checking if an xpath has a value attached.
     */
    @Override
    public boolean containsValue(Object value) {
        try {
            return getProperty(value.toString()) != null;
        } catch (PropertyException e) {
            return false;
        }
    }

    @Override
    public Serializable get(Object key) {
        try {
            return getProperty(key.toString());
        } catch (PropertyException e) {
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
        Property p = principal.getModel().getProperty(key);
        Serializable v = p.getValue();
        p.setValue(value);
        return v;
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
