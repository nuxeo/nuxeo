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
package org.nuxeo.ecm.webengine.forms.validation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.forms.FormDataProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FormManager implements InvocationHandler, Form {

    protected Map<String, Object> map;

    protected Map<String, String[]> fields;

    protected List<String> unknownKeys;

    protected FormDescriptor fd;

    public FormManager(FormDescriptor fd) {
        this.fd = fd;
        unknownKeys = new ArrayList<>();
        map = new HashMap<>(); // remove any previous data
        fields = new HashMap<>(); // remove any previous data
        // TODO when implementing file upload - remove here any previously created file
    }

    @Override
    public Collection<String> unknownKeys() {
        return unknownKeys;
    }

    @Override
    public Map<String, String[]> fields() {
        return fields;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load(FormDataProvider data, Form proxy) throws ValidationException {
        ValidationException ve = null;
        Set<String> reqs = (Set<String>) fd.requiredFields.clone();
        for (String key : data.getKeys()) {
            String[] values = data.getList(key);
            if (values != null) {
                int k = 0;
                for (String v : values) {
                    if (v.length() == 0) {
                        k++;
                    }
                }
                if (k == values.length) {
                    values = null;
                }
            }
            if (values != null) {
                fields.put(key, values);
                reqs.remove(key);
            }
            FormDescriptor.Field f = fd.fields.get(key);
            if (f != null) {
                Object o = null;
                try {
                    if (f.isArray) {
                        if (values != null) {
                            o = f.validateArray(values);
                        }
                    } else {
                        String v = values != null && values.length > 0 ? values[0] : null;
                        if (v != null && v.length() > 0) {

                            o = f.validate(v);
                        }
                    }
                } catch (ValidationException e) {
                    if (ve == null) {
                        ve = e;
                    }
                    ve.addInvalidField(key);
                }
                map.put(key, o);
            } else {
                unknownKeys.add(key);
            }
        }
        if (!reqs.isEmpty()) {
            if (ve == null) {
                ve = new ValidationException();
            }
            for (String req : reqs) {
                ve.addRequiredField(req);
            }
        }
        if (ve != null) {
            throw ve;
        }
        if (fd.validator != null) {
            fd.validator.validate(data, proxy);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Form.class) {
            try {
                return method.invoke(this, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        } else {
            String name = method.getName();
            int len = name.length();
            if (len > 3) {
                if (name.startsWith("get")) {
                    name = FormDescriptor.getFieldName(name, len);
                    return map.get(name);
                }
            }
        }
        throw new UnsupportedOperationException("Method unsupported: " + method);
    }

    protected static Map<Class<?>, FormDescriptor> forms = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T newProxy(Class<T> type) {
        WebEngine we = Framework.getService(WebEngine.class);
        ClassLoader cl = we != null ? we.getWebLoader().getClassLoader() : FormManager.class.getClassLoader();
        return (T) Proxy.newProxyInstance(cl, new Class<?>[] { type }, new FormManager(getDescriptor(type)));
    }

    public void flushCache() {
        forms.clear();
    }

    static FormDescriptor getDescriptor(Class<?> type) {
        FormDescriptor fd = forms.get(type);
        if (fd == null) {
            try {
                fd = new FormDescriptor(type);
                forms.put(type, fd);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to build form descriptor", e);
            }
        }
        return fd;
    }

}
