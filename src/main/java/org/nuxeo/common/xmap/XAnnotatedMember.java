/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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

    protected boolean trim;

    /** The Java type of the described element. */
    protected Class type;

    /** Not null if the described object is an xannotated object. */
    protected XAnnotatedObject xao;

    /**
     * The value factory used to transform strings in objects compatible with
     * this member type. In the case of collection types this factory is used
     * for collection components.
     */
    protected XValueFactory valueFactory;

    private final XMap xmap;

    protected XAnnotatedMember(XMap xmap, XAccessor accessor) {
        this.xmap = xmap;
        this.accessor = accessor;
    }

    public XAnnotatedMember(XMap xmap, XAccessor setter, XNode anno) {
        this.xmap = xmap;
        accessor = setter;
        path = new Path(anno.value());
        trim = anno.trim();
        type = setter.getType();
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
                    return Enum.valueOf(type, arg1);
                }
            };
            xmap.setValueFactory(type, valueFactory);
        }
        xao = xmap.register(type);
    }

    protected void setValue(Object instance, Object value) {
        try {
            accessor.setValue(instance, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("%s, setter=%s, value=%s", e.getMessage(),
                            accessor, value), e);
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
        Object value = getValue(ctx, element);
        if (value != null) {
            setValue(ctx.getObject(), value);
        }
    }

    protected Object getValue(Context ctx, Element base) {
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
        if (val != null) {
            if (trim) {
                val = val.trim();
            }
            if (valueFactory == null) {
                throw new NullPointerException("Missing XValueFactory for "
                        + type);
            }
            return valueFactory.deserialize(ctx, val);
        }
        return null;
    }

}
