/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.common.xmap;

import org.nuxeo.common.xmap.annotation.XNode;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class XAnnotatedMember {

    protected final XAccessor accessor;

    protected Path path;

    protected Path fallbackPath;

    protected String defaultValue;

    protected boolean trim = true;

    /** The Java type of the described element. */
    protected Class<?> type;

    /** Not null if the described object is an xannotated object. */
    protected XAnnotatedObject xao;

    /**
     * The value factory used to transform strings in objects compatible with this member type. In the case of
     * collection types this factory is used for collection components.
     */
    protected XValueFactory valueFactory;

    protected XAnnotatedMember(XMap xmap, XAccessor accessor) {
        this.accessor = accessor;
    }

    protected XAnnotatedMember(XMap xmap, XAccessor accessor, String path, String fallbackPath, String defaultValue,
            boolean trim) {
        this(xmap, accessor);
        this.path = new Path(path);
        if (fallbackPath != null && !XNode.NO_FALLBACK_VALUE_MARKER.equals(fallbackPath)) {
            this.fallbackPath = new Path(fallbackPath);
        }
        type = accessor.getType();
        valueFactory = xmap.getValueFactory(type);
        if (valueFactory == null && type.isEnum()) {
            valueFactory = new XValueFactory() {
                @Override
                public String serialize(Context arg0, Object arg1) {
                    return ((Enum<?>) arg1).name();
                }

                @SuppressWarnings("unchecked")
                @Override
                public Object deserialize(Context arg0, String arg1) {
                    @SuppressWarnings("rawtypes")
                    Class<Enum> enumType = (Class<Enum>) type;
                    return Enum.valueOf(enumType, arg1);
                }
            };
            xmap.setValueFactory(type, valueFactory);
        }
        xao = xmap.register(type);
        if (!XNode.NO_DEFAULT_VALUE_MARKER.equals(defaultValue)) {
            this.defaultValue = defaultValue;
        }
        this.trim = trim;
    }

    public XAnnotatedMember(XMap xmap, XAccessor setter, XNode anno) {
        this(xmap, setter, anno.value(), anno.fallbackValue(), anno.defaultValue(), anno.trim());
    }

    protected void setValue(Object instance, Object value) {
        try {
            accessor.setValue(instance, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("%s, setter=%s, value=%s", e.getMessage(), accessor, value), e);
        }
    }

    public void toXML(Object instance, Element parent) {
        Element e = XMLBuilder.getOrCreateElement(parent, path);
        Object v = accessor.getValue(instance);
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
     * @since TODO
     */
    public void process(Context ctx, Element element, Object existing) {
        if (existing == null || hasValue(ctx, element)) {
            Object value = getValue(ctx, element);
            if (value != null) {
                setValue(ctx.getObject(), value);
            }
        }
    }

    protected boolean hasValue(Context ctx, Element element) {
        if (type == Element.class) {
            return element != null;
        }
        return DOMHelper.hasNode(element, path) || (fallbackPath != null && DOMHelper.hasNode(element, fallbackPath));
    }

    /**
     * @since TODO
     */
    public Object getValue(Context ctx, Element base) {
        if (xao != null) {
            Element el = (Element) DOMHelper.getElementNode(base, path);
            if (el == null) {
                return null;
            } else {
                return xao.newInstance(ctx, el);
            }
        }
        // scalar field
        if (type == Element.class) {
            // allow DOM elements as values
            return base;
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

    protected Object getDefaultValue(Context ctx) {
        if (defaultValue != null && valueFactory != null) {
            return valueFactory.deserialize(ctx, defaultValue);
        }
        return defaultValue;
    }

}
