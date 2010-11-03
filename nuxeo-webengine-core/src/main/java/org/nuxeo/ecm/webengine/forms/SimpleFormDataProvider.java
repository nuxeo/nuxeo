/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.webengine.forms;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.webengine.forms.validation.Form;
import org.nuxeo.ecm.webengine.forms.validation.FormManager;
import org.nuxeo.ecm.webengine.forms.validation.ValidationException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SimpleFormDataProvider extends HashMap<String,String[]>  implements FormDataProvider {

    private static final long serialVersionUID = 1L;

    public Collection<String> getKeys() {
        return keySet();
    }

    public String[] getList(String key) {
        return get(key);
    }

    public String getString(String key) {
        String[] v = get(key);
        if (v != null && v.length > 0) {
            return v[0];
        }
        return null;
    }

    public Map<String, String[]> getFormFields() {
        return this;
    }

    public void putString(String key, String value) {
        put(key, new String[] {value});
    }

    public void putList(String key, String ... values) {
        put(key, values);
    }

    public void putList(String key, Collection<String> values) {
        if (values == null) {
            return;
        }
        String[] ar = values.toArray(new String[values.size()]);
        put(key, ar);
    }

    public <T extends Form> T validate(Class<T> type) throws ValidationException {
        T proxy = FormManager.newProxy(type);
        try {
            proxy.load(this, proxy);
            return proxy;
        } catch (ValidationException e) {
            e.setForm(proxy);
            throw e;
        }
    }

}
