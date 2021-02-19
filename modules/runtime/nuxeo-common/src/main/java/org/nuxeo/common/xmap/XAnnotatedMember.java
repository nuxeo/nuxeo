/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *     Bogdan Stefanescu
 *     Anahide Tchertchian
 */

package org.nuxeo.common.xmap;

import org.nuxeo.common.xmap.annotation.XNode;
import org.w3c.dom.Element;

/**
 * Processor for annotated member field or method.
 */
public class XAnnotatedMember<T> {

    protected final XAccessor<T> accessor;

    protected Path path;

    protected Path fallbackPath;

    protected String defaultValue;

    protected boolean trim = true;

    /** The Java type of the described element. */
    protected Class<T> type;

    /** Not null if the described object is an xannotated object. */
    protected XAnnotatedObject<T> xao;

    /**
     * The value factory used to transform strings in objects compatible with this member type. In the case of
     * collection types this factory is used for collection components.
     */
    protected XValueFactory<T> valueFactory;

    protected XAnnotatedMember(XMap xmap, XAccessor<T> accessor) {
        this.accessor = accessor;
    }

    public XAnnotatedMember(XMap xmap, XAccessor<T> accessor, String path, String fallbackPath, String defaultValue,
            boolean trim) {
        this(xmap, accessor);
        this.path = new Path(path);
        if (fallbackPath != null && !XNode.NO_FALLBACK_MARKER.equals(fallbackPath)) {
            this.fallbackPath = new Path(fallbackPath);
        }
        type = accessor.getType();
        valueFactory = xmap.getValueFactory(type);
        if (valueFactory == null && type.isEnum()) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            XValueFactory<T> factory = (XValueFactory<T>) new XValueFactory<Enum>() {
                @Override
                public String serialize(Context context, Enum value) {
                    return value.name();
                }

                @Override
                public Enum deserialize(Context context, String value) {
                    Class<Enum> enumType = (Class<Enum>) type;
                    return Enum.valueOf(enumType, value);
                }
            };
            valueFactory = factory;
            xmap.setValueFactory(type, valueFactory);
        }
        xao = xmap.register(type);
        if (!XNode.NO_DEFAULT_ASSIGNMENT_MARKER.equals(defaultValue)) {
            this.defaultValue = defaultValue;
        }
        this.trim = trim;
    }

    public XAnnotatedMember(XMap xmap, XAccessor<T> setter, XNode anno) {
        this(xmap, setter, anno.value(), anno.fallback(), anno.defaultAssignment(), anno.trim());
    }

    protected void setValue(Object instance, T value) {
        try {
            accessor.setValue(instance, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("%s, setter=%s, value=%s", e.getMessage(), accessor, value), e);
        }
    }

    public void toXML(Object instance, Element parent) {
        Element e = XMLBuilder.getOrCreateElement(parent, path);
        T v = accessor.getValue(instance);
        if (xao == null) {
            if (v != null && valueFactory != null) {
                String value = valueFactory.serialize(null, v);
                if (value != null) {
                    XMLBuilder.fillField(e, value, path.attribute);
                }
            }
        } else {
            XMLBuilder.toXML(v, e, xao);
        }
    }

    public void process(Context ctx, Element element) {
        process(ctx, element, null);
    }

    /**
     * Sets the resolved value on the given object, potentially applying merge logic with given existing object.
     *
     * @since 11.5
     */
    public void process(Context ctx, Element element, Object existing) {
        if (existing == null || hasValue(ctx, element)) {
            T value;
            if (existing == null) {
                value = getValue(ctx, element);
            } else {
                value = getValue(ctx, element, accessor.getValue(existing));
            }
            if (value != null) {
                setValue(ctx.getObject(), value);
            }
        }
    }

    /**
     * Returns true if there is a specified value in given element.
     *
     * @since 11.5
     */
    public boolean hasValue(Context ctx, Element element) {
        return DOMHelper.hasNode(element, path) || (fallbackPath != null && DOMHelper.hasNode(element, fallbackPath));
    }

    protected Element getElement(Element base) {
        Element el = (Element) DOMHelper.getElementNode(base, path);
        if (el == null && fallbackPath != null) {
            el = (Element) DOMHelper.getElementNode(base, fallbackPath);
        }
        return el;
    }

    /**
     * Returns the resolved value for given element.
     *
     * @since 11.5
     */
    public T getValue(Context ctx, Element base) {
        return getValue(ctx, base, null);
    }

    /**
     * Returns the resolved value for given element and existing object.
     *
     * @since 11.5
     */
    public T getValue(Context ctx, Element base, T existing) {
        if (xao != null) {
            Element el = getElement(base);
            if (el == null) {
                return null;
            } else {
                return xao.newInstance(ctx, el, existing);
            }
        }
        // scalar field
        if (type == Element.class) {
            // allow DOM elements as values
            @SuppressWarnings("unchecked")
            T value = (T) getElement(base);
            return value;
        }
        String val = DOMHelper.getNodeValue(base, path);
        if (val == null && fallbackPath != null) {
            val = DOMHelper.getNodeValue(base, fallbackPath);
        }
        if (val != null) {
            if (trim) {
                val = val.trim();
            }
            if (valueFactory == null) {
                throw new NullPointerException("Missing XValueFactory for " + type);
            }
            return valueFactory.deserialize(ctx, val);
        }
        return getDefaultValue(ctx);
    }

    protected T getDefaultValue(Context ctx) {
        if (defaultValue != null && valueFactory != null) {
            return valueFactory.deserialize(ctx, defaultValue);
        }
        return (T) defaultValue;
    }

}
