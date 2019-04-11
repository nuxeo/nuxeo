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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.webengine.forms.validation.annotations.Enumeration;
import org.nuxeo.ecm.webengine.forms.validation.annotations.Length;
import org.nuxeo.ecm.webengine.forms.validation.annotations.NotNull;
import org.nuxeo.ecm.webengine.forms.validation.annotations.Range;
import org.nuxeo.ecm.webengine.forms.validation.annotations.Regex;
import org.nuxeo.ecm.webengine.forms.validation.annotations.Required;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FormDescriptor {

    protected FormValidator validator;

    protected Map<String, Field> fields = new HashMap<>();

    protected HashSet<String> requiredFields = new HashSet<>();

    public FormDescriptor(Class<?> type) throws ReflectiveOperationException {
        Method[] methods = type.getMethods(); // get all inherited public methods
        int mod = type.getModifiers();
        if (!Modifier.isInterface(mod)) {
            throw new IllegalArgumentException("Form type is not an interface");
        }
        for (Method m : methods) {
            String name = m.getName();
            if (!name.startsWith("get")) {
                continue;
            }
            // get the field name
            name = getFieldName(name, name.length());
            Field field = new Field(m, name);
            if (field.required) {
                requiredFields.add(field.name);
            }
            fields.put(name, field);
        }
        org.nuxeo.ecm.webengine.forms.validation.annotations.FormValidator fv = type.getAnnotation(org.nuxeo.ecm.webengine.forms.validation.annotations.FormValidator.class);
        if (fv != null) {
            validator = fv.value().getDeclaredConstructor().newInstance();
        }
    }

    static class Field {
        CompositeValidator validator;

        String name;

        Method m;

        boolean isArray;

        boolean required;

        boolean notnull;

        String defaultValue;

        TypeConvertor<?> convertor;

        Field(Method m, String name) throws ReflectiveOperationException {
            validator = new CompositeValidator();
            // not null
            NotNull nn = m.getAnnotation(NotNull.class);
            if (nn != null) {
                String dv = nn.value();
                if (dv.length() > 0) {
                    defaultValue = dv;
                } else {
                    notnull = true;
                }
            }
            // required
            required = m.isAnnotationPresent(Required.class);
            // enum
            Enumeration aenum = m.getAnnotation(Enumeration.class);
            if (aenum != null) {
                validator.add(new EnumerationValidator(aenum.value()));
            }
            // regex
            Regex regex = m.getAnnotation(Regex.class);
            if (regex != null) {
                validator.add(new RegexValidator(regex.value()));
            }
            // length
            Length length = m.getAnnotation(Length.class);
            if (length != null) {
                if (length.value() > -1) {
                    validator.add(new ExactLengthValidator(length.value()));
                } else {
                    validator.add(new LengthValidator(length.min(), length.max()));
                }
            }
            // range
            Range range = m.getAnnotation(Range.class);
            if (range != null) {
                validator.add(new RangeValidator(range.min(), range.max(), range.negate()));
            }
            // custom validator
            org.nuxeo.ecm.webengine.forms.validation.annotations.FieldValidator custom = m.getAnnotation(org.nuxeo.ecm.webengine.forms.validation.annotations.FieldValidator.class);
            if (custom != null) {
                validator.add((FieldValidator) custom.value().getDeclaredConstructor().newInstance());
            }
            // type convertor
            Class<?> rtype = m.getReturnType();
            isArray = rtype.isArray();
            if (isArray) {
                rtype = rtype.getComponentType();
            }
            convertor = TypeConvertor.getConvertor(rtype);

            this.m = m;
            this.name = name;
        }

        Object validate(String value) throws ValidationException {
            if (value == null || value.length() == 0) {
                value = null; // "" empty strings are treated as null values
                if (notnull) {
                    throw new ValidationException();
                } else if (defaultValue != null) {
                    value = defaultValue;
                }
            }
            Object obj = value;
            if (convertor != null) {
                obj = convertor.convert(value);
            }
            if (validator != null) {
                validator.validate(value, obj);
            }
            return obj;
        }

        Object[] validateArray(String[] values) throws ValidationException {
            List<Object> result = new ArrayList<>();
            for (String value : values) {
                result.add(validate(value));
            }
            if (convertor != null) {
                return result.toArray(convertor.newArray(values.length));
            } else {
                return result.toArray(new String[values.length]);
            }
        }
    }

    static String getFieldName(String key, int len) {
        if (len == 4) {
            return "" + Character.toLowerCase(key.charAt(3));
        } else {
            return Character.toLowerCase(key.charAt(3)) + key.substring(4);
        }
    }

}
