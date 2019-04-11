/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.webengine.forms;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.webengine.forms.validation.Form;
import org.nuxeo.ecm.webengine.forms.validation.FormManager;
import org.nuxeo.ecm.webengine.forms.validation.ValidationException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SimpleFormDataProvider extends HashMap<String, String[]> implements FormDataProvider {

    private static final long serialVersionUID = 1L;

    @Override
    public Collection<String> getKeys() {
        return keySet();
    }

    @Override
    public String[] getList(String key) {
        return get(key);
    }

    @Override
    public String getString(String key) {
        String[] v = get(key);
        if (v != null && v.length > 0) {
            return v[0];
        }
        return null;
    }

    @Override
    public Map<String, String[]> getFormFields() {
        return this;
    }

    public void putString(String key, String value) {
        put(key, new String[] { value });
    }

    public void putList(String key, String... values) {
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
