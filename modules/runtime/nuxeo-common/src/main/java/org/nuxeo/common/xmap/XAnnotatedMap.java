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

import java.util.Collections;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Processor for annotated field or method into a map-like object.
 */
public class XAnnotatedMap extends XAnnotatedList {

    protected static final ElementMapVisitor elementMapVisitor = new ElementMapVisitor();

    protected static final ElementValueMapVisitor elementVisitor = new ElementValueMapVisitor();

    protected static final AttributeValueMapVisitor attributeVisitor = new AttributeValueMapVisitor();

    protected final Path key;

    protected final boolean isNullByDefault;

    public XAnnotatedMap(XMap xmap, XAccessor setter, XNodeMap anno) {
        super(xmap, setter);
        path = new Path(anno.value());
        trim = anno.trim();
        key = new Path(anno.key());
        type = anno.type();
        componentType = anno.componentType();
        valueFactory = xmap.getValueFactory(componentType);
        xao = xmap.register(componentType);
        isNullByDefault = anno.nullByDefault();
    }

    @Override
    public void process(Context ctx, Element element, Object existing) {
        if (remove != null && Boolean.TRUE.equals(remove.getValue(ctx, element))) {
            setValue(ctx.getObject(), convertMap(Collections.emptyMap()));
            return;
        }
        Map<String, Object> initMap = getInitMap();
        if (existing != null
                && (!hasValue(ctx, element) || merge == null || Boolean.TRUE.equals(merge.getValue(ctx, element)))) {
            Map<String, ?> map = getCurrentMapValue(existing);
            if (map != null) {
                initMap.putAll(map);
            }
        }
        Object value = getValue(ctx, element, initMap);
        if (value != null) {
            setValue(ctx.getObject(), value);
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getInitMap() {
        try {
            return (Map<String, Object>) type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Object getValue(Context ctx, Element base) {
        return getValue(ctx, base, getInitMap());
    }

    protected Object getValue(Context ctx, Element base, Map<String, Object> values) {
        if (xao != null) {
            DOMHelper.visitMapNodes(ctx, this, base, path, elementMapVisitor, values);
        } else {
            if (path.attribute != null) {
                // attribute list
                DOMHelper.visitMapNodes(ctx, this, base, path, attributeVisitor, values);
            } else {
                // element list
                DOMHelper.visitMapNodes(ctx, this, base, path, elementVisitor, values);
            }
        }
        return convertMap(values);
    }

    protected Object convertMap(Map<String, Object> values) {
        if (isNullByDefault && values.isEmpty()) {
            values = null;
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, ?> getCurrentMapValue(Object instance) {
        Object v = accessor.getValue(instance);
        if (v instanceof Map<?, ?>) {
            return (Map<String, ?>) v;
        }
        return null;
    }

    @Override
    public void toXML(Object instance, Element parent) {
        Map<String, ?> map = getCurrentMapValue(instance);
        if (map == null) {
            return;
        }
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String entryKey = entry.getKey();
            Element e = XMLBuilder.addElement(parent, path);
            Element keyElement = XMLBuilder.getOrCreateElement(e, key);
            XMLBuilder.fillField(keyElement, entryKey, key.attribute);
            if (xao == null) {
                String value = valueFactory.serialize(null, entry.getValue());
                XMLBuilder.fillField(e, value, null);
            } else {
                XMLBuilder.fillField(keyElement, entryKey, key.attribute);
                XMLBuilder.toXML(entry.getValue(), e, xao);
            }
        }
    }

}

class ElementMapVisitor implements DOMHelper.NodeMapVisitor {

    @Override
    public void visitNode(Context ctx, XAnnotatedMember xam, Node node, String key, Map<String, Object> result) {
        result.put(key, xam.xao.newInstance(ctx, (Element) node));
    }
}

class ElementValueMapVisitor implements DOMHelper.NodeMapVisitor {
    @Override
    public void visitNode(Context ctx, XAnnotatedMember xam, Node node, String key, Map<String, Object> result) {
        String val = node.getTextContent();
        if (xam.trim) {
            val = val.trim();
        }
        if (xam.valueFactory != null) {
            result.put(key, xam.valueFactory.deserialize(ctx, val));
        } else {
            // TODO: log warning?
            result.put(key, val);
        }
    }
}

class AttributeValueMapVisitor implements DOMHelper.NodeMapVisitor {
    @Override
    public void visitNode(Context ctx, XAnnotatedMember xam, Node node, String key, Map<String, Object> result) {
        String val = node.getNodeValue();
        if (xam.valueFactory != null) {
            result.put(key, xam.valueFactory.deserialize(ctx, val));
        } else {
            // TODO: log warning?
            result.put(key, val);
        }
    }
}
