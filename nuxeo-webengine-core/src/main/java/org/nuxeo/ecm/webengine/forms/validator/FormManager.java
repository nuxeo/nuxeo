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
package org.nuxeo.ecm.webengine.forms.validator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.webengine.forms.FormDataProvider;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FormManager implements InvocationHandler, Form {

    protected Map<String, Object> map;
    protected List<String> unknownKeys;
    protected FormDescriptor fd;

    
    public FormManager(FormDescriptor fd) {
        this.fd = fd;
        map = new HashMap<String, Object>();
        unknownKeys = new ArrayList<String>();
    }
    
    public Collection<String> unknownKeys() {
        return unknownKeys;
    }
    
    @SuppressWarnings("unchecked")
    public void loadData(FormDataProvider data, Form proxy) throws ValidationException {
        ValidationException ve = null;
        HashSet<String> reqs = (HashSet<String>)fd.requiredFields.clone();
        for (String key : data.getKeys()) {
            reqs.remove(key);
            FormDescriptor.Field f = fd.fields.get(key);
            if (f != null) {
                Object o = null;
                try {
                    if (f.isArray) {
                        o = f.validateArray(data.getList(key));
                    } else {
                        o = f.validate(data.getString(key));
                    }
                } catch (ValidationException e) {
                    if (ve == null) {
                        ve = e;
                    }
                    ve.addField(key);
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
    
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
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
        throw new UnsupportedOperationException("Method unsupported: "+method);
    }
    
    

    
    
    protected static Map<Class<?>, FormDescriptor> forms = new Hashtable<Class<?>, FormDescriptor>();
    
    @SuppressWarnings("unchecked")
    public static <T> T newProxy(Class<T> type) {
        return (T)Proxy.newProxyInstance(FormManager.class.getClassLoader(), 
                new Class<?>[] {type}, 
                new FormManager(getDescriptor(type)));
    }

    public void flushCache() {
        forms = new Hashtable<Class<?>, FormDescriptor>();
    }
    
    static FormDescriptor getDescriptor(Class<?> type) {
        FormDescriptor fd = forms.get(type);
        if (fd == null) {
            try {
                fd = new FormDescriptor(type);
                forms.put(type, fd);
            } catch (Exception e) {
                throw new Error("Failed to build form descriptor", e);
            }
        }
        return fd;
    }
    
    
    
}
